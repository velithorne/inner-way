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
                ? `0 0 18px 4px ${hexWithAlpha(emotion.color, 0.55)}, 0 0 6px 1px ${hexWithAlpha(emotion.color, 0.3)}`
                : `0 0 0px 0px ${hexWithAlpha(emotion.color, 0)}`,
              borderColor: isSelected ? emotion.color : 'rgba(255,255,255,0.1)',
              backgroundColor: isSelected
                ? hexWithAlpha(emotion.color, 0.18)
                : 'rgba(255,255,255,0.04)',
              color: isSelected ? emotion.color : 'rgba(255,255,255,0.6)',
            }}
            whileHover={{
              borderColor: hexWithAlpha(emotion.color, 0.7),
              backgroundColor: hexWithAlpha(emotion.color, 0.1),
              color: emotion.color,
              scale: 1.04,
            }}
            whileTap={{ scale: 0.96 }}
            transition={{ duration: 0.2 }}
            style={{
              border: '1px solid',
              borderRadius: '4px',
              padding: '8px 14px',
              fontSize: '12px',
              fontWeight: 500,
              letterSpacing: '0.08em',
              textTransform: 'uppercase',
              cursor: 'pointer',
              background: 'transparent',
              transition: 'all 0.2s',
            }}
          >
            {emotion.label}
          </motion.button>
        );
      })}
    </div>
  );
}
