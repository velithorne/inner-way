import { useState } from 'react'
import { MonsterCreation } from './components/MonsterCreation'
import { HomeScreen } from './components/HomeScreen'
import { BattleScene } from './components/BattleScene'
import { useGameStore } from './store/gameStore'
import './App.css'

function App() {
  const hasMonster = useGameStore((s) => s.hasMonster)
  const dirtyFromBattle = useGameStore((s) => s.dirtyFromBattle)
  const [screen, setScreen] = useState(hasMonster() ? 'home' : 'create')
  const [inBattle, setInBattle] = useState(false)

  const handleMonsterCreated = () => setScreen('home')
  const handleStartBattle = () => setInBattle(true)
  const handleExitBattle = () => {
    dirtyFromBattle()
    setInBattle(false)
  }

  if (inBattle) {
    return <BattleScene onExit={handleExitBattle} />
  }

  if (screen === 'create') {
    return (
      <div className="app">
        <MonsterCreation onComplete={handleMonsterCreated} />
      </div>
    )
  }

  return (
    <div className="app">
      <HomeScreen
        onBattle={handleStartBattle}
        onNewMonster={() => setScreen('create')}
      />
    </div>
  )
}

export default App
