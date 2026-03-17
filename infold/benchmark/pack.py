"""
Benchmark pack: correctness fixtures, realistic repos, synthetic stress datasets.

Defines datasets for benchmark campaign with metadata.
"""

from pathlib import Path
from typing import Any

# Correctness fixtures: small, known-good datasets for validation
CORRECTNESS_FIXTURES = [
    ("tests/fixtures/duplicate_python", "duplicate-heavy-python"),
    ("tests/fixtures/template_heavy", "template-heavy"),
    ("tests/fixtures/config_heavy", "config-heavy"),
    ("tests/fixtures/mixed_project", "mixed-small"),
    ("tests/fixtures/hierarchy_mirror", "hierarchy-mirror"),
    ("tests/fixtures/dependency_motif", "dependency-motif"),
]

# Realistic small/medium repos
REALISTIC_FIXTURES = [
    (".", "infold-workspace"),
]

# Byte Fold focused datasets (repeated opaque, large text, version-like, mixed low-structure)
BYTE_FOLD_FIXTURES = [
    ("tests/fixtures/byte_fold", "byte-fold-opaque"),
    ("tests/fixtures/byte_fold_large_text", "byte-fold-large-text"),
    ("tests/fixtures/byte_fold_version_like", "byte-fold-version-like"),
]

# Synthetic stress datasets (generated if missing)
SyntheticStressConfig = dict[str, Any]
STRESS_DATASETS: list[tuple[str, str, SyntheticStressConfig]] = [
    ("benchmark/synthetic/duplicate_stress", "duplicate-stress", {"n_files": 50, "file_size": 200}),
    ("benchmark/synthetic/template_stress", "template-stress", {"n_files": 30, "template_variants": 10}),
]


def get_benchmark_datasets(base_path: Path) -> list[tuple[Path, str, str]]:
    """
    Return list of (path, dataset_id, category) for benchmark pack.
    category: correctness | realistic | stress | byte_fold
    """
    datasets: list[tuple[Path, str, str]] = []
    for rel, did in CORRECTNESS_FIXTURES:
        p = base_path / rel
        if p.exists():
            datasets.append((p, did, "correctness"))
    for rel, did in REALISTIC_FIXTURES:
        p = base_path / rel
        if p.exists():
            datasets.append((p, did, "realistic"))
    for rel, did in BYTE_FOLD_FIXTURES:
        p = base_path / rel
        if p.exists():
            datasets.append((p, did, "byte_fold"))
    for rel, did, _ in STRESS_DATASETS:
        p = base_path / rel
        if p.exists():
            datasets.append((p, did, "stress"))
    return datasets


def ensure_stress_datasets(base_path: Path) -> list[tuple[Path, str]]:
    """
    Create synthetic stress datasets if missing or empty. Returns list of (path, dataset_id).
    """
    created: list[tuple[Path, str]] = []
    for rel, did, cfg in STRESS_DATASETS:
        p = base_path / rel
        p.mkdir(parents=True, exist_ok=True)
        existing = list(p.glob("*")) if p.exists() else []
        if existing:
            continue
        if "duplicate-stress" in did:
            _create_duplicate_stress(p, cfg)
        elif "template-stress" in did:
            _create_template_stress(p, cfg)
        created.append((p, did))
    return created


def _create_duplicate_stress(path: Path, cfg: dict) -> None:
    """Create many duplicate files for stress testing."""
    n = cfg.get("n_files", 50)
    size = cfg.get("file_size", 200)
    content = "x" * size + "\n# comment\n"
    for i in range(n):
        (path / f"dup_{i:03d}.py").write_text(content, encoding="utf-8")


def _create_template_stress(path: Path, cfg: dict) -> None:
    """Create template-heavy files for stress testing."""
    n = cfg.get("n_files", 30)
    variants = cfg.get("template_variants", 10)
    scaffold = '''def handler_{}():
    """Process request {}."""
    return "ok_{}"
'''
    for i in range(n):
        v = i % variants
        (path / f"handler_{i:03d}.py").write_text(
            scaffold.format(v, i, v),
            encoding="utf-8",
        )


BENCHMARK_PACK = {
    "correctness": CORRECTNESS_FIXTURES,
    "realistic": REALISTIC_FIXTURES,
    "byte_fold": BYTE_FOLD_FIXTURES,
    "stress": STRESS_DATASETS,
}
