"""Main application window."""

from __future__ import annotations

from pathlib import Path

from PySide6.QtWidgets import (
    QMainWindow,
    QWidget,
    QVBoxLayout,
    QHBoxLayout,
    QSplitter,
    QTabWidget,
    QPushButton,
    QLabel,
    QComboBox,
    QSpinBox,
    QDoubleSpinBox,
    QGroupBox,
    QTableWidget,
    QTableWidgetItem,
    QTextEdit,
    QFileDialog,
    QMessageBox,
    QFrame,
    QGridLayout,
    QCheckBox,
)
from PySide6.QtCore import Qt, QThread, pyqtSignal
from PySide6.QtGui import QFont

from falcor.core.config import AppConfig, DeviceConfig, ExperimentConfig
from falcor.core.paths import get_run_path
from falcor.devices.virtual_assembly import load_assembly
from falcor.experiments.experiment_plan import ExperimentPlan
from falcor.experiments.run_executor import RunExecutor
from falcor.storage.readers import read_parquet, read_json
from falcor.core.logging import get_logger

logger = get_logger("ui")


def _find_package_root() -> Path:
    """Find falcor package root (for examples)."""
    p = Path(__file__).resolve().parent.parent.parent
    return p


class RunWorker(QThread):
    """Background worker for run execution."""
    finished = pyqtSignal(str)
    error = pyqtSignal(str)

    def __init__(self, executor: RunExecutor, plan: ExperimentPlan, device_path: Path):
        super().__init__()
        self.executor = executor
        self.plan = plan
        self.device_path = device_path

    def run(self):
        try:
            run_id = self.executor.run(plan=self.plan, device_path=self.device_path)
            self.finished.emit(run_id)
        except Exception as e:
            self.error.emit(str(e))


