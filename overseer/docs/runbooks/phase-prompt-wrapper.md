# Standard Claude prompt wrapper (build package §11.1)

```
PHASE: <ID>
OBJECTIVE: <one bounded objective>

READ FIRST:
- CLAUDE.md
- docs/contracts/*
- docs/gates/<ID>.md

CONSTRAINTS:
- Do not expand scope.
- Do not enable external writes/spending unless explicitly authorised.
- Preserve all accepted contracts.
- Add/update tests for every behavior changed.

DELIVER:
1. Implementation.
2. Automated test results.
3. Manual test instructions for me.
4. Evidence report with changed files and unresolved issues.
5. STOP. Do not begin the next phase.
```

Gate discipline (§19): when a phase is reported complete, compare the evidence to every gate
item. Any FAIL or unproven item keeps the phase open. UI phases need visual acceptance;
physical-sample phases need real-world inspection. Only after explicit acceptance should the
next phase prompt be issued.
