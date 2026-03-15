"""Smoke tests for Origami fold engine."""

import tempfile
from pathlib import Path

import pytest

from core.intake.project_loader import load_project
from core.folding.fold_engine import FoldEngine
from core.types import ProjectSheet
from operators import get_default_operators


def test_load_project_empty_dir() -> None:
    """Load an empty directory."""
    with tempfile.TemporaryDirectory() as d:
        sheet = load_project(d)
        assert sheet.project_id
        assert sheet.file_nodes == []
        assert isinstance(sheet.folder_nodes, list)


def test_load_project_with_files() -> None:
    """Load a directory with a Python file."""
    with tempfile.TemporaryDirectory() as d:
        (Path(d) / "foo.py").write_text("print('hello')")
        sheet = load_project(d)
        assert len(sheet.file_nodes) == 1
        assert sheet.file_nodes[0].path == "foo.py"
        assert sheet.file_nodes[0].language == "python"
        assert "hello" in sheet.file_nodes[0].raw_text


def test_fold_engine_runs() -> None:
    """Fold engine runs without error on empty sheet."""
    sheet = ProjectSheet(project_id="test", file_nodes=[], folder_nodes=[])
    engine = FoldEngine(operators=get_default_operators())
    folded, records = engine.run(sheet)
    assert folded.project_id == sheet.project_id
    assert isinstance(records, list)


def test_operators_registered() -> None:
    """All 5 operators are registered."""
    ops = get_default_operators()
    assert len(ops) == 5
    ids = {op.operator_id() for op in ops}
    assert ids == {
        "exact_repetition",
        "template_skeleton",
        "symbol_table",
        "hierarchy_mirror",
        "dependency_motif",
    }
