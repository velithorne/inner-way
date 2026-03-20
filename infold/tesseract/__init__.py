"""Phase 10: Tesseract Fold v0.1 — multi-dimensional family identity layer."""

from infold.tesseract.planner import (
    compute_tesseract_planner_profile,
    compute_dimension_strengths,
    tesseract_planner_summary,
)
from infold.tesseract.execution_plan import (
    generate_execution_plan,
    execution_plan_allows_metadata_followup,
    execution_plan_summary,
)
from infold.tesseract.signature import (
    build_tesseract_signature,
    build_tesseract_signatures_from_archive,
    build_tesseract_from_lineage_entry,
    get_tesseract_for_match,
    tesseract_summary,
    TESSERACT_DIMENSIONS,
)

__all__ = [
    "compute_tesseract_planner_profile",
    "compute_dimension_strengths",
    "tesseract_planner_summary",
    "generate_execution_plan",
    "execution_plan_allows_metadata_followup",
    "execution_plan_summary",
    "build_tesseract_signature",
    "build_tesseract_signatures_from_archive",
    "build_tesseract_from_lineage_entry",
    "get_tesseract_for_match",
    "tesseract_summary",
    "TESSERACT_DIMENSIONS",
]