class MainWindow(QMainWindow):
    """FALCOR main window."""

    def __init__(self):
        super().__init__()
        self.setWindowTitle("FALCOR v2 — Physics-First Discovery Harness")
        self.setMinimumSize(1200, 800)
        self.resize(1400, 900)

        self.config = AppConfig()
        self._run_worker: RunWorker | None = None
        self._assembly_path: Path | None = None

        central = QWidget()
        self.setCentralWidget(central)
        main_layout = QVBoxLayout(central)

        splitter = QSplitter(Qt.Horizontal)

        # Left panel: Device, Experiment, Run
        left = QWidget()
        left_layout = QVBoxLayout(left)
        left_layout.addWidget(self._build_device_panel())
        left_layout.addWidget(self._build_experiment_panel())
        left_layout.addWidget(self._build_run_controls())
        splitter.addWidget(left)

        # Center: 3D viewport
        self._viewport_3d = self._build_3d_viewport()
        splitter.addWidget(self._viewport_3d)

        # Right: Property editor, Sensor status, Calibration
        right = QWidget()
        right_layout = QVBoxLayout(right)
        right_layout.addWidget(self._build_property_panel())
        right_layout.addWidget(self._build_sensor_panel())
        splitter.addWidget(right)

        main_layout.addWidget(splitter, 3)

        # Bottom: Tabs
        bottom_tabs = QTabWidget()
        bottom_tabs.addTab(self._build_results_panel(), "Results")
        bottom_tabs.addTab(self._build_plot_panel(), "Plots")
        bottom_tabs.addTab(self._build_energy_audit_panel(), "Energy Audit")
        bottom_tabs.addTab(self._build_terminal_panel(), "Logs/Terminal")
        main_layout.addWidget(bottom_tabs, 1)

        # Demo button
        demo_btn = QPushButton("Run Demo Sweep")
        demo_btn.clicked.connect(self._run_demo)
        left_layout.insertWidget(0, demo_btn)

    def _build_device_panel(self) -> QGroupBox:
        g = QGroupBox("Device")
        layout = QVBoxLayout(g)
        self._device_combo = QComboBox()
        self._device_combo.addItem("(none)")
        pkg = _find_package_root()
        examples = pkg / "falcor" / "devices" / "examples"
        if examples.exists():
            for f in examples.glob("*.json"):
                self._device_combo.addItem(f.name, str(f))
        layout.addWidget(self._device_combo)
        btn = QPushButton("Load...")
        btn.clicked.connect(self._load_device)
        layout.addWidget(btn)
        return g

    def _build_experiment_panel(self) -> QGroupBox:
        g = QGroupBox("Experiment Plan")
        layout = QGridLayout(g)
        layout.addWidget(QLabel("Mode:"), 0, 0)
        self._mode_combo = QComboBox()
        self._mode_combo.addItems(["SIM", "FIELD"])
        layout.addWidget(self._mode_combo, 0, 1)
        layout.addWidget(QLabel("Freq start (Hz):"), 1, 0)
        self._freq_start = QDoubleSpinBox()
        self._freq_start.setRange(0.1, 10000)
        self._freq_start.setValue(1.0)
        layout.addWidget(self._freq_start, 1, 1)
        layout.addWidget(QLabel("Freq stop (Hz):"), 2, 0)
        self._freq_stop = QDoubleSpinBox()
        self._freq_stop.setRange(0.1, 10000)
        self._freq_stop.setValue(100.0)
        layout.addWidget(self._freq_stop, 2, 1)
        layout.addWidget(QLabel("Steps:"), 3, 0)
        self._freq_steps = QSpinBox()
        self._freq_steps.setRange(1, 1000)
        self._freq_steps.setValue(10)
        layout.addWidget(self._freq_steps, 3, 1)
        layout.addWidget(QLabel("Dwell (s):"), 4, 0)
        self._dwell = QDoubleSpinBox()
        self._dwell.setRange(0.1, 3600)
        self._dwell.setValue(5.0)
        layout.addWidget(self._dwell, 4, 1)
        layout.addWidget(QLabel("Settle (s):"), 5, 0)
        self._settle = QDoubleSpinBox()
        self._settle.setRange(0, 600)
        self._settle.setValue(2.0)
        layout.addWidget(self._settle, 5, 1)
        layout.addWidget(QLabel("Sample rate (Hz):"), 6, 0)
        self._sample_rate = QDoubleSpinBox()
        self._sample_rate.setRange(0.1, 1000)
        self._sample_rate.setValue(10.0)
        layout.addWidget(self._sample_rate, 6, 1)
        return g

    def _build_run_controls(self) -> QGroupBox:
        g = QGroupBox("Run Controls")
        layout = QVBoxLayout(g)
        self._run_btn = QPushButton("Run")
        self._run_btn.clicked.connect(self._start_run)
        layout.addWidget(self._run_btn)
        self._run_id_label = QLabel("Run ID: —")
        layout.addWidget(self._run_id_label)
        return g

    def _build_3d_viewport(self) -> QWidget:
        try:
            from falcor.ui.widgets.viewport_3d import Viewport3D
            return Viewport3D()
        except ImportError:
            return QLabel("3D viewport (PyVistaQt required):\nLoad device to render.")

    def _build_property_panel(self) -> QGroupBox:
        g = QGroupBox("Property Editor")
        layout = QVBoxLayout(g)
        self._prop_label = QLabel("Select a component")
        layout.addWidget(self._prop_label)
        return g

    def _build_sensor_panel(self) -> QGroupBox:
        g = QGroupBox("Sensor Status")
        layout = QVBoxLayout(g)
        self._sensor_label = QLabel("Mode: SIM — sensors simulated")
        layout.addWidget(self._sensor_label)
        return g

    def _build_results_panel(self) -> QWidget:
        w = QWidget()
        layout = QVBoxLayout(w)
        self._results_table = QTableWidget()
        self._results_table.setColumnCount(8)
        self._results_table.setHorizontalHeaderLabels(
            ["Step", "Freq (Hz)", "Mag", "T_mean", "Slope", "B", "Vib", "Pass"]
        )
        layout.addWidget(self._results_table)
        return w

    def _build_plot_panel(self) -> QWidget:
        w = QWidget()
        layout = QVBoxLayout(w)
        self._plot_label = QLabel("Select a run and metric to plot. Export PNG via button.")
        layout.addWidget(self._plot_label)
        export_btn = QPushButton("Export PNG")
        export_btn.clicked.connect(self._export_plot)
        layout.addWidget(export_btn)
        return w

    def _build_energy_audit_panel(self) -> QWidget:
        w = QWidget()
        layout = QVBoxLayout(w)
        self._audit_text = QTextEdit()
        self._audit_text.setReadOnly(True)
        self._audit_text.setPlaceholderText("Run an experiment to see energy audit.")
        layout.addWidget(self._audit_text)
        return w

    def _build_terminal_panel(self) -> QWidget:
        w = QWidget()
        layout = QVBoxLayout(w)
        self._log_text = QTextEdit()
        self._log_text.setReadOnly(True)
        self._log_text.setPlaceholderText("Log output will appear here.")
        layout.addWidget(self._log_text)
        return w

    def _load_device(self):
        path, _ = QFileDialog.getOpenFileName(
            self, "Load Virtual Assembly", "", "JSON (*.json)"
        )
        if path:
            try:
                load_assembly(path)
                self._assembly_path = Path(path)
                self._device_combo.addItem(Path(path).name, path)
                self._device_combo.setCurrentText(Path(path).name)
                if hasattr(self._viewport_3d, "load_assembly"):
                    self._viewport_3d.load_assembly(path)
            except Exception as e:
                QMessageBox.warning(self, "Load Error", str(e))

    def _get_plan(self) -> ExperimentPlan:
        return ExperimentPlan(
            mode="SIM" if self._mode_combo.currentText() == "SIM" else "FIELD",
            frequency_start_hz=self._freq_start.value(),
            frequency_stop_hz=self._freq_stop.value(),
            frequency_steps=self._freq_steps.value(),
            dwell_time_s=self._dwell.value(),
            settle_time_s=self._settle.value(),
            sample_rate_hz=self._sample_rate.value(),
        )

    def _get_device_path(self) -> Path | None:
        data = self._device_combo.currentData()
        if data:
            return Path(data)
        return self._assembly_path

    def _start_run(self):
        path = self._get_device_path()
        if not path or not path.exists():
            QMessageBox.warning(self, "Error", "Select a device first.")
            return
        plan = self._get_plan()
        self.config.device = DeviceConfig(path=str(path))
        self.config.experiment = ExperimentConfig(
            mode=plan.mode,
            frequency_sweep_start_hz=plan.frequency_start_hz,
            frequency_sweep_stop_hz=plan.frequency_stop_hz,
            frequency_sweep_steps=plan.frequency_steps,
            dwell_time_s=plan.dwell_time_s,
            settle_time_s=plan.settle_time_s,
            sample_rate_hz=plan.sample_rate_hz,
        )
        self._run_btn.setEnabled(False)
        self._run_worker = RunWorker(RunExecutor(self.config), plan, path)
        self._run_worker.finished.connect(self._on_run_finished)
        self._run_worker.error.connect(self._on_run_error)
        self._run_worker.start()

    def _on_run_finished(self, run_id: str):
        self._run_btn.setEnabled(True)
        self._run_id_label.setText(f"Run ID: {run_id}")
        self._load_results(run_id)

    def _on_run_error(self, msg: str):
        self._run_btn.setEnabled(True)
        QMessageBox.critical(self, "Run Error", msg)

    def _load_results(self, run_id: str):
        run_path = get_run_path(run_id, str(self.config.lab_results_dir))
        if not run_path.exists():
            return
        derived_p = run_path / "derived_metrics.parquet"
        if derived_p.exists():
            df = read_parquet(derived_p)
            self._results_table.setRowCount(len(df))
            for i, row in df.iterrows():
                for j, col in enumerate(["step", "frequency_hz", "magnet_state", "T_mean_K", "slope", "B_T", "vib_rms", "pass"]):
                    if col in df.columns:
                        val = row[col]
                        self._results_table.setItem(i, j, QTableWidgetItem(str(val)))
        audit_p = run_path / "audit_report.md"
        if audit_p.exists():
            self._audit_text.setPlainText(audit_p.read_text(encoding="utf-8"))

    def _run_demo(self):
        pkg = _find_package_root()
        device = pkg / "falcor" / "devices" / "examples" / "witches_hat_v1.json"
        plan_path = pkg / "plans" / "demo_sweep.json"
        if not device.exists():
            QMessageBox.warning(self, "Demo", f"Example device not found: {device}")
            return
        self._assembly_path = device
        for i in range(self._device_combo.count()):
            if self._device_combo.itemData(i) == str(device):
                self._device_combo.setCurrentIndex(i)
                break
        if hasattr(self._viewport_3d, "load_assembly"):
            self._viewport_3d.load_assembly(device)
        if plan_path.exists():
            data = read_json(plan_path)
            self._freq_start.setValue(data.get("frequency_start_hz", 1))
            self._freq_stop.setValue(data.get("frequency_stop_hz", 50))
            self._freq_steps.setValue(data.get("frequency_steps", 10))
            self._dwell.setValue(data.get("dwell_time_s", 3))
            self._settle.setValue(data.get("settle_time_s", 1))
        self._start_run()

    def _export_plot(self):
        path, _ = QFileDialog.getSaveFileName(self, "Export Plot", "", "PNG (*.png)")
        if path:
            # Placeholder: would render matplotlib to file
            QMessageBox.information(self, "Export", f"Plot export to {path} (placeholder)")
