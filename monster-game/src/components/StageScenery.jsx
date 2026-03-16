import './StageScenery.css'

const ELEMENT_SCENERY = {
  fire: (
    <>
      <div className="scenery-ground scenery-lava" />
      <div className="scenery-rock scenery-rock-1" />
      <div className="scenery-rock scenery-rock-2" />
      <div className="scenery-flame scenery-flame-1" />
      <div className="scenery-flame scenery-flame-2" />
      <div className="scenery-flame scenery-flame-3" />
    </>
  ),
  water: (
    <>
      <div className="scenery-ground scenery-water" />
      <div className="scenery-wave scenery-wave-1" />
      <div className="scenery-wave scenery-wave-2" />
      <div className="scenery-bubble scenery-bubble-1" />
      <div className="scenery-bubble scenery-bubble-2" />
      <div className="scenery-bubble scenery-bubble-3" />
    </>
  ),
  earth: (
    <>
      <div className="scenery-ground scenery-grass" />
      <div className="scenery-mound scenery-mound-1" />
      <div className="scenery-mound scenery-mound-2" />
      <div className="scenery-stone scenery-stone-1" />
      <div className="scenery-stone scenery-stone-2" />
    </>
  ),
  air: (
    <>
      <div className="scenery-ground scenery-clouds" />
      <div className="scenery-cloud scenery-cloud-1" />
      <div className="scenery-cloud scenery-cloud-2" />
      <div className="scenery-cloud scenery-cloud-3" />
    </>
  ),
  nature: (
    <>
      <div className="scenery-ground scenery-grass" />
      <div className="scenery-plant scenery-plant-1" />
      <div className="scenery-plant scenery-plant-2" />
      <div className="scenery-flower scenery-flower-1" />
      <div className="scenery-flower scenery-flower-2" />
    </>
  ),
  electric: (
    <>
      <div className="scenery-ground scenery-crystal" />
      <div className="scenery-spark scenery-spark-1" />
      <div className="scenery-spark scenery-spark-2" />
      <div className="scenery-crystal-shard scenery-crystal-1" />
      <div className="scenery-crystal-shard scenery-crystal-2" />
    </>
  ),
  shadow: (
    <>
      <div className="scenery-ground scenery-void" />
      <div className="scenery-mist scenery-mist-1" />
      <div className="scenery-mist scenery-mist-2" />
      <div className="scenery-orb scenery-orb-1" />
      <div className="scenery-orb scenery-orb-2" />
    </>
  ),
  light: (
    <>
      <div className="scenery-ground scenery-golden" />
      <div className="scenery-ray scenery-ray-1" />
      <div className="scenery-ray scenery-ray-2" />
      <div className="scenery-sparkle scenery-sparkle-1" />
      <div className="scenery-sparkle scenery-sparkle-2" />
    </>
  ),
}

export function StageScenery({ element }) {
  const scenery = ELEMENT_SCENERY[element] || ELEMENT_SCENERY.fire
  return <div className={`stage-scenery stage-scenery-${element}`}>{scenery}</div>
}
