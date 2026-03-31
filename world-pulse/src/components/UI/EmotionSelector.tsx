import { motion } from 'framer-motion';
import { EMOTIONS } from '../../data/emotions';
import { useWorldPulseStore } from '../../app/store';
import { hexWithAlpha } from '../../lib/colors';
import type { EmotionKey } from '../../types';

export function EmotionSelector() {
  const { selectedEmotion, setSelectedEmotion } = useWorldPulseStore();

  return (
    <div className="emotion-grid">
      {EMOTIONS.map((emotion) => {
        const isSelected = selectedEmotion === emotion.key;
        return (
          <motion.button
            key={emotion.key}
            className="emotion-chip"
            onClick={() => setSelectedEmotion(isSelected ? null : (emotion.key as EmotionKey))}
            animate={{
              boxShadow: isSelected
                ? `0 0 14px 3px ${hexWithAlpha(emotion.color, 0.45)}`
                : 'none',
              borderColor: isSelected ? emotion.color : 'rgba(255,255,255,0.08)',
              backgroundColor: isSelected
                ? hexWithAlpha(emotion.color, 0.15)
                : 'rgba(255,255,255,0.03)',
              color: isSelected ? emotion.color : 'rgba(255,255,255,0.5)',
            }}
            whileHover={{
              borderColor: hexWithAlpha(emotion.color, 0.55),
              backgroundColor: hexWithAlpha(emotion.color, 0.08),
              color: emotion.color,
              scale: 1.04,
            }}
            whileTap={{ scale: 0.95 }}
            transition={{ duration: 0.15 }}
            style={{
              border: '1px solid',
              borderRadius: '4px',
              padding: '6px 11px',
              fontSize: '10px',
              fontWeight: 500,
              letterSpacing: '0.09em',
              textTransform: 'uppercase',
              cursor: 'pointer',
              background: 'transparent',
            }}
          >
            {emotion.label}
          </motion.button>
        );
      })}
    </div>
  );
}
