"""Reporting and benchmark layer: summaries, diagnostics, baseline comparisons."""

from infold.reporting.benchmark import benchmark_to_text, run_benchmark
from infold.reporting.benchmark_suite import benchmark_suite_to_text, run_benchmark_suite
from infold.reporting.report import build_report, export_report, report_to_json, report_to_text

__all__ = [
    "build_report",
    "report_to_json",
    "report_to_text",
    "export_report",
    "run_benchmark",
    "benchmark_to_text",
    "run_benchmark_suite",
    "benchmark_suite_to_text",
]
