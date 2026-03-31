import { motion } from 'framer-motion';
import { useWorldPulseStore } from '../../app/store';

export function LiveStatusBadge() {
  const { pulses, mockEngineSettings } = useWorldPulseStore();
  const isLive = mockEngineSettings.enabled;

  return (
    <div className="live-badge">
      <motion.div
        animate={{
          opacity: isLive ? [1, 0.25, 1] : 0.25,
          scale:   isLive ? [1, 1.4, 1]  : 1,
        }}
        transition={{ duration: 2.2, repeat: Infinity, ease: 'easeInOut' }}
        style={{
          width: 5,
          height: 5,
          borderRadius: '50%',
          background: isLive ? '#22c55e' : '#4b5563',
          flexShrink: 0,
        }}
      />
      <span className="live-label">
        {isLive ? 'live sim' : 'paused'} · {pulses.length}
      </span>
    </div>
  );
}
