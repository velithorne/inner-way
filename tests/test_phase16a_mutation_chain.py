"""
Phase 16A: Mutation Chain v0.1 tests.
"""
import json
from pathlib import Path

import pytest

from infold.engine.mutation_chain import (
    build_mutation_chain,
    reconstruct_from_chain,
    find_mutation_chain_candidates,
    _pick_base,
    _compute_mutation,
    _apply_mutation,
)
from infold.operators.mutation_chain import MutationChainOperator
from infold.models.project_sheet import ProjectSheet
from infold.archive import create_archive, validate_archive, reconstruct_archive, explain_archive


def test_pick_base_deterministic():
    """Base selection: smallest by bytes, then first by path."""
    paths = [Path("c"), Path("a"), Path("b")]
    contents = {Path("a"): "x" * 10, Path("b"): "x" * 5, Path("c"): "x" * 10}
    base = _pick_base(paths, contents)
    assert base == Path("b")  # smallest


def test_compute_mutation():
    """Mutation records only differing lines."""
    base = ["a\n", "b\n", "c\n"]
    member = ["a\n", "X\n", "c\n"]
    mut = _compute_mutation(base, member)
    assert mut == [(1, "X\n")]


def test_apply_mutation():
    """Reconstruction from base + mutation."""
    base = ["a\n", "b\n", "c\n"]
    mut = [(1, "X\n")]
    result = _apply_mutation(base, mut)
    assert result == ["a\n", "X\n", "c\n"]


def test_build_mutation_chain_net_positive():
    """Chain built only when net-positive."""
    base = "header\n" * 15 + "slot\n" + "footer\n" * 4  # 20 lines, 1 slot
    paths = [Path("a"), Path("b"), Path("c")]
    contents = {
        Path("a"): base.replace("slot\n", "v1\n"),
        Path("b"): base.replace("slot\n", "v2\n"),
        Path("c"): base.replace("slot\n", "v3\n"),
    }
    chain = build_mutation_chain(paths, contents, min_family_size=3, min_lines=10, min_line_overlap_ratio=0.9)
    assert chain is not None
    assert chain["gain_bytes"] > 0
    assert chain["base_path"] in ["a", "b", "c"]
    assert len(chain["mutations"]) == 2


def test_reconstruct_from_chain():
    """Exact reconstruction from chain."""
    base = ("h1\nh2\nh3\nh4\nh5\n" * 20)[:-1]  # 100 lines
    paths = [Path("a"), Path("b"), Path("c")]
    contents = {
        Path("a"): base.replace("h5\n", "v1\n", 1),
        Path("b"): base.replace("h5\n", "v2\n", 1),
        Path("c"): base.replace("h5\n", "v3\n", 1),
    }
    chain = build_mutation_chain(paths, contents, min_family_size=3, min_lines=50, min_line_overlap_ratio=0.98)
    assert chain is not None
    recon = reconstruct_from_chain(chain)
    for p, expected in contents.items():
        assert recon[str(p)] == expected


def test_find_candidates():
    """Find mutation chain candidates from path_to_content."""
    base = ("h1\nh2\nh3\nh4\nh5\n" * 20)[:-1]
    path_to_content = {
        Path("a"): base.replace("h5\n", "v1\n", 1),
        Path("b"): base.replace("h5\n", "v2\n", 1),
        Path("c"): base.replace("h5\n", "v3\n", 1),
    }
    chains = find_mutation_chain_candidates(path_to_content, min_family_size=3, min_lines=50, min_line_overlap_ratio=0.98)
    assert len(chains) >= 1


def test_mutation_chain_operator_detect():
    """Operator detects candidates on fixture."""
    fixture = Path(__file__).parent.parent / "tests" / "fixtures" / "mutation_chain"
    if not fixture.exists():
        pytest.skip("mutation_chain fixture not found")
    from infold.intake import scan_project
    from infold.parsers import parse_project
    sheet = scan_project(fixture)
    parse_project(sheet)
    op = MutationChainOperator()
    cfg = {"thresholds": {"mutation_chain": {"min_family_size": 3, "min_lines": 5, "min_line_overlap_ratio": 0.80}}}
    candidates = op.detect_candidates(sheet, cfg)
    assert len(candidates) >= 1


