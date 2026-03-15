"""Fold operators."""

from operators.exact_repetition.operator import ExactRepetitionOperator
from operators.template_skeleton.operator import TemplateSkeletonOperator
from operators.symbol_table.operator import SymbolTableOperator
from operators.hierarchy_mirror.operator import HierarchyMirrorOperator
from operators.dependency_motif.operator import DependencyMotifOperator

__all__ = [
    "ExactRepetitionOperator",
    "TemplateSkeletonOperator",
    "SymbolTableOperator",
    "HierarchyMirrorOperator",
    "DependencyMotifOperator",
]


def get_default_operators() -> list:
    """Return all operators in build order."""
    return [
        ExactRepetitionOperator(),
        TemplateSkeletonOperator(),
        SymbolTableOperator(),
        HierarchyMirrorOperator(),
        DependencyMotifOperator(),
    ]
