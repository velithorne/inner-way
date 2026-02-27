import { useState } from 'react'
import { MonsterCreation } from './components/MonsterCreation'
import { HomeScreen } from './components/HomeScreen'
import { BattleMenuScreen } from './components/BattleMenuScreen'
import { CampaignScreen } from './components/CampaignScreen'
import { AdventureScreen } from './components/AdventureScreen'
import { BattleScene } from './components/BattleScene'
import { useGameStore } from './store/gameStore'
import { generateWildEnemy } from './data/enemies'
import './App.css'

function App() {
  const hasMonster = useGameStore((s) => s.hasMonster)
  const dirtyFromBattle = useGameStore((s) => s.dirtyFromBattle)
  const level = useGameStore((s) => s.level)

  const [screen, setScreen] = useState(hasMonster() ? 'home' : 'create')
  const [battleState, setBattleState] = useState(null)

  const handleMonsterCreated = () => setScreen('home')

  const handleBattleClick = () => setScreen('battleMenu')

  const handleWildBattle = () => {
    const enemy = generateWildEnemy(level)
    setBattleState({ enemy, mode: 'wild' })
  }

  const handleCampaignBattle = (enemy) => {
    setBattleState({ enemy, mode: 'campaign' })
  }

  const handleAdventureBattle = (enemy) => {
    setBattleState({ enemy, mode: 'adventure' })
  }

  const handleExitBattle = () => {
    dirtyFromBattle()
    setBattleState(null)
  }

  if (battleState) {
    return (
      <BattleScene
        onExit={handleExitBattle}
        enemy={battleState.enemy}
        battleMode={battleState.mode}
      />
    )
  }

  if (screen === 'battleMenu') {
    return (
      <div className="app">
        <BattleMenuScreen
          onWildBattle={handleWildBattle}
          onCampaign={() => setScreen('campaign')}
          onAdventure={() => setScreen('adventure')}
          onBack={() => setScreen('home')}
        />
      </div>
    )
  }

  if (screen === 'campaign') {
    return (
      <div className="app">
        <CampaignScreen
          onStartBattle={handleCampaignBattle}
          onBack={() => setScreen('battleMenu')}
        />
      </div>
    )
  }

  if (screen === 'adventure') {
    return (
      <div className="app">
        <AdventureScreen
          onStartBattle={handleAdventureBattle}
          onBack={() => setScreen('battleMenu')}
        />
      </div>
    )
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
        onBattle={handleBattleClick}
        onNewMonster={() => setScreen('create')}
      />
    </div>
  )
}

export default App
