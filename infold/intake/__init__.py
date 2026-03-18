"""Intake layer: scans folders, classifies files, loads text, builds project inventory."""

from infold.intake.scanner import compute_scope_metrics, scan_project

__all__ = ["compute_scope_metrics", "scan_project"]
