import { motion } from 'framer-motion';
import { useWorldPulseStore } from '../../app/store';

export function LiveStatusBadge() {
  const { pulses, mockEngineSettings } = useWorldPulseStore();
  const isLive = mockEngineSettings.enabled;
  const count = pulses.length;

  return (
    <div className="live-badge">
      <motion.div
        className="live-dot"
        animate={{
          opacity: isLive ? [1, 0.3, 1] : 0.3,
          scale: isLive ? [1, 1.3, 1] : 1,
        }}
        transition={{
          duration: 2,
          repeat: Infinity,
          ease: 'easeInOut',
        }}
        style={{
          width: 6,
          height: 6,
          borderRadius: '50%',
          background: isLive ? '#22c55e' : '#6b7280',
          flexShrink: 0,
        }}
      />
      <span className="live-label">
        {isLive ? 'Simulated Live' : 'Paused'} &middot; {count} active
      </span>
    </div>
  );
}