def test_archive_with_mutation_chain_reconstructs(tmp_path):
    """Archive with mutation chain folds reconstructs exactly."""
    fixture = Path(__file__).parent.parent / "tests" / "fixtures" / "mutation_chain"
    if not fixture.exists():
        pytest.skip("mutation_chain fixture not found")
    archive = tmp_path / "out.infold"
    create_archive(fixture, archive, {"operators": {"mutation_chain": {"enabled": True}}})
    ok, _ = validate_archive(archive, mode="strict")
    assert ok
    out = tmp_path / "restored"
    result = reconstruct_archive(archive, out)
    for p in fixture.rglob("*"):
        if p.is_file():
            rel = p.relative_to(fixture)
            restored = out / rel
            assert restored.exists(), f"Missing {rel}"
            assert restored.read_text() == p.read_text(), f"Content mismatch {rel}"


def test_explain_shows_mutation_chain(tmp_path):
    """Explain output includes mutation chain info when used."""
    fixture = Path(__file__).parent.parent / "tests" / "fixtures" / "mutation_chain"
    if not fixture.exists():
        pytest.skip("mutation_chain fixture not found")
    archive = tmp_path / "out.infold"
    create_archive(fixture, archive, {"operators": {"mutation_chain": {"enabled": True}}})
    info = explain_archive(archive)
    if info.get("fold_counts_by_operator", {}).get("mutation_chain", 0) > 0:
        assert "mutation_chain" in info.get("fold_counts_by_operator", {})
        assert info.get("mutation_chain_families")


def test_contiguous_block_encoding():
    """Phase 16B: Contiguous block encoding for 2+ adjacent differing lines."""
    from infold.engine.mutation_chain import _compute_mutation, _apply_mutation
    base = ["a\n", "b\n", "c\n", "d\n", "e\n"]
    member = ["a\n", "X\n", "Y\n", "d\n", "e\n"]
    mut = _compute_mutation(base, member)
    assert len(mut) == 1
    assert mut[0][0] == 1
    assert isinstance(mut[0][1], list)
    assert mut[0][1] == ["X\n", "Y\n"]
    recon = _apply_mutation(base, mut)
    assert recon == member


def test_cost_aware_base_selection():
    """Phase 16B: Base selection is deterministic and chain builds when net-positive."""
    from pathlib import Path
    from infold.engine.mutation_chain import _pick_base_min_mutation_payload, build_mutation_chain
    base_content = "header_line_here\n" * 30 + "slot\n"
    contents = {
        Path("a"): base_content.replace("slot\n", "v1\n"),
        Path("b"): base_content.replace("slot\n", "v2\n"),
        Path("c"): base_content.replace("slot\n", "v3\n"),
    }
    lines_map = {p: contents[p].splitlines(keepends=True) for p in contents}
    base = _pick_base_min_mutation_payload(list(contents.keys()), contents, lines_map)
    assert base in contents
    chain = build_mutation_chain(list(contents.keys()), contents, min_family_size=3, min_lines=30, min_line_overlap_ratio=0.96)
    assert chain is not None


def test_mutation_fixtures_reconstruct(tmp_path):
    """Phase 16B: All mutation-friendly fixtures reconstruct exactly."""
    base = Path(__file__).parent.parent
    for name in ["mutation_chain", "mutation_version_like", "mutation_config_heavy", "mutation_script_like"]:
        fixture = base / "tests" / "fixtures" / name
        if not fixture.exists():
            continue
        archive = tmp_path / f"{name}.infold"
        create_archive(fixture, archive)
        ok, _ = validate_archive(archive, mode="strict")
        assert ok
        out = tmp_path / f"restored_{name}"
        reconstruct_archive(archive, out)
        for p in fixture.rglob("*"):
            if p.is_file():
                rel = p.relative_to(fixture)
                restored = out / rel
                assert restored.exists(), f"Missing {rel} in {name}"
                assert restored.read_text() == p.read_text(), f"Content mismatch {rel} in {name}"
