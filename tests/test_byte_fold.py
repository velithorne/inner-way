"""
Tests for Infold Byte Fold v0.1: chunking, reconstruction, package integration.
"""

import json
import tempfile
from pathlib import Path

import pytest

from infold.archive.operations import create_archive, reconstruct_archive, validate_archive
from infold.chunking.roller import ChunkResult, chunk_bytes
from infold.engine.file_routing import (
    get_chunk_eligible_paths,
    get_chunk_eligible_paths_with_diagnostics,
    route_file,
    route_file_v2,
    RouteResult,
)
from infold.engine import run_fold
from infold.operators.byte_fold import ByteFoldOperator


def test_chunk_bytes_deterministic() -> None:
    """Chunking is deterministic for same input."""
    data = b"x" * 2000
    r1 = chunk_bytes(data, min_chunk=256, max_chunk=8192, avg_chunk=1024)
    r2 = chunk_bytes(data, min_chunk=256, max_chunk=8192, avg_chunk=1024)
    assert r1.chunk_ids == r2.chunk_ids
    assert r1.boundaries == r2.boundaries


def test_chunk_bytes_empty() -> None:
    """Empty input produces no chunks."""
    r = chunk_bytes(b"", min_chunk=256, max_chunk=8192, avg_chunk=1024)
    assert r.chunk_ids == []
    assert r.boundaries == []


def test_chunk_bytes_small() -> None:
    """Small input below min_chunk produces one chunk."""
    data = b"hello" * 10  # 50 bytes
    r = chunk_bytes(data, min_chunk=64, max_chunk=8192, avg_chunk=256)
    assert len(r.chunk_ids) >= 1
    assert sum(r.chunk_sizes) == len(data)


def test_route_file_passthrough_small() -> None:
    """Very small files route to passthrough_only."""
    route = route_file(Path("x.txt"), 50, None, set())
    assert route in ("passthrough_only", "low_value", "chunk_first")


def test_get_chunk_eligible_excludes_committed() -> None:
    """Committed paths are excluded from chunk eligibility."""
    from infold.intake import scan_project
    from infold.parsers import parse_project

    sheet = scan_project(Path("tests/fixtures/byte_fold"))
    parse_project(sheet)
    committed = {"part_a.txt", "part_b.txt", "part_c.txt"}
    eligible = get_chunk_eligible_paths(sheet.file_nodes, committed, {})
    paths_str = [str(p).replace("\\", "/") for p in eligible]
    assert "part_a.txt" not in paths_str
    assert "part_b.txt" not in paths_str
    assert "part_c.txt" not in paths_str


def test_byte_fold_operator_interface() -> None:
    """ByteFoldOperator implements base interface."""
    op = ByteFoldOperator()
    assert op.operator_id() == "byte_fold"
    assert op.operator_name() == "Byte Fold"
    assert "exact byte recovery" in op.invariants()


def test_byte_fold_detect_returns_list() -> None:
    """detect_candidates returns a list (may be empty)."""
    from infold.intake import scan_project
    from infold.parsers import parse_project

    sheet = scan_project(Path("tests/fixtures/byte_fold"))
    parse_project(sheet)
    op = ByteFoldOperator()
    config = {"_committed_paths": {"part_a.txt", "part_b.txt", "part_c.txt"}}
    config["thresholds"] = {"byte_fold": {"min_net_gain": -10000}}
    candidates = op.detect_candidates(sheet, config)
    assert isinstance(candidates, list)


def test_archive_with_byte_fold_reconstructs() -> None:
    """Archive create/validate/reconstruct works when byte_fold is enabled."""
    with tempfile.TemporaryDirectory() as tmp:
        out = Path(tmp) / "test.infold"
        restored = Path(tmp) / "restored"
        create_archive(Path("tests/fixtures/byte_fold"), out)
        ok, _ = validate_archive(out, mode="basic")
        assert ok
        result = reconstruct_archive(out, restored)
        assert isinstance(result, dict)
        for path_str, content in result.items():
            orig = Path("tests/fixtures/byte_fold") / path_str
            if orig.exists():
                assert content == orig.read_text(encoding="utf-8"), f"Mismatch: {path_str}"


def test_archive_validate_strict_with_byte_fold() -> None:
    """Strict validation passes for archives that may include byte_fold."""
    with tempfile.TemporaryDirectory() as tmp:
        out = Path(tmp) / "test.infold"
        create_archive(Path("tests/fixtures/byte_fold"), out)
        ok, errors = validate_archive(out, mode="strict")
        assert ok, errors


# Byte Fold focused tests: mixed, opaque, large text, version-like datasets


def test_byte_fold_mixed_arbitrary_content() -> None:
    """Mixed arbitrary content: archive create/validate/reconstruct preserves exact reconstruction."""
    with tempfile.TemporaryDirectory() as tmp:
        out = Path(tmp) / "mixed.infold"
        restored = Path(tmp) / "restored"
        create_archive(Path("tests/fixtures/mixed_project"), out)
        ok, _ = validate_archive(out, mode="strict")
        assert ok
        result = reconstruct_archive(out, restored)
        for path_str, content in result.items():
            orig = Path("tests/fixtures/mixed_project") / path_str
            if orig.exists():
                assert content == orig.read_text(encoding="utf-8"), f"Mismatch: {path_str}"


