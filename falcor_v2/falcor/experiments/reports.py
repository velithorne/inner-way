"""Report generation: audit report, markdown, HTML."""

from __future__ import annotations

from pathlib import Path
from typing import Any

from falcor.experiments.energy_audit import EnergyAuditResult


def render_audit_markdown(audit: EnergyAuditResult, run_id: str) -> str:
    """Render energy audit as markdown."""
    lines = [
        f"# Energy Audit Report — Run {run_id}",
        "",
        "## Summary",
        "",
        f"| Bucket | Value (W) | Uncertainty | Label |",
        f"|--------|----------|-------------|-------|",
        f"| Pin | {audit.Pin.value:.3f} | ±{audit.Pin.uncertainty:.3f} | {audit.Pin.label or '-'} |",
        f"| Pmech | {audit.Pmech.value:.3f} | ±{audit.Pmech.uncertainty:.3f} | {audit.Pmech.label or '-'} |",
        f"| Pthermal | {audit.Pthermal.value:.3f} | ±{audit.Pthermal.uncertainty:.3f} | {audit.Pthermal.label or '-'} |",
        f"| PEM | {audit.PEM.value:.3f} | ±{audit.PEM.uncertainty:.3f} | {audit.PEM.label or '-'} |",
        f"| Ploss | {audit.Ploss.value:.3f} | ±{audit.Ploss.uncertainty:.3f} | {audit.Ploss.label or '-'} |",
        f"| unknown/unmodeled | {audit.unknown.value:.3f} | ±{audit.unknown.uncertainty:.3f} | {audit.unknown.label or '-'} |",
        "",
        "## Flags",
        "",
    ]
    if audit.flags:
        for f in audit.flags:
            lines.append(f"- {f}")
    else:
        lines.append("- None")
    lines.extend(["", f"**Stable:** {audit.is_stable}", ""])
    return "\n".join(lines)


def render_audit_html(audit: EnergyAuditResult, run_id: str) -> str:
    """Render energy audit as HTML."""
    md = render_audit_markdown(audit, run_id)
    # Simple HTML wrapper
    html = f"""<!DOCTYPE html>
<html>
<head><meta charset="utf-8"><title>Audit — {run_id}</title></head>
<body>
<pre style="font-family: monospace; white-space: pre-wrap;">{md}</pre>
</body>
</html>"""
    return html
