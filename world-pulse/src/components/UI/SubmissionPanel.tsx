import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { EmotionSelector } from './EmotionSelector';
import { useWorldPulseStore } from '../../app/store';
import { createUserPulse } from '../../data/mockPulses';
import { emotionToHex } from '../../lib/colors';

export function SubmissionPanel() {
  const { selectedEmotion, addPulse, setSelectedEmotion } = useWorldPulseStore();
  const [submitted, setSubmitted] = useState(false);
  const [submittedColor, setSubmittedColor] = useState('');

  const handleSubmit = () => {
    if (!selectedEmotion) return;
    const pulse = createUserPulse(selectedEmotion);
    addPulse(pulse);
    setSubmittedColor(emotionToHex(selectedEmotion));
    setSubmitted(true);
    setTimeout(() => {
      setSubmitted(false);
      setSelectedEmotion(null);
    }, 3000);
  };

  return (
    <div className="submission-panel glass-panel">
      <AnimatePresence mode="wait">
        {submitted ? (
          <motion.div
            key="confirmation"
            initial={{ opacity: 0, scale: 0.96 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.35 }}
            className="confirmation-state"
          >
            <motion.div
              animate={{ scale: [1, 1.7, 1], opacity: [1, 0.4, 1] }}
              transition={{ duration: 1.3, repeat: 2 }}
              style={{
                width: 12,
                height: 12,
                borderRadius: '50%',
                background: submittedColor,
                margin: '0 auto 12px',
                boxShadow: `0 0 20px 6px ${submittedColor}44`,
              }}
            />
            <p style={{ color: 'rgba(255,255,255,0.7)', fontSize: 12, margin: 0, letterSpacing: '0.04em' }}>
              Your pulse reached the world.
            </p>
          </motion.div>
        ) : (
          <motion.div
            key="form"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.25 }}
          >
            <p className="prompt-text">One word. Right now. How does the world feel?</p>
            <EmotionSelector />
            <div className="submit-row">
              <motion.button
                className="submit-btn"
                onClick={handleSubmit}
                disabled={!selectedEmotion}
                animate={{
                  opacity: selectedEmotion ? 1 : 0.3,
                  boxShadow: selectedEmotion
                    ? `0 0 16px 3px ${emotionToHex(selectedEmotion!)}33`
                    : 'none',
                }}
                whileHover={selectedEmotion ? { scale: 1.03 } : {}}
                whileTap={selectedEmotion ? { scale: 0.97 } : {}}
                transition={{ duration: 0.18 }}
              >
                Send Pulse
              </motion.button>
              <span className="anonymous-note">Anonymous · One pulse</span>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
