import { useState } from 'react'
import { PERSONALITY_QUESTIONS } from '../data/questions'
import './PersonalityQuiz.css'

export function PersonalityQuiz({ onComplete }) {
  const [currentIndex, setCurrentIndex] = useState(0)
  const [answers, setAnswers] = useState({})

  const question = PERSONALITY_QUESTIONS[currentIndex]
  const isLast = currentIndex === PERSONALITY_QUESTIONS.length - 1

  const handleAnswer = (option) => {
    const newAnswers = { ...answers, [question.id]: option }
    setAnswers(newAnswers)

    if (isLast) {
      onComplete(newAnswers)
    } else {
      setCurrentIndex((i) => i + 1)
    }
  }

  return (
    <div className="personality-quiz">
      <div className="quiz-progress">
        <div
          className="quiz-progress-bar"
          style={{ width: `${((currentIndex + 1) / PERSONALITY_QUESTIONS.length) * 100}%` }}
        />
      </div>

      <h2>{question.question}</h2>

      <div className="quiz-options">
        {question.options.map((option) => (
          <button
            key={option}
            className="quiz-option"
            onClick={() => handleAnswer(option)}
          >
            {option}
          </button>
        ))}
      </div>

      <p className="quiz-step">
        {currentIndex + 1} of {PERSONALITY_QUESTIONS.length}
      </p>
    </div>
  )
}
