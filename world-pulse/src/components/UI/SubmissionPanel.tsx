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
    }, 3200);
  };

  return (
    <div className="submission-panel glass-panel">
      <AnimatePresence mode="wait">
        {submitted ? (
          <motion.div
            key="confirmation"
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.95 }}
            transition={{ duration: 0.4 }}
            className="confirmation-state"
            style={{
              textAlign: 'center',
              padding: '24px 0',
            }}
          >
            <motion.div
              className="pulse-dot-confirm"
              animate={{
                scale: [1, 1.6, 1],
                opacity: [1, 0.5, 1],
              }}
              transition={{ duration: 1.4, repeat: 2 }}
              style={{
                width: 16,
                height: 16,
                borderRadius: '50%',
                background: submittedColor,
                margin: '0 auto 16px',
                boxShadow: `0 0 24px 8px ${submittedColor}55`,
              }}
            />
            <p style={{ color: 'rgba(255,255,255,0.85)', fontSize: 14, margin: 0 }}>
              Your pulse reached the world.
            </p>
          </motion.div>
        ) : (
          <motion.div
            key="form"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.3 }}
          >
            <p className="prompt-text">
              One word. Right now. How does the world feel to you?
            </p>
            <EmotionSelector />
            <div className="submit-row">
              <motion.button
                className="submit-btn"
                onClick={handleSubmit}
                disabled={!selectedEmotion}
                animate={{
                  opacity: selectedEmotion ? 1 : 0.35,
                  boxShadow: selectedEmotion
                    ? `0 0 20px 4px ${emotionToHex(selectedEmotion!)}44`
                    : 'none',
                }}
                whileHover={selectedEmotion ? { scale: 1.03 } : {}}
                whileTap={selectedEmotion ? { scale: 0.97 } : {}}
                transition={{ duration: 0.2 }}
              >
                Send Pulse
              </motion.button>
              <span className="anonymous-note">Anonymous. One word. One pulse.</span>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
