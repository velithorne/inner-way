"""Phase 14A: Path DNA Folding and Family Membranes tests."""

import json
import tempfile
from pathlib import Path

import pytest

from infold.engine.path_dna import build_path_dna, expand_path_dna, path_dna_net_saved


def test_path_dna_encode_decode():
    """Path DNA encoding and decoding is deterministic and reversible."""
    paths = [
        "tests/fixtures/template_heavy/a.py",
        "tests/fixtures/template_heavy/b.py",
        "tests/fixtures/template_heavy/c.py",
    ]
    dna = build_path_dna(paths)
    assert dna is not None
    expanded = expand_path_dna(dna)
    assert expanded == paths


def test_path_dna_returns_none_when_no_savings():
    """Path DNA returns None when encoding would not save bytes."""
    paths = ["a", "b", "c"]  # No shared prefixes
    dna = build_path_dna(paths)
    assert dna is None


def test_path_dna_deterministic():
    """Path DNA produces same output for same input."""
    paths = ["src/a.py", "src/b.py", "src/c.py"]
    dna1 = build_path_dna(paths)
    dna2 = build_path_dna(paths)
    assert dna1 == dna2


def test_path_dna_net_saved():
    """path_dna_net_saved returns positive when dna saves bytes."""
    paths = ["tests/fixtures/template_heavy/a.py", "tests/fixtures/template_heavy/b.py"]
    dna = build_path_dna(paths)
    if dna:
        saved = path_dna_net_saved(paths, dna)
        assert saved > 0


def test_micro_archive_has_family_membranes_or_path_dna(tmp_path):
    """Micro archive uses family membranes or path DNA when beneficial."""
    from infold.archive import create_archive

    fixture = Path(__file__).parent / "fixtures" / "template_heavy"
    if not fixture.exists():
        pytest.skip("template_heavy fixture not found")
    out = tmp_path / "test.infold"
    create_archive(fixture, out, {"_micro_mode": True, "_lean_mode": True})
    import zipfile
    with zipfile.ZipFile(out, "r") as zf:
        manifest = json.loads(zf.read("manifest.json").decode("utf-8"))
    assert manifest.get("family_membranes") is True or manifest.get("path_dna_folding") is True or manifest.get("metadata_table_fold") is True


def test_load_path_table_expands_path_dna(tmp_path):
    """load_path_table expands path_dna format to full paths."""
    from infold.engine.metadata_table_fold import load_path_table

    (tmp_path / "shared" / "metadata_tables").mkdir(parents=True)
    path_dna = {"r": ["tests/fixtures/"], "p": [[0, "a.py"], [0, "b.py"]]}
    (tmp_path / "shared" / "metadata_tables" / "path_table.json").write_text(
        '{"path_dna":' + __import__("json").dumps(path_dna, separators=(",", ":")) + "}",
        encoding="utf-8",
    )
    result = load_path_table(tmp_path)
    assert result is not None
    assert result == ["tests/fixtures/a.py", "tests/fixtures/b.py"]


def test_micro_archive_exact_reconstruction(tmp_path):
    """Micro archive with Phase 14A preserves exact reconstruction."""
    from infold.archive import create_archive, reconstruct_archive

    fixture = Path(__file__).parent / "fixtures" / "template_heavy"
    if not fixture.exists():
        pytest.skip("template_heavy fixture not found")
    out = tmp_path / "test.infold"
    restored = tmp_path / "restored"
    create_archive(fixture, out, {"_micro_mode": True, "_lean_mode": True})
    reconstruct_archive(out, restored)
    for f in fixture.rglob("*"):
        if f.is_file():
            rel = f.relative_to(fixture)
            rest = restored / rel
            assert rest.exists(), f"Missing {rel}"
            assert rest.read_bytes() == f.read_bytes(), f"Mismatch {rel}"
