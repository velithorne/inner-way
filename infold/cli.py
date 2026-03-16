"""
Infold CLI — entry point for fold operations.
"""

import json
import sys
from pathlib import Path

from infold import __version__


def load_config(config_path: Path | None = None) -> dict:
    """Load configuration from JSON file."""
    if config_path is None:
        config_path = Path(__file__).parent / "config.json"
    with open(config_path, encoding="utf-8") as f:
        return json.load(f)


def verify_models() -> bool:
    """Verify core data classes can be imported and instantiated."""
    from infold.models import (
        ProjectSheet,
        FileNode,
        CandidateCrease,
        GainEstimate,
        StressEstimate,
        FoldRecord,
        FoldSimulationResult,
        ValidationResult,
        UnfoldResult,
    )

    # Smoke test: create minimal instances
    gain = GainEstimate(100, 10, 90, 0.8, 0.95)
    stress = StressEstimate(0.1, 0.2, 0.1, 0.0, 0.15)
    sheet = ProjectSheet(source_path=Path("."))
    return True


def verify_intake(config: dict) -> bool:
    """Verify intake layer: scan project and build inventory."""
    from infold.intake import scan_project

    source = Path(config["project"]["source_path"])
    if source == Path("."):
        source = Path(__file__).parent.parent  # workspace root
    excludes = config["project"].get("exclude_patterns", [])
    includes = config["project"].get("include_extensions", [])

    sheet = scan_project(source, exclude_patterns=excludes, include_extensions=includes)
    return sheet


def main() -> int:
    """Main CLI entry point."""
    print(f"Infold Core v{__version__}")
    print("Structure-aware folding engine for code projects and structured text")
    print()
    try:
        config = load_config()
        print("✓ Config loaded successfully")
        print(f"  - Project include extensions: {config['project']['include_extensions']}")
        print(f"  - Operators enabled: {[k for k, v in config['operators'].items() if v.get('enabled')]}")
        print()
        verify_models()
        print("✓ Core data models OK (ProjectSheet, FileNode, CandidateCrease, etc.)")
        print()
        sheet = verify_intake(config)
        print("✓ Intake layer OK — project scanned")
        print(f"  - Files: {sheet.metrics.get('file_count', 0)}")
        print(f"  - Folders: {sheet.metrics.get('folder_count', 0)}")
        print(f"  - Original size: {sheet.metrics.get('original_size_bytes', 0):,} bytes")
        print()
        from infold.parsers import parse_project

        parse_project(sheet)
        py_files = [n for n in sheet.file_nodes.values() if n.language == "python"]
        total_tokens = sum(len(n.tokens) for n in sheet.file_nodes.values())
        total_symbols = sum(len(n.symbols) for n in sheet.file_nodes.values())
        total_imports = sum(len(n.imports) for n in sheet.file_nodes.values())
        print("✓ Parsers OK — Python + text fallback")
        print(f"  - Python files: {len(py_files)}")
        print(f"  - Total tokens: {total_tokens}")
        print(f"  - Total symbols: {total_symbols}")
        print(f"  - Total imports: {total_imports}")
        return 0
    except Exception as e:
        print(f"✗ Error: {e}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
