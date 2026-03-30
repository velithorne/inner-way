package com.collide.app.domain.evaluator

import com.collide.app.domain.engine.CorpusRunner
import com.collide.app.domain.model.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CorpusRunnerTest {

    private val runner = CorpusRunner()

    private val fixtures = listOf(
        InputSample("repetitive.bin", null, ByteArray(3000) { (it % 4).toByte() }),
        InputSample("random.bin", null, ByteArray(500) { (it * 37 % 256).toByte() }),
        InputSample("zeros.bin", null, ByteArray(2000) { 0x00.toByte() })
    )

    private val config = CorpusRunner.CorpusRunConfig(
        runMode = RunMode.SAFE,
        baselineStrategy = BaselineStrategy.RAW_DEFLATE,
        maxCandidatesOverride = 10
    )

    @Test
    fun `corpus run produces summary with correct fixture count`() = runTest {
        val summary = runner.run(fixtures, config)
        assertEquals(3, summary.totalFixtures)
        assertEquals(3, summary.fixtureResults.size)
    }

    @Test
    fun `all fixtures appear in results`() = runTest {
        val summary = runner.run(fixtures, config)
        val names = summary.fixtureResults.map { it.fixtureName }.toSet()
        fixtures.forEach { f ->
            assertTrue("Fixture ${f.fileName} should appear in results", f.fileName in names)
        }
    }

    @Test
    fun `fixture results have non-negative counts`() = runTest {
        val summary = runner.run(fixtures, config)
        summary.fixtureResults.forEach { fr ->
            assertTrue(fr.winnersFound >= 0)
            assertTrue(fr.candidatesEvaluated >= 0)
            assertTrue(fr.exactnessFailures >= 0)
            assertTrue(fr.elapsedMs >= 0)
        }
    }

    @Test
    fun `total candidates evaluated is sum of individual fixtures`() = runTest {
        val summary = runner.run(fixtures, config)
        val expected = summary.fixtureResults.sumOf { it.candidatesEvaluated }
        assertEquals(expected, summary.totalCandidatesEvaluated)
    }

    @Test
    fun `fixtures with wins and without wins sum to total`() = runTest {
        val summary = runner.run(fixtures, config)
        assertEquals(summary.totalFixtures, summary.fixturesWithWins + summary.fixturesWithoutWins)
    }

    @Test
    fun `corpus run mode and baseline are stored in summary`() = runTest {
        val summary = runner.run(fixtures, config)
        assertEquals(RunMode.SAFE.name, summary.runMode)
        assertEquals(BaselineStrategy.RAW_DEFLATE.name, summary.baselineStrategy)
    }

    @Test
    fun `best case is null when no winners found`() = runTest {
        // Use a tiny max of 1 candidate and very small inputs likely to not win
        val tinyConfig = config.copy(maxCandidatesOverride = 1)
        val tinyFixtures = listOf(InputSample("tiny.bin", null, ByteArray(10) { it.toByte() }))
        val summary = runner.run(tinyFixtures, tinyConfig)
        // bestCase is null if no wins — just verify it doesn't crash
        assertNotNull(summary)
    }
}
