package com.dartsapp.domain

import com.dartsapp.data.model.ScoreMultiplier
import com.dartsapp.domain.model.CloseCondition
import com.dartsapp.domain.model.DartInput
import com.dartsapp.domain.usecase.game.BustReason
import com.dartsapp.domain.usecase.game.BustResult
import com.dartsapp.domain.usecase.game.CheckBustUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CheckBustUseCaseTest {

    private lateinit var useCase: CheckBustUseCase

    @Before
    fun setUp() {
        useCase = CheckBustUseCase()
    }

    private fun dart(field: Int, multiplier: ScoreMultiplier) =
        DartInput(field, multiplier, field * multiplier.value)

    @Test
    fun `overshoot is bust in both modes`() {
        val darts = listOf(dart(20, ScoreMultiplier.TRIPLE)) // 60 > 40
        val result = useCase(40, darts, CloseCondition.DOUBLE_OUT)
        assertTrue(result is BustResult.Bust)
        assertEquals(BustReason.OVERSHOOT, (result as BustResult.Bust).reason)
    }

    @Test
    fun `landing on 1 in double out is bust`() {
        val darts = listOf(dart(19, ScoreMultiplier.SINGLE)) // 20 - 19 = 1
        val result = useCase(20, darts, CloseCondition.DOUBLE_OUT)
        assertTrue(result is BustResult.Bust)
        assertEquals(BustReason.UNREACHABLE_ONE, (result as BustResult.Bust).reason)
    }

    @Test
    fun `landing on 1 in single out is continue`() {
        val darts = listOf(dart(19, ScoreMultiplier.SINGLE))
        val result = useCase(20, darts, CloseCondition.SINGLE_OUT)
        assertTrue(result is BustResult.Continue)
        assertEquals(1, (result as BustResult.Continue).remaining)
    }

    @Test
    fun `double out with double wins`() {
        val darts = listOf(dart(20, ScoreMultiplier.DOUBLE)) // 40
        val result = useCase(40, darts, CloseCondition.DOUBLE_OUT)
        assertTrue(result is BustResult.Win)
        assertEquals(1, (result as BustResult.Win).dartsUsed)
    }

    @Test
    fun `double out with single is bust`() {
        val darts = listOf(dart(20, ScoreMultiplier.SINGLE)) // 20
        val result = useCase(20, darts, CloseCondition.DOUBLE_OUT)
        assertTrue(result is BustResult.Bust)
        assertEquals(BustReason.NOT_DOUBLE_OUT, (result as BustResult.Bust).reason)
    }

    @Test
    fun `single out with single wins`() {
        val darts = listOf(dart(20, ScoreMultiplier.SINGLE))
        val result = useCase(20, darts, CloseCondition.SINGLE_OUT)
        assertTrue(result is BustResult.Win)
    }

    @Test
    fun `continue with darts remaining`() {
        val darts = listOf(dart(5, ScoreMultiplier.SINGLE), dart(10, ScoreMultiplier.SINGLE))
        val result = useCase(100, darts, CloseCondition.DOUBLE_OUT)
        assertTrue(result is BustResult.Continue)
        assertEquals(85, (result as BustResult.Continue).remaining)
    }

    @Test
    fun `win on second dart double out`() {
        val darts = listOf(
            dart(10, ScoreMultiplier.SINGLE),  // 100 - 10 = 90
            dart(45, ScoreMultiplier.DOUBLE)   // 90 - 90 = 0 with double
        )
        val result = useCase(100, darts, CloseCondition.DOUBLE_OUT)
        assertTrue(result is BustResult.Win)
        assertEquals(2, (result as BustResult.Win).dartsUsed)
    }
}
