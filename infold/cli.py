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
        print()
        from infold.operators import NoOpOperator

        op = NoOpOperator()
        candidates = op.detect_candidates(sheet, config)
        assert op.operator_id() == "noop"
        assert op.scope() == "none"
        assert len(candidates) == 0
        print("✓ Base operator interface OK")
        print(f"  - NoOpOperator: {op.operator_name()}, scope={op.scope()}")
        print(f"  - detect_candidates returned {len(candidates)} (expected 0)")
        print()
        from infold.validation import validate_candidate
        from infold.models import CandidateCrease, GainEstimate, StressEstimate

        synthetic = CandidateCrease(
            operator_id="noop",
            targets=["test"],
            gain=GainEstimate(0, 0, 0, 0.0, 0.0),
            stress=StressEstimate(0.0, 0.0, 0.0, 0.0, 0.0),
            invariants=[],
            metadata={},
        )
        vr = validate_candidate(op, synthetic, sheet, config)
        assert vr.accepted
        print("✓ Validation pipeline OK")
        print(f"  - validate_candidate(NoOp, synthetic) -> accepted={vr.accepted}")
        print()
        from infold.operators import ExactRepetitionOperator
        from infold.models import FileNode, ProjectSheet
        import hashlib

        # Exact Repetition: create sheet with 2 identical files (min 64 bytes)
        dup_content = ("x = 1\ny = 2\nz = x + y\n" * 5)  # ~90 bytes
        dup_hash = hashlib.sha256(dup_content.encode("utf-8")).hexdigest()
        base = Path(__file__).parent.parent
        dup_sheet = ProjectSheet(source_path=base)
        for name in ["dup_a.py", "dup_b.py"]:
            dup_sheet.file_nodes[Path(name)] = FileNode(
                path=Path(name),
                language="python",
                raw_text=dup_content,
                tokens=[],
                ast_data=None,
                symbols=[],
                imports=[],
                raw_hash=dup_hash,
                token_hash=None,
                parser_confidence=1.0,
                diagnostics=[],
            )
        ex_op = ExactRepetitionOperator()
        ex_candidates = ex_op.detect_candidates(dup_sheet, config)
        assert len(ex_candidates) == 1
        c = ex_candidates[0]
        vr2 = validate_candidate(ex_op, c, dup_sheet, config)
        assert vr2.accepted
        record = ex_op.apply(c, dup_sheet, config)
        unfolded = ex_op.unfold(record, None, config)
        assert len(unfolded) == 2
        assert unfolded[Path("dup_a.py")] == dup_content
        assert unfolded[Path("dup_b.py")] == dup_content
        print("✓ Exact Repetition Fold OK")
        print(f"  - Found {len(ex_candidates)} candidate(s), gain={record.gain} bytes")
        print(f"  - Unfold restored {len(unfolded)} files correctly")
        print()
        from infold.engine import run_fold

        source = Path(__file__).parent.parent
        result = run_fold(source, config)
        print("✓ Fold engine orchestration OK")
        print(f"  - Ledger: {result.ledger.total_folds} folds, {result.ledger.total_bytes_saved} bytes saved")
        print(f"  - Errors: {len(result.errors)}")
        print()
        from infold.reporting import export_report, run_benchmark, benchmark_to_text

        json_path, text_path = export_report(result, config)
        print("✓ Reporting OK")
        print(f"  - Exported: {json_path}, {text_path}")
        bench = run_benchmark(result, config)
        print()
        print("✓ Benchmark OK")
        print(benchmark_to_text(bench))
        from infold.engine import export_package

        pkg_path = export_package(result, config, "infold_package")
        print()
        print("✓ Package export OK")
        print(f"  - {pkg_path}/")
        print(f"  - manifest.json, ledger.json, shared/, maps/, reports/, snapshots/")
        return 0
    except Exception as e:
        print(f"✗ Error: {e}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
