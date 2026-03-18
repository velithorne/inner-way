"""Infold benchmark campaign: pack, profiling, export."""

from infold.benchmark.campaign import (
    run_benchmark_campaign,
    export_campaign_csv,
    export_campaign_json,
    export_campaign_markdown,
    export_campaign_tuning_report,
)
from infold.benchmark.pack import BENCHMARK_PACK, get_benchmark_datasets, ensure_stress_datasets
from infold.benchmark.profile_comparison import (
    run_profile_comparison,
    build_comparison_summary,
    export_comparison_csv,
    export_comparison_markdown,
    export_auto_vs_best_csv,
)
from infold.benchmark.creature_comparison import (
    run_creature_comparison,
    creature_comparison_to_markdown,
)
from infold.benchmark.competitive_matrix import (
    run_competitive_matrix,
    export_matrix_csv,
    export_matrix_json,
    export_matrix_markdown,
    build_competitive_summary,
    build_feature_matrix,
)

__all__ = [
    "run_benchmark_campaign",
    "export_campaign_csv",
    "export_campaign_json",
    "export_campaign_markdown",
    "export_campaign_tuning_report",
    "run_profile_comparison",
    "build_comparison_summary",
    "export_comparison_csv",
    "export_comparison_markdown",
    "export_auto_vs_best_csv",
    "BENCHMARK_PACK",
    "get_benchmark_datasets",
    "ensure_stress_datasets",
    "run_creature_comparison",
    "creature_comparison_to_markdown",
    "run_competitive_matrix",
    "export_matrix_csv",
    "export_matrix_json",
    "export_matrix_markdown",
    "build_competitive_summary",
    "build_feature_matrix",
]
