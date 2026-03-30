package com.collide.app.domain.model

data class FixtureResult(
    val fixtureName: String,
    val fixtureSize: Long,
    val baselineSize: Long,
    val winnersFound: Int,
    val bestWinnerSize: Long?,
    val bestSavings: Long,
    val candidatesEvaluated: Int,
    val exactnessFailures: Int,
    val notApplicableCount: Int,
    val noGainCount: Int,
    val elapsedMs: Long
) {
    val bestSavingsPct: Double
        get() = if (baselineSize > 0 && bestSavings > 0) (bestSavings.toDouble() / baselineSize) * 100.0 else 0.0
    val hadWin: Boolean get() = winnersFound > 0
}

data class CorpusSummary(
    val timestamp: Long,
    val runMode: String,
    val baselineStrategy: String,
    val fixtureResults: List<FixtureResult>,
    val totalElapsedMs: Long
) {
    val totalFixtures: Int get() = fixtureResults.size
    val fixturesWithWins: Int get() = fixtureResults.count { it.hadWin }
    val fixturesWithoutWins: Int get() = fixtureResults.count { !it.hadWin }
    val totalWinners: Int get() = fixtureResults.sumOf { it.winnersFound }
    val totalCandidatesEvaluated: Int get() = fixtureResults.sumOf { it.candidatesEvaluated }
    val averageSavingsPct: Double
        get() {
            val wins = fixtureResults.filter { it.hadWin }
            return if (wins.isEmpty()) 0.0 else wins.map { it.bestSavingsPct }.average()
        }
    val bestCase: FixtureResult? get() = fixtureResults.filter { it.hadWin }.maxByOrNull { it.bestSavings }
    val worstCase: FixtureResult? get() = fixtureResults.minByOrNull { it.winnersFound }
}
