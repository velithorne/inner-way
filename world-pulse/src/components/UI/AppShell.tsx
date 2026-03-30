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
      {/* Full-screen 3D Canvas */}
      <div className="canvas-container">
        <GlobeScene />
      </div>

      {/* Top header */}
      <motion.header
        className="app-header"
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 1.2, delay: 0.3, ease: 'easeOut' }}
      >
        <div className="logo-area">
          <span className="logo-text">WORLD PULSE</span>
          <span className="logo-sub">PLANETARY CONSCIOUSNESS INTERFACE</span>
        </div>
        <LiveStatusBadge />
      </motion.header>

      {/* Bottom panel */}
      <motion.div
        className="bottom-panel"
        initial={{ opacity: 0, y: 30 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 1.0, delay: 0.6, ease: 'easeOut' }}
      >
        <SubmissionPanel />
      </motion.div>

      {/* Debug panel - bottom right */}
      <div className="debug-anchor">
        <DebugControls />
      </div>
    </div>
  );
}
