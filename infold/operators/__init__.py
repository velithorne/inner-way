"""Fold operators: Exact Repetition, Template Skeleton, Symbol Table, Hierarchy Mirror, Dependency Motif."""

from infold.operators.base import BaseOperator
from infold.operators.dependency_motif import DependencyMotifOperator
from infold.operators.exact_repetition import ExactRepetitionOperator
from infold.operators.hierarchy_mirror import HierarchyMirrorOperator
from infold.operators.noop_operator import NoOpOperator
from infold.operators.symbol_table import SymbolTableOperator
from infold.operators.template_skeleton import TemplateSkeletonOperator
from infold.operators.fold_echo import FoldEchoOperator
from infold.operators.mutation_chain import MutationChainOperator

__all__ = ["BaseOperator", "NoOpOperator", "ExactRepetitionOperator", "SymbolTableOperator", "TemplateSkeletonOperator", "MutationChainOperator", "FoldEchoOperator", "HierarchyMirrorOperator", "DependencyMotifOperator"]
