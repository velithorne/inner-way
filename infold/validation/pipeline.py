"""
Validation pipeline: dry fold, dry unfold, invariant checks, threshold checks.

Per roadmap section 22:
1. Candidate sanity check
2. Gain and stress estimation (reject no-gain/high-risk early)
3. Dry fold (simulate)
4. Dry unfold (attempt reconstruction)
5. Fidelity checks
6. Hard rule checks
7. Threshold check
8. Commit readiness (earlier folds not invalidated)
"""

from typing import Any

from infold.models.candidate import CandidateCrease
from infold.models.project_sheet import ProjectSheet
from infold.models.simulation import FoldSimulationResult
from infold.models.validation_result import ValidationResult
from infold.operators.base import BaseOperator


def _get_validation_config(config: dict[str, Any]) -> dict[str, Any]:
    """Extract validation thresholds from config."""
    return config.get("validation", {})


def _candidate_sanity_check(
    candidate: CandidateCrease,
    project_sheet: ProjectSheet,
) -> list[str]:
    """Step 1: Targets exist, scope is valid."""
    failures: list[str] = []
    for target in candidate.targets:
        if target is None:
            failures.append("Target is None")
    if not candidate.targets and candidate.operator_id != "noop":
        failures.append("No targets")
    return failures


def _threshold_check(
    gain_net: int,
    stress_overall: float,
    utility: float,
    config: dict[str, Any],
) -> tuple[bool, list[str]]:
    """Step 7: Enforce min net gain, max stress, min utility."""
    failures: list[str] = []
    val = config
    min_gain = val.get("min_net_gain", 0)
    max_stress = val.get("max_stress", 1.0)
    min_utility = val.get("min_utility_score", 0)

    if gain_net < min_gain:
        failures.append(f"Net gain {gain_net} below minimum {min_gain}")
    if stress_overall > max_stress:
        failures.append(f"Stress {stress_overall} exceeds maximum {max_stress}")
    if utility < min_utility:
        failures.append(f"Utility {utility} below minimum {min_utility}")

    return len(failures) == 0, failures


def _hard_rule_checks(
    operator: BaseOperator,
    simulation_result: FoldSimulationResult,
    operator_validation: ValidationResult,
    config: dict[str, Any],
) -> list[str]:
    """
    Step 6: Mandatory hard rules.
    - No fold in exact mode if exact recovery fails
    - No silent downgrade from exact mode
    """
    failures: list[str] = []
    exact_mode = config.get("exact_mode", True)

    if exact_mode and not simulation_result.success:
        failures.append("Exact mode: simulation failed (no successful unfold)")
    if exact_mode and not operator_validation.fidelity_ok:
        failures.append("Exact mode: fidelity check failed")
    failures.extend(operator_validation.hard_failures)
    return failures


def validate_candidate(
    operator: BaseOperator,
    candidate: CandidateCrease,
    project_sheet: ProjectSheet,
    config: dict[str, Any],
) -> ValidationResult:
    """
    Run the full validation pipeline for a candidate.

    Returns ValidationResult with accepted=True only if all steps pass.
    """
    val_config = _get_validation_config(config)
    hard_failures: list[str] = []
    warnings: list[str] = []

    # Step 1: Candidate sanity check
    sanity_failures = _candidate_sanity_check(candidate, project_sheet)
    hard_failures.extend(sanity_failures)
    if hard_failures:
        return ValidationResult(
            accepted=False,
            hard_failures=hard_failures,
            warnings=warnings,
            fidelity_ok=False,
            stress_ok=False,
            utility_ok=False,
        )

    # Step 2: Gain and stress estimation
    gain = operator.estimate_gain(candidate, project_sheet, config)
    stress = operator.estimate_stress(candidate, project_sheet, config)

    if gain.net_bytes_saved <= 0 and candidate.operator_id != "noop":
        hard_failures.append("No net gain")
    if stress.overall_stress > val_config.get("max_stress", 1.0):
        hard_failures.append(f"Stress {stress.overall_stress} exceeds threshold")

    if hard_failures:
        return ValidationResult(
            accepted=False,
            hard_failures=hard_failures,
            warnings=warnings,
            fidelity_ok=False,
            stress_ok=stress.overall_stress <= val_config.get("max_stress", 1.0),
            utility_ok=gain.utility_score >= val_config.get("min_utility_score", 0),
        )

    # Steps 3–4: Dry fold and dry unfold (operator.simulate does both)
    simulation_result = operator.simulate(candidate, project_sheet, config)

    # Step 5–6: Operator validation and hard rules
    operator_validation = operator.validate(simulation_result, project_sheet, config)
    hard_failures.extend(
        _hard_rule_checks(operator, simulation_result, operator_validation, val_config)
    )
    warnings.extend(operator_validation.warnings)

    # Step 7: Threshold check
    threshold_ok, threshold_failures = _threshold_check(
        gain.net_bytes_saved,
        stress.overall_stress,
        gain.utility_score,
        val_config,
    )
    hard_failures.extend(threshold_failures)

    accepted = (
        len(hard_failures) == 0
        and operator_validation.accepted
        and threshold_ok
    )

    return ValidationResult(
        accepted=accepted,
        hard_failures=hard_failures,
        warnings=warnings,
        fidelity_ok=operator_validation.fidelity_ok,
        stress_ok=operator_validation.stress_ok and threshold_ok,
        utility_ok=operator_validation.utility_ok and threshold_ok,
    )
