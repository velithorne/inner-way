import { useEffect } from 'react';
import { motion } from 'framer-motion';
import { GlobeScene } from '../Globe/GlobeScene';
import { SubmissionPanel } from './SubmissionPanel';
import { LiveStatusBadge } from './LiveStatusBadge';
import { DebugControls } from './DebugControls';
import { useMockEngine } from '../../hooks/useMockEngine';
import { useGlowAccumulation } from '../../hooks/useGlowAccumulation';

export function AppShell() {
  useMockEngine();
  useGlowAccumulation();

  useEffect(() => {
    document.title = 'World Pulse';
  }, []);

  return (
    <div className="app-shell">
      <div className="canvas-container">
        <GlobeScene />
      </div>

      {/* Top header — feather-weight */}
      <motion.header
        className="app-header"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 1.6, delay: 0.4, ease: 'easeOut' }}
      >
        <div className="logo-area">
          <span className="logo-text">WORLD PULSE</span>
          <span className="logo-sub">Planetary Consciousness</span>
        </div>
        <LiveStatusBadge />
      </motion.header>

      {/* Bottom command dock */}
      <motion.div
        className="bottom-panel"
        initial={{ opacity: 0, y: 24 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 1.0, delay: 0.7, ease: 'easeOut' }}
      >
        <SubmissionPanel />
      </motion.div>

      {/* Debug — bottom-left icon */}
      <div className="debug-anchor">
        <DebugControls />
      </div>
    </div>
  );
}
