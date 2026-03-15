"""Report writer: export fold summaries and statistics."""

from pathlib import Path


class ReportWriter:
    """Writes fold reports in JSON and/or text format."""

    def __init__(self, config: dict | None = None) -> None:
        self.config = config or {}
        self.export_json = self.config.get("reports", {}).get("export_json", True)
        self.export_text = self.config.get("reports", {}).get("export_text", True)

    def write(
        self,
        output_dir: Path | str,
        sheet: object,
        fold_records: list[dict],
        metrics: dict | None = None,
    ) -> list[str]:
        """
        Write reports to output_dir.
        Returns list of written file paths.
        Stub: creates directory and placeholder files.
        """
        output_dir = Path(output_dir)
        output_dir.mkdir(parents=True, exist_ok=True)
        written: list[str] = []

        if self.export_text:
            report_path = output_dir / "report.txt"
            report_path.write_text(
                f"Origami Fold Report\n"
                f"==================\n"
                f"Fold count: {len(fold_records)}\n"
                f"Metrics: {metrics or {}}\n"
            )
            written.append(str(report_path))

        if self.export_json:
            import json
            json_path = output_dir / "report.json"
            json_path.write_text(
                json.dumps(
                    {"fold_count": len(fold_records), "metrics": metrics or {}},
                    indent=2,
                )
            )
            written.append(str(json_path))

        return written
