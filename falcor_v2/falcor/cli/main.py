"""CLI entry point."""

from __future__ import annotations

import sys
from pathlib import Path

from falcor.cli.commands import run_cmd, export_cmd, report_cmd, calibrate_cmd, gui_cmd


def main() -> int:
    """Main CLI entry."""
    args = sys.argv[1:]
    if not args:
        print("FALCOR v2 — Physics-First Discovery Harness")
        print("Usage: falcor <command> [options]")
        print("Commands: run, export, report, calibrate, gui")
        return 0

    cmd = args[0].lower()
    rest = args[1:]

    if cmd == "run":
        return run_cmd(rest)
    if cmd == "export":
        return export_cmd(rest)
    if cmd == "report":
        return report_cmd(rest)
    if cmd == "calibrate":
        return calibrate_cmd(rest)
    if cmd == "gui":
        return gui_cmd(rest)

    print(f"Unknown command: {cmd}")
    return 1
