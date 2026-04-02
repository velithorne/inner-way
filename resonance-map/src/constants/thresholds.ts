// Magnetometer field strength thresholds (microtesla)
export const FIELD_WEAK_MAX = 30;         // < 30 µT = weak
export const FIELD_NORMAL_MAX = 60;       // 30–60 µT = normal
// > 60 µT = strong

// Anomaly detection
export const ANOMALY_DEVIATION_PERCENT = 0.15;   // 15% deviation triggers anomaly
export const ROLLING_AVERAGE_SECONDS = 30;
export const ROLLING_AVERAGE_SAMPLES = 300;      // at 10hz effective storage rate

// History buffer
export const WAVEFORM_HISTORY_SAMPLES = 300;     // ~60 seconds at 5hz display rate

// Calibration
export const CALIBRATION_DURATION_MS = 8000;

// Sensor update interval
export const MAGNETOMETER_UPDATE_MS = 1000 / 60; // 60hz

// Simulation mode sine wave parameters
export const SIMULATION_BASE_MAGNITUDE = 47;     // µT (Earth's average)
export const SIMULATION_AMPLITUDE = 8;
export const SIMULATION_FREQUENCY = 0.3;         // Hz

// Phase 2+ integration hooks (not yet implemented)
export const PHASE2_SCHUMANN_ENDPOINT = null;
export const PHASE3_SYNC_ENDPOINT = null;
