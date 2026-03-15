"""Origami Information Folding Engine — main entry point."""

import json
import sys
from pathlib import Path

from core.intake.project_loader import load_project
from core.folding.fold_engine import FoldEngine
from core.reporting.report_writer import ReportWriter
from operators import get_default_operators


def load_config() -> dict:
    """Load default config from config/default_config.json."""
    config_path = Path(__file__).parent.parent / "config" / "default_config.json"
    if config_path.exists():
        return json.loads(config_path.read_text())
    return {}


def main() -> None:
    if len(sys.argv) < 2:
        print("Usage: python -m app.main /path/to/project")
        sys.exit(1)

    project_path = Path(sys.argv[1])
    if not project_path.exists():
        print(f"Error: path does not exist: {project_path}")
        sys.exit(1)

    config = load_config()
    project_config = config.get("project", {})

    # Load project
    sheet = load_project(
        project_path,
        include_hidden=project_config.get("include_hidden", False),
        include_vendor=project_config.get("include_vendor", False),
    )

    # Run fold engine
    engine = FoldEngine(operators=get_default_operators())
    folded_sheet, fold_records = engine.run(sheet, config)

    # Report
    report_writer = ReportWriter(config)
    output_dir = project_path / "folded_output"
    written = report_writer.write(
        output_dir,
        folded_sheet,
        fold_records,
        metrics={
            "file_count": len(folded_sheet.file_nodes),
            "fold_count": len(fold_records),
        },
    )

    print(f"Loaded {len(sheet.file_nodes)} files from {project_path}")
    print(f"Applied {len(fold_records)} folds")
    print(f"Reports written to: {written}")


if __name__ == "__main__":
    main()
