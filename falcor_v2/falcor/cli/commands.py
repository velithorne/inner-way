"""CLI commands."""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

from falcor.core.config import AppConfig, DeviceConfig, ExperimentConfig
from falcor.core.paths import get_lab_results_path, get_run_path
from falcor.experiments.experiment_plan import ExperimentPlan
from falcor.experiments.run_executor import RunExecutor
from falcor.storage.exports import export_run_csv
from falcor.storage.readers import read_json


def _parse_run_args(args: list[str]) -> argparse.Namespace:
    p = argparse.ArgumentParser(prog="falcor run")
    p.add_argument("--device", "-d", required=True, help="Path to virtual assembly JSON")
    p.add_argument("--mode", "-m", choices=["SIM", "FIELD"], default="SIM")
    p.add_argument("--plan", "-p", help="Path to experiment plan JSON (optional)")
    p.add_argument("--freq-start", type=float, default=1.0)
    p.add_argument("--freq-stop", type=float, default=100.0)
    p.add_argument("--freq-steps", type=int, default=10)
    p.add_argument("--dwell", type=float, default=5.0)
    p.add_argument("--seed", type=int, default=None)
    return p.parse_args(args)


def run_cmd(args: list[str]) -> int:
    """Execute run."""
    ns = _parse_run_args(args)
    device_path = Path(ns.device)
    if not device_path.exists():
        print(f"Device file not found: {device_path}")
        return 1

    config = AppConfig(
        device=DeviceConfig(path=str(device_path)),
        experiment=ExperimentConfig(
            mode=ns.mode,
            frequency_sweep_start_hz=ns.freq_start,
            frequency_sweep_stop_hz=ns.freq_stop,
            frequency_sweep_steps=ns.freq_steps,
            dwell_time_s=ns.dwell,
            seed=ns.seed,
        ),
    )

    plan = None
    if ns.plan:
        plan_path = Path(ns.plan)
        if plan_path.exists():
            data = read_json(plan_path)
            plan = ExperimentPlan(
                mode=data.get("mode", "SIM"),
                frequency_start_hz=data.get("frequency_start_hz", ns.freq_start),
                frequency_stop_hz=data.get("frequency_stop_hz", ns.freq_stop),
                frequency_steps=data.get("frequency_steps", ns.freq_steps),
                dwell_time_s=data.get("dwell_time_s", ns.dwell),
                seed=data.get("seed"),
            )

    executor = RunExecutor(config)
    run_id = executor.run(plan=plan, device_path=device_path)
    print(f"Run complete: {run_id}")
    run_path = get_run_path(run_id, str(config.lab_results_dir))
    print(f"Results: {run_path}")
    return 0


def _parse_export_args(args: list[str]) -> argparse.Namespace:
    p = argparse.ArgumentParser(prog="falcor export")
    p.add_argument("--run", "-r", required=True, help="Run ID")
    p.add_argument("--format", "-f", choices=["csv"], default="csv")
    p.add_argument("--output", "-o", help="Output path")
    p.add_argument("--lab-dir", default="lab_results")
    return p.parse_args(args)


def export_cmd(args: list[str]) -> int:
    """Export run to CSV."""
    ns = _parse_export_args(args)
    run_path = get_run_path(ns.run, ns.lab_dir)
    if not run_path.exists():
        print(f"Run not found: {run_path}")
        return 1
    out = export_run_csv(run_path, ns.output)
    print(f"Exported to {out}")
    return 0


def _parse_report_args(args: list[str]) -> argparse.Namespace:
    p = argparse.ArgumentParser(prog="falcor report")
    p.add_argument("--run", "-r", required=True, help="Run ID")
    p.add_argument("--lab-dir", default="lab_results")
    return p.parse_args(args)


def report_cmd(args: list[str]) -> int:
    """Print audit report."""
    ns = _parse_report_args(args)
    run_path = get_run_path(ns.run, ns.lab_dir)
    if not run_path.exists():
        print(f"Run not found: {run_path}")
        return 1
    audit_path = run_path / "audit_report.md"
    if audit_path.exists():
        print(audit_path.read_text(encoding="utf-8"))
    else:
        print("No audit report found.")
    return 0


def _parse_calibrate_args(args: list[str]) -> argparse.Namespace:
    p = argparse.ArgumentParser(prog="falcor calibrate")
    p.add_argument("--sensor", "-s", required=True, help="Sensor ID (e.g. temp_ir)")
    return p.parse_args(args)


def calibrate_cmd(args: list[str]) -> int:
    """Calibrate sensor (stub: prints workflow)."""
    ns = _parse_calibrate_args(args)
    print(f"Calibration workflow for sensor: {ns.sensor}")
    print("1. Capture baseline window (stable conditions)")
    print("2. Estimate offset, scale, noise_sigma, lag from samples")
    print("3. Store calibration profile")
    print("(Hardware calibration not implemented in CLI; use UI)")
    return 0


def gui_cmd(args: list[str]) -> int:
    """Launch GUI."""
    from falcor.app import run_app
    return run_app()
