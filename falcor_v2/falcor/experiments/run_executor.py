"""Run executor: execute experiment plan, collect data, produce artifacts."""

from __future__ import annotations

import json
import uuid
from pathlib import Path
from typing import Any

import numpy as np
import pandas as pd

from falcor.core.config import AppConfig, ExperimentConfig
from falcor.core.manifest import create_manifest
from falcor.core.paths import get_run_path, ensure_run_structure
from falcor.core.rng import create_run_rng, set_default_seed
from falcor.devices.virtual_assembly import load_assembly, VirtualAssembly
from falcor.experiments.experiment_plan import ExperimentPlan
from falcor.experiments.energy_audit import EnergyAudit, EnergyAuditResult
from falcor.experiments.reports import render_audit_markdown, render_audit_html
from falcor.physics.thermal.heat_solver import LumpedThermalSolver, ThermalNode
from falcor.physics.mechanics.rigid_body import RigidBodyKinematics
from falcor.physics.em.quasistatic_solver import QuasistaticSolver, CoilSpec
from falcor.physics.em.energy_density import magnetic_energy_density
from falcor.storage.writers import write_parquet, write_json
from falcor.core.logging import get_logger

logger = get_logger("run_executor")


def _build_thermal_solver(assembly: VirtualAssembly) -> LumpedThermalSolver:
    """Build lumped thermal solver from assembly."""
    nodes: list[ThermalNode] = []
    mats = assembly.get_materials()
    for c in assembly.components:
        mat = mats.get(c.material, {})
        rho = mat.get("density", 1000)
        cp = mat.get("cp", 1000)
        k = mat.get("k", 1)
        params = c.params
        if c.shape == "disk":
            r = params.get("radius_m", 0.1)
            th = params.get("thickness_m", 0.01)
            vol = np.pi * r**2 * th
            area = 2 * np.pi * r**2 + 2 * np.pi * r * th
        elif c.shape == "cylinder":
            r = params.get("radius_m", 0.02)
            h = params.get("height_m", 0.05)
            vol = np.pi * r**2 * h
            area = 2 * np.pi * r * (r + h)
        else:
            vol = 0.001
            area = 0.01
        mass = rho * vol
        nodes.append(ThermalNode(id=c.id, mass=mass, cp=cp, area=area, k=k))
    return LumpedThermalSolver(nodes)


def _build_em_solver(assembly: VirtualAssembly) -> QuasistaticSolver:
    """Build EM solver from assembly coils."""
    coils: list[CoilSpec] = []
    for c in assembly.components:
        if c.shape == "coil":
            t = c.transform
            coils.append(
                CoilSpec(
                    radius_m=c.params.get("radius_m", 0.08),
                    turns=int(c.params.get("turns", 100)),
                    height_m=c.params.get("height_m", 0.02),
                    x=t.x, y=t.y, z=t.z,
                )
            )
    return QuasistaticSolver(coils) if coils else QuasistaticSolver([CoilSpec(0.08, 100, 0.02)])


