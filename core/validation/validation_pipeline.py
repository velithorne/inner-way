"""Validation pipeline: sanity checks, gain/stress, dry fold/unfold, fidelity."""

from core.types import ProjectSheet


class ValidationPipeline:
    """
    Validates fold candidates through:
    1. Candidate sanity check
    2. Gain/stress estimation
    3. Dry fold
    4. Dry unfold
    5. Fidelity checks
    6. Hard rule check
    7. Threshold check
    """

    def __init__(self, config: dict | None = None) -> None:
        self.config = config or {}
        self.max_stress = self.config.get("validation", {}).get("max_stress", 0.35)
        self.min_net_bytes_saved = self.config.get("validation", {}).get("min_net_bytes_saved", 32)

    def validate_candidate(
        self,
        sheet: ProjectSheet,
        candidate: dict,
        operator_id: str,
    ) -> tuple[bool, list[str]]:
        """
        Validate a fold candidate.
        Returns (is_valid, list of failure reasons).
        Stub: returns True with empty reasons.
        """
        reasons: list[str] = []
        # TODO: implement full pipeline
        return (len(reasons) == 0, reasons)
