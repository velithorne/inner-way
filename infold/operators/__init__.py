"""Fold operators: Exact Repetition, Template Skeleton, Symbol Table, Hierarchy Mirror, Dependency Motif."""

from infold.operators.base import BaseOperator
from infold.operators.exact_repetition import ExactRepetitionOperator
from infold.operators.noop_operator import NoOpOperator
from infold.operators.template_skeleton import TemplateSkeletonOperator

__all__ = ["BaseOperator", "NoOpOperator", "ExactRepetitionOperator", "TemplateSkeletonOperator"]
