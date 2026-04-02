# FALCOR v2 Quick Start

## Install

```bash
cd falcor_v2
pip install -e .
```

For development (pytest, ruff, black):

```bash
pip install -e ".[dev]"
```

## Run SIM Demo

### GUI

```bash
falcor gui
```

On first launch, click **Run Demo Sweep** to:
- Load `witches_hat_v1.json` virtual assembly
- Run a small frequency sweep (10 frequencies, 2 magnet states)
- Generate plots and audit report

### CLI

```bash
falcor run --device falcor/devices/examples/witches_hat_v1.json --mode SIM --freq-steps 10
```

Results are written to `lab_results/runs/<run_id>/`.

## View Results

```bash
# Print audit report
falcor report --run <run_id>

# Export to CSV
falcor export --run <run_id> --format csv
```

## Run Tests

```bash
pytest tests/ -v
```

## Directory Layout After Demo

```
lab_results/
  index.parquet          # Run index
  runs/
    <run_id>/
      manifest.json
      config_snapshot.json
      raw_timeseries.parquet
      derived_metrics.parquet
      audit_report.md
      audit_report.html
      plots/
      exports/
```
