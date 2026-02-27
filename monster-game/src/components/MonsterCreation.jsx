import { useState } from 'react'
import { FaceCapture } from './FaceCapture'
import { PersonalityQuiz } from './PersonalityQuiz'
import { MonsterPreview } from './MonsterPreview'
import { generateMonsterFromData } from '../utils/monsterGenerator'
import { useGameStore } from '../store/gameStore'
import './MonsterCreation.css'

const STEPS = ['face', 'quiz', 'preview']

export function MonsterCreation({ onComplete }) {
  const [step, setStep] = useState(0)
  const [faceData, setFaceData] = useState(null)
  const [answers, setAnswers] = useState(null)
  const createMonster = useGameStore((s) => s.createMonster)

  const handleFaceCapture = (data) => {
    setFaceData(data)
    setStep(1)
  }

  const handleFaceSkip = () => {
    setFaceData(null)
    setStep(1)
  }

  const handleQuizComplete = (quizAnswers) => {
    setAnswers(quizAnswers)
    setStep(2)
  }

  const handleConfirm = () => {
    const monster = generateMonsterFromData(faceData, answers ? Object.values(answers) : [])
    createMonster(monster)
    onComplete()
  }

  return (
    <div className="monster-creation">
      <div className="creation-header">
        <h1>Create Your Monster</h1>
        <p>Your face + personality = your unique companion</p>
      </div>

      {STEPS[step] === 'face' && (
        <FaceCapture onCapture={handleFaceCapture} onSkip={handleFaceSkip} />
      )}

      {STEPS[step] === 'quiz' && (
        <PersonalityQuiz onComplete={handleQuizComplete} />
      )}

      {STEPS[step] === 'preview' && (
        <MonsterPreview
          monster={generateMonsterFromData(faceData, answers ? Object.values(answers) : [])}
          onConfirm={handleConfirm}
        />
      )}
    </div>
  )
}
