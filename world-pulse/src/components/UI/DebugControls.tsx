import { motion, AnimatePresence } from 'framer-motion';
import { useWorldPulseStore } from '../../app/store';
import { createCityPulse, createBurst } from '../../data/mockPulses';
import { DEMO_CITIES } from '../../data/demoLocations';
import type { RegionBias } from '../../types';

const REGION_OPTIONS: RegionBias[] = ['global', 'urban', 'asia', 'europe', 'americas'];

export function DebugControls() {
  const { showDebug, toggleDebug, mockEngineSettings, updateMockSettings, addPulse } =
    useWorldPulseStore();

  const handleCityPulse = (cityName: string) => {
    const pulse = createCityPulse(cityName);
    if (pulse) addPulse(pulse);
  };

  const handleStorm = () => {
    createBurst(12, 'urban').forEach(addPulse);
  };

  return (
    <div className="debug-container">
      {/* Icon-only circle button */}
      <button
        className="debug-toggle-btn"
        onClick={toggleDebug}
        title="Developer controls"
        aria-label="Toggle debug panel"
      >
        {showDebug ? '✕' : '⚙'}
      </button>

      <AnimatePresence>
        {showDebug && (
          <motion.div
            className="debug-panel glass-panel"
            initial={{ opacity: 0, y: 8, scale: 0.97 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 8, scale: 0.97 }}
            transition={{ duration: 0.22 }}
          >
            <h4 className="debug-title">Mock Engine</h4>

            <div className="debug-row">
              <label>Live Mode</label>
              <button
                className={`debug-btn ${mockEngineSettings.enabled ? 'active' : ''}`}
                onClick={() => updateMockSettings({ enabled: !mockEngineSettings.enabled })}
              >
                {mockEngineSettings.enabled ? 'ON' : 'OFF'}
              </button>
            </div>

            <div className="debug-row">
              <label>Interval</label>
              <input
                type="range" min={400} max={5000} step={100}
                value={mockEngineSettings.intervalMs}
                onChange={(e) => updateMockSettings({ intervalMs: Number(e.target.value) })}
              />
              <span>{mockEngineSettings.intervalMs}ms</span>
            </div>

            <div className="debug-row">
              <label>Burst %</label>
              <input
                type="range" min={0} max={1} step={0.05}
                value={mockEngineSettings.burstProbability}
                onChange={(e) => updateMockSettings({ burstProbability: Number(e.target.value) })}
              />
              <span>{Math.round(mockEngineSettings.burstProbability * 100)}%</span>
            </div>

            <div className="debug-row">
              <label>Burst Size</label>
              <input
                type="range" min={1} max={14} step={1}
                value={mockEngineSettings.burstSize}
                onChange={(e) => updateMockSettings({ burstSize: Number(e.target.value) })}
              />
              <span>{mockEngineSettings.burstSize}</span>
            </div>

            <div className="debug-row">
              <label>Region</label>
              <select
                value={mockEngineSettings.regionBias}
                onChange={(e) => updateMockSettings({ regionBias: e.target.value as RegionBias })}
                className="debug-select"
              >
                {REGION_OPTIONS.map((r) => (
                  <option key={r} value={r}>{r}</option>
                ))}
              </select>
            </div>

            <div className="debug-section-title">Cities</div>
            <div className="city-grid">
              {DEMO_CITIES.slice(0, 8).map((city) => (
                <button key={city.name} className="city-btn" onClick={() => handleCityPulse(city.name)}>
                  {city.name}
                </button>
              ))}
            </div>

            <button className="storm-btn" onClick={handleStorm}>
              ⚡ Storm Density
            </button>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
