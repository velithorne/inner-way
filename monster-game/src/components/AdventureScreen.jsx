import { useGameStore } from '../store/gameStore'
import { ADVENTURE_NODES, getAdventureEnemy } from '../data/enemies'
import './AdventureScreen.css'

export function AdventureScreen({ onStartBattle, onBack }) {
  const adventureNode = useGameStore((s) => s.adventureNode)
  const level = useGameStore((s) => s.level)
  const advanceAdventureNode = useGameStore((s) => s.advanceAdventureNode)
  const applyAdventureReward = useGameStore((s) => s.applyAdventureReward)
  const addExp = useGameStore((s) => s.addExp)
  const resetAdventure = useGameStore((s) => s.resetAdventure)

  const currentNode = ADVENTURE_NODES[adventureNode]

  const handleEnterNode = () => {
    if (!currentNode) return

    if (currentNode.type === 'battle') {
      const enemy = getAdventureEnemy(level, currentNode)
      onStartBattle(enemy)
    } else if (currentNode.type === 'rest') {
      applyAdventureReward(currentNode.reward)
      advanceAdventureNode()
    } else if (currentNode.type === 'treasure') {
      addExp(currentNode.reward.exp)
      advanceAdventureNode()
    }
  }

  if (!currentNode) {
    return (
      <div className="adventure-screen">
        <h1>Adventure Complete! 🎉</h1>
        <p>You've reached the end of this adventure.</p>
        <button className="btn-restart" onClick={resetAdventure}>
          Start New Adventure
        </button>
        <button className="btn-back" onClick={onBack}>
          ← Back
        </button>
      </div>
    )
  }

  const isBattle = currentNode.type === 'battle'
  const isRest = currentNode.type === 'rest'
  const isTreasure = currentNode.type === 'treasure'

  return (
    <div className="adventure-screen">
      <h1>Adventure</h1>
      <p className="adventure-sub">Node {adventureNode + 1} of 7</p>

      <div className="adventure-path">
        {ADVENTURE_NODES.map((node, i) => (
          <div
            key={node.id}
            className={`path-node ${i < adventureNode ? 'passed' : ''} ${i === adventureNode ? 'current' : ''} ${node.type}`}
          >
            {node.type === 'battle' && <span>⚔️</span>}
            {node.type === 'rest' && <span>😴</span>}
            {node.type === 'treasure' && <span>💎</span>}
          </div>
        ))}
      </div>

      <div className={`adventure-node-card ${currentNode.type}`}>
        <h2>
          {isBattle && (currentNode.isBoss ? '👹 Boss Battle' : '⚔️ Battle')}
          {isRest && '😴 Rest Spot'}
          {isTreasure && '💎 Treasure'}
        </h2>
        {isBattle && (
          <>
            <p>Difficulty: {currentNode.difficulty}</p>
            <p>Bonus: +{currentNode.expBonus || 0} EXP</p>
          </>
        )}
        {isRest && (
          <p>+{currentNode.reward.energy} Energy, +{currentNode.reward.hunger} Hunger</p>
        )}
        {isTreasure && (
          <p>+{currentNode.reward.exp} EXP</p>
        )}
        <button className="btn-enter" onClick={handleEnterNode}>
          {isBattle ? 'Fight' : 'Collect'}
        </button>
      </div>

      <button className="btn-back" onClick={onBack}>
        ← Back
      </button>
    </div>
  )
}