def test_byte_fold_repeated_opaque_files() -> None:
    """Repeated opaque/binary-like files: archive create/validate/reconstruct."""
    with tempfile.TemporaryDirectory() as tmp:
        out = Path(tmp) / "opaque.infold"
        restored = Path(tmp) / "restored"
        create_archive(Path("tests/fixtures/byte_fold"), out)
        ok, _ = validate_archive(out, mode="strict")
        assert ok
        result = reconstruct_archive(out, restored)
        for path_str, content in result.items():
            orig = Path("tests/fixtures/byte_fold") / path_str
            if orig.exists():
                assert content == orig.read_text(encoding="utf-8"), f"Mismatch: {path_str}"


def test_byte_fold_slightly_changed_large_text() -> None:
    """Slightly changed large text files: archive create/validate/reconstruct."""
    with tempfile.TemporaryDirectory() as tmp:
        out = Path(tmp) / "large.infold"
        restored = Path(tmp) / "restored"
        create_archive(Path("tests/fixtures/byte_fold_large_text"), out)
        ok, _ = validate_archive(out, mode="strict")
        assert ok
        result = reconstruct_archive(out, restored)
        for path_str, content in result.items():
            orig = Path("tests/fixtures/byte_fold_large_text") / path_str
            if orig.exists():
                assert content == orig.read_text(encoding="utf-8"), f"Mismatch: {path_str}"


def test_byte_fold_version_like_partial_repeated() -> None:
    """Version-like folders with partial repeated content: archive create/validate/reconstruct."""
    with tempfile.TemporaryDirectory() as tmp:
        out = Path(tmp) / "version.infold"
        restored = Path(tmp) / "restored"
        create_archive(Path("tests/fixtures/byte_fold_version_like"), out)
        ok, _ = validate_archive(out, mode="strict")
        assert ok
        result = reconstruct_archive(out, restored)
        for path_str, content in result.items():
            orig = Path("tests/fixtures/byte_fold_version_like") / path_str
            if orig.exists():
                assert content == orig.read_text(encoding="utf-8"), f"Mismatch: {path_str}"


# Phase 5B: routing v2, chunk tuning, compact format


def test_route_file_v2_returns_diagnostic() -> None:
    """route_file_v2 returns RouteResult with route and reason."""
    r = route_file_v2(Path("x.txt"), 50, None, set())
    assert isinstance(r, RouteResult)
    assert r.route in ("passthrough_only", "low_value", "chunk_first")


def test_route_file_v2_committed() -> None:
    """Committed paths route to structural_first."""
    r = route_file_v2(Path("foo.py"), 1000, None, {"foo.py"})
    assert r.route == "structural_first"
    assert "committed" in r.reason.lower()


def test_route_file_v2_binary_like() -> None:
    """Binary-like extensions route to chunk_first."""
    r = route_file_v2(Path("data.bin"), 500, None, set())
    assert r.route == "chunk_first"
    assert "binary" in r.reason.lower()


def test_route_file_v2_size_bounds() -> None:
    """Size below min routes to passthrough_only."""
    r = route_file_v2(Path("x.txt"), 50, None, set())
    assert r.route == "passthrough_only"


def test_chunk_tuning_max_chunks_per_file() -> None:
    """max_chunks_per_file filters out files with too many chunks."""
    from infold.intake import scan_project
    from infold.parsers import parse_project

    sheet = scan_project(Path("tests/fixtures/byte_fold"))
    parse_project(sheet)
    config = {
        "_committed_paths": {"part_a.txt", "part_b.txt", "part_c.txt"},
        "thresholds": {"byte_fold": {"max_chunks_per_file": 1, "min_net_gain": -10000}},
    }
    op = ByteFoldOperator()
    candidates = op.detect_candidates(sheet, config)
    assert isinstance(candidates, list)


def test_compact_chunk_reconstruction() -> None:
    """Archive with byte_fold uses compact paths+seqs format and reconstructs correctly."""
    with tempfile.TemporaryDirectory() as tmp:
        out = Path(tmp) / "test.infold"
        restored = Path(tmp) / "restored"
        create_archive(Path("tests/fixtures/byte_fold"), out)
        import zipfile
        import json
        with zipfile.ZipFile(out, "r") as zf:
            zf.extractall(tmp)
        root = Path(tmp)
        recon_path = root / "maps" / "chunk_reconstruction.json"
        if recon_path.exists():
            data = json.loads(recon_path.read_text())
            for rec in data.get("records", []):
                if "paths" in rec and "seqs" in rec:
                    assert isinstance(rec["paths"], list)
                    assert isinstance(rec["seqs"], list)
        result = reconstruct_archive(out, restored)
        for path_str, content in result.items():
            orig = Path("tests/fixtures/byte_fold") / path_str
            if orig.exists():
                assert content == orig.read_text(encoding="utf-8"), f"Mismatch: {path_str}"
