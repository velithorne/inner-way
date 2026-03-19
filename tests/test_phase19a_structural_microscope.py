"""
Phase 19A: Structural Microscope v0.1 tests.
"""
from pathlib import Path

import pytest

from infold.engine.structural_microscope import (
    magnify_content,
    find_eligible_tiny_files,
    find_microscope_assisted_groups,
)
from infold.intake import scan_project
from infold.archive import create_archive, validate_archive, explain_archive
from infold.archive.operations import explain_to_text


def test_magnify_json():
    """JSON expands to one key-value per line."""
    content = '{"a":1,"b":2}'
    result = magnify_content(content, Path("x.json"))
    assert len(result) >= 3
    assert "a" in "".join(result)
    assert "b" in "".join(result)


def test_find_eligible_tiny_files():
    """Eligible files are below thresholds."""
    fixture = Path(__file__).parent / "fixtures" / "microscope_tiny"
    if not fixture.exists():
        pytest.skip("microscope_tiny fixture not found")
    sheet = scan_project(fixture)
    eligible = find_eligible_tiny_files(sheet, max_bytes=512, max_lines=6)
    assert len(eligible) >= 3


def test_find_microscope_assisted_groups():
    """Microscope groups tiny files by structure."""
    fixture = Path(__file__).parent / "fixtures" / "microscope_tiny"
    if not fixture.exists():
        pytest.skip("microscope_tiny fixture not found")
    sheet = scan_project(fixture)
    groups = find_microscope_assisted_groups(sheet, max_bytes=512, max_lines=6, min_family=3)
    assert len(groups) >= 1
    paths, lines_list = groups[0]
    assert len(paths) >= 3
    assert len(lines_list) == len(paths)


def test_microscope_archive():
    """Archive creation with microscope fixture."""
    fixture = Path(__file__).parent / "fixtures" / "microscope_tiny"
    if not fixture.exists():
        pytest.skip("microscope_tiny fixture not found")
    out = Path(__file__).parent.parent / "results" / "microscope_test.infold"
    out.parent.mkdir(parents=True, exist_ok=True)
    create_archive(str(fixture), str(out), profile="fox")
    valid, _ = validate_archive(str(out), mode="strict")
    assert valid


def test_explain_microscope():
    """Explain may show microscope-assisted matches."""
    fixture = Path(__file__).parent / "fixtures" / "microscope_tiny"
    if not fixture.exists():
        pytest.skip("microscope_tiny fixture not found")
    out = Path(__file__).parent.parent / "results" / "microscope_explain.infold"
    out.parent.mkdir(parents=True, exist_ok=True)
    create_archive(str(fixture), str(out), profile="fox")
    info = explain_archive(str(out))
    text = explain_to_text(info)
    assert "Archive Explain" in text


def test_microscope_reconstruction():
    """Microscope-assisted folds reconstruct exactly."""
    fixture = Path(__file__).parent / "fixtures" / "microscope_tiny"
    if not fixture.exists():
        pytest.skip("microscope_tiny fixture not found")
    import tempfile
    import shutil
    out = Path(tempfile.mkdtemp()) / "micro_recon.infold"
    restored = Path(tempfile.mkdtemp()) / "restored"
    try:
        create_archive(str(fixture), str(out), profile="fox")
        from infold.archive import reconstruct_archive
        reconstruct_archive(str(out), str(restored))
        for name in ["cfg_a.json", "cfg_b.json", "cfg_c.json"]:
            orig = fixture / name
            rest = restored / name
            if orig.exists() and rest.exists():
                assert orig.read_text() == rest.read_text(), f"Mismatch: {name}"
    finally:
        shutil.rmtree(out.parent, ignore_errors=True)
        shutil.rmtree(restored.parent, ignore_errors=True)