class RunExecutor:
    """Execute experiment runs."""

    def __init__(self, config: AppConfig):
        self.config = config
        self.assembly: VirtualAssembly | None = None

    def load_device(self, path: Path | str) -> VirtualAssembly:
        self.assembly = load_assembly(path)
        return self.assembly

    def run(
        self,
        plan: ExperimentPlan | None = None,
        device_path: Path | str | None = None,
    ) -> str:
        """Execute run. Returns run_id."""
        plan = plan or ExperimentPlan(
            mode=self.config.experiment.mode,
            frequency_start_hz=self.config.experiment.frequency_sweep_start_hz,
            frequency_stop_hz=self.config.experiment.frequency_sweep_stop_hz,
            frequency_steps=self.config.experiment.frequency_sweep_steps,
            magnet_state_schedule=list(self.config.experiment.magnet_state_schedule),
            dwell_time_s=self.config.experiment.dwell_time_s,
            settle_time_s=self.config.experiment.settle_time_s,
            sample_rate_hz=self.config.experiment.sample_rate_hz,
            seed=self.config.experiment.seed,
        )

        device_path = device_path or (self.config.device.path if self.config.device else None)
        if not device_path:
            raise ValueError("No device path provided")
        assembly = self.load_device(device_path)

        run_id = str(uuid.uuid4())[:8]
        seed = plan.seed or 42
        set_default_seed(seed)
        rng = create_run_rng(run_id, seed)

        thermal = _build_thermal_solver(assembly)
        em_solver = _build_em_solver(assembly)
        kinematics = RigidBodyKinematics()

        config_snapshot = self.config.model_dump(mode="json")
        device_dict = assembly.model_dump(mode="json")
        manifest = create_manifest(run_id, config_snapshot, device_dict)

        run_path = get_run_path(run_id, str(self.config.lab_results_dir))
        ensure_run_structure(run_path)

        write_json(run_path / "manifest.json", manifest.to_dict())
        write_json(run_path / "config_snapshot.json", config_snapshot)

        # Append to lab_results index
        lab_dir = Path(self.config.lab_results_dir)
        if not lab_dir.is_absolute():
            lab_dir = Path.cwd() / lab_dir
        lab_dir.mkdir(parents=True, exist_ok=True)
        index_path = lab_dir / "index.parquet"
        index_row = pd.DataFrame([{
            "run_id": run_id,
            "manifest_hash": manifest.manifest_hash,
            "device": str(device_path),
        }])
        if index_path.exists():
            existing = pd.read_parquet(index_path)
            index_row = pd.concat([existing, index_row], ignore_index=True)
        write_parquet(index_path, index_row)

        steps = plan.get_sweep_steps()
        raw_rows: list[dict] = []
        derived_rows: list[dict] = []

        for i, step in enumerate(steps):
            freq = step["frequency_hz"]
            rpm = step["rpm"]
            magnet = step["magnet_state"]
            kinematics.set_rpm(rpm)
            I_coil = 1.0 if magnet == "ON" else 0.0
            B = em_solver.B_axial(0.1, I_coil)
            thermal.set_eddy_heating("disk", 0.1 * I_coil)  # Simplified

            n_samples = int((plan.dwell_time_s + plan.settle_time_s) * plan.sample_rate_hz)
            t = np.linspace(0, plan.dwell_time_s + plan.settle_time_s, n_samples)
            dt = t[1] - t[0] if len(t) > 1 else 0.1

            for j, ti in enumerate(t):
                thermal.step_temperature(dt)
                temps = thermal.get_temperatures()
                for comp_id, Tk in temps.items():
                    raw_rows.append({
                        "step": i,
                        "t": ti,
                        "component": comp_id,
                        "T_K": Tk + rng.normal(0, 0.1),
                        "rpm": rpm + rng.normal(0, 0.5),
                        "B_T": B + rng.normal(0, 0.01),
                        "magnet_state": magnet,
                        "frequency_hz": freq,
                    })

            T_agg = np.mean(list(temps.values()))
            dT_dt = 0.01  # Placeholder
            C_agg = sum(n.mass * n.cp for n in thermal.nodes.values())
            audit = EnergyAudit(config=self.config.audit)
            audit_result = audit.compute(
                motor_power=10.0,
                motor_power_sigma=0.5,
                coil_power=I_coil * 5.0,
                coil_power_sigma=0.2,
                dT_dt_aggregate=dT_dt,
                heat_capacity_aggregate=C_agg,
                P_thermal_sigma=0.1,
            )
            derived_rows.append({
                "step": i,
                "frequency_hz": freq,
                "magnet_state": magnet,
                "T_mean_K": T_agg,
                "T_sigma": 0.1,
                "slope": dT_dt,
                "B_T": B,
                "vib_rms": 0.01,
                "power_W": 10.0 + I_coil * 5.0,
                "pass": True,
                "audit_stable": audit_result.is_stable,
            })

        raw_df = pd.DataFrame(raw_rows)
        derived_df = pd.DataFrame(derived_rows)
        write_parquet(run_path / "raw_timeseries.parquet", raw_df)
        write_parquet(run_path / "derived_metrics.parquet", derived_df)

        audit_result = audit.compute(
            motor_power=10.0,
            motor_power_sigma=0.5,
            coil_power=2.5,
            coil_power_sigma=0.2,
            dT_dt_aggregate=0.01,
            heat_capacity_aggregate=C_agg,
            P_thermal_sigma=0.1,
        )
        (run_path / "audit_report.md").write_text(render_audit_markdown(audit_result, run_id), encoding="utf-8")
        (run_path / "audit_report.html").write_text(render_audit_html(audit_result, run_id), encoding="utf-8")

        logger.info("Run %s complete: %d steps", run_id, len(steps))
        return run_id
