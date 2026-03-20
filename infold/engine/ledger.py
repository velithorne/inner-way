"""Fold ledger: append-only history of committed folds."""

from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Any

from infold.models.fold_record import FoldRecord


@dataclass
class FoldLedger:
    """
    Ledger: version, project id, fold records, totals.
    Append-only during a run. Self-describing, serializable.
    """

    version: str = "1.0"
    project_id: str | None = None
    engine_version: str = "0.1.0"
    created: str = field(default_factory=lambda: datetime.now(timezone.utc).isoformat())
    source_fingerprint: str = ""
    mode: str = "exact"
    config_snapshot: dict[str, Any] = field(default_factory=dict)
    fold_records: list[FoldRecord] = field(default_factory=list)

    @property
    def total_folds(self) -> int:
        return len(self.fold_records)

    @property
    def total_bytes_saved(self) -> int:
        return sum(r.gain for r in self.fold_records)

    def append(self, record: FoldRecord) -> None:
        """Append a fold record. Append-only."""
        self.fold_records.append(record)

    def to_dict(self) -> dict[str, Any]:
        """Serializable representation for reports."""
        return {
            "version": self.version,
            "project_id": self.project_id,
            "engine_version": self.engine_version,
            "created": self.created,
            "source_fingerprint": self.source_fingerprint,
            "mode": self.mode,
            "total_folds": self.total_folds,
            "total_bytes_saved": self.total_bytes_saved,
            "fold_records": [
                {
                    "operator_id": r.operator_id,
                    "gain": r.gain,
                    "utility": r.utility,
                    "target_count": len(r.targets),
                    "validation_summary": r.validation_summary,
                }
                for r in self.fold_records
            ],
        }
