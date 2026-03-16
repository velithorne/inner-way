"""Decision object for fold validation."""

from dataclasses import dataclass


@dataclass
class ValidationResult:
    """Decision object: hard failures, warnings, fidelity, stress, utility."""

    accepted: bool
    hard_failures: list[str]  # reasons to reject
    warnings: list[str]
    fidelity_ok: bool  # exact bytes/tokens/syntax preserved
    stress_ok: bool  # below threshold
    utility_ok: bool  # above minimum
