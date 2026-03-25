package com.dartsapp.ui.screens.training

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dartsapp.data.db.dao.TrainingDao
import com.dartsapp.data.db.dao.TrainingThrowDao
import com.dartsapp.data.db.entity.PlayerEntity
import com.dartsapp.data.db.entity.TrainingSessionEntity
import com.dartsapp.data.db.entity.TrainingThrowEntity
import com.dartsapp.data.model.ScoreMultiplier
import com.dartsapp.domain.model.DartInput
import com.dartsapp.domain.model.TrainingDifficulty
import com.dartsapp.domain.model.TrainingMode
import com.dartsapp.domain.model.generateTargetFields
import com.dartsapp.domain.model.requiresDouble
import com.dartsapp.domain.model.targetCenterForAtcNumber
import com.dartsapp.domain.model.targetCenterForZielfeldField
import com.dartsapp.domain.usecase.player.GetPlayersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TrainingUiState {
    object Loading : TrainingUiState()
    data class Running(val modeState: ModeState) : TrainingUiState()
    data class Finished(val result: TrainingResult) : TrainingUiState()
}

data class TrainingResult(
    val mode: TrainingMode,
    val difficulty: TrainingDifficulty,
    val playerName: String,
    /** Total darts (Zielfeld/AtC) or average score ×10 (Scoring Rounds) */
    val primaryResult: Int,
    /** Completed fields / rounds */
    val fieldsCompleted: Int
)

sealed class ModeState {
    data class Zielfeld(
        val targetFields: List<String>,
        val currentFieldIndex: Int,
        val throwsForCurrentField: List<String>,
        val completedFields: List<Pair<String, Int>>, // field to dart count
        val throwDartsForCurrentField: List<DartInput> = emptyList(),
        /** Darts thrown for the last completed field (incl. the hit dart) – used for undo. */
        val lastCompletedFieldDarts: List<DartInput> = emptyList()
    ) : ModeState() {
        val currentField: String get() = targetFields[currentFieldIndex]
        val totalDartsSoFar: Int get() = completedFields.sumOf { it.second } + throwsForCurrentField.size
    }

    data class AroundTheClock(
        val currentNumber: Int,
        val dartsOnCurrentNumber: Int,
        val totalDarts: Int,
        val completedNumbers: List<Int>,
        val difficulty: TrainingDifficulty,
        val lastDart: DartInput? = null,
        /** dartsOnCurrentNumber value before the last hit – used to restore on undo. */
        val prevDartsOnNumber: Int = 0
    ) : ModeState() {
        val requiresDoubleForCurrent: Boolean
            get() = requiresDouble(currentNumber, difficulty)
    }

    data class ScoringRounds(
        val currentRound: Int,
        val roundScores: List<Int>,
        val pendingDarts: List<DartInput>,
        val targetAverage: Int,
        /** Darts of the last committed round – used to undo after auto-commit. */
        val lastRoundDarts: List<DartInput> = emptyList()
    ) : ModeState() {
        val totalRounds: Int get() = 10
        val runningAverage: Double
            get() = if (roundScores.isEmpty()) 0.0 else roundScores.average()
        val isFinished: Boolean get() = roundScores.size >= totalRounds
        val pendingScore: Int get() = pendingDarts.sumOf { it.scoreValue }
    }
}

@HiltViewModel
class TrainingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getPlayersUseCase: GetPlayersUseCase,
    private val trainingDao: TrainingDao,
    private val trainingThrowDao: TrainingThrowDao
) : ViewModel() {

    private val modeArg: String = savedStateHandle["mode"] ?: TrainingMode.ZIELFELD.name
    private val difficultyArg: String = savedStateHandle["difficulty"] ?: TrainingDifficulty.BEGINNER.name
    val playerIdArg: Long = savedStateHandle["playerId"] ?: 0L

    val mode: TrainingMode = runCatching { TrainingMode.valueOf(modeArg) }.getOrDefault(TrainingMode.ZIELFELD)
    val difficulty: TrainingDifficulty = runCatching { TrainingDifficulty.valueOf(difficultyArg) }.getOrDefault(TrainingDifficulty.BEGINNER)

    private val allPlayers: StateFlow<List<PlayerEntity>> = getPlayersUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow<TrainingUiState>(TrainingUiState.Loading)
    val uiState: StateFlow<TrainingUiState> = _uiState.asStateFlow()

    init {
        startSession()
    }

    private fun startSession() {
        _uiState.value = TrainingUiState.Running(
            when (mode) {
                TrainingMode.ZIELFELD -> ModeState.Zielfeld(
                    targetFields = generateTargetFields(difficulty),
                    currentFieldIndex = 0,
                    throwsForCurrentField = emptyList(),
                    completedFields = emptyList()
                )
                TrainingMode.AROUND_THE_CLOCK -> ModeState.AroundTheClock(
                    currentNumber = 1,
                    dartsOnCurrentNumber = 0,
                    totalDarts = 0,
                    completedNumbers = emptyList(),
                    difficulty = difficulty
                )
                TrainingMode.SCORING_ROUNDS -> ModeState.ScoringRounds(
                    currentRound = 1,
                    roundScores = emptyList(),
                    pendingDarts = emptyList(),
                    targetAverage = difficulty.targetAverage()
                )
            }
        )
    }

    // ── Zielfeld ──────────────────────────────────────────────────────────────

    fun recordZielfeldDart(dart: DartInput) {
        val thrownField = dart.toZielfeldField()
        val state = (_uiState.value as? TrainingUiState.Running)?.modeState as? ModeState.Zielfeld
        if (state != null && dart.tapX != null && dart.tapY != null) {
            val (tx, ty) = targetCenterForZielfeldField(state.currentField)
            val isHit = thrownField == state.currentField
            viewModelScope.launch {
                trainingThrowDao.insert(
                    TrainingThrowEntity(
                        playerId  = playerIdArg,
                        targetNx  = tx,
                        targetNy  = ty,
                        actualNx  = if (isHit) tx else dart.tapX,
                        actualNy  = if (isHit) ty else dart.tapY,
                        thrownAt  = System.currentTimeMillis()
                    )
                )
            }
        }
        recordZielfeldThrow(dart, thrownField)
    }

    fun undoZielfeldThrow() {
        val state = (_uiState.value as? TrainingUiState.Running)?.modeState as? ModeState.Zielfeld
            ?: return
        if (state.throwsForCurrentField.isNotEmpty()) {
            // Regular in-field undo
            _uiState.value = TrainingUiState.Running(
                state.copy(
                    throwsForCurrentField = state.throwsForCurrentField.dropLast(1),
                    throwDartsForCurrentField = state.throwDartsForCurrentField.dropLast(1)
                )
            )
        } else if (state.lastCompletedFieldDarts.isNotEmpty()) {
            // Undo the hit that completed the previous field
            val prev = state.lastCompletedFieldDarts
            _uiState.value = TrainingUiState.Running(
                state.copy(
                    currentFieldIndex = state.currentFieldIndex - 1,
                    throwsForCurrentField = prev.dropLast(1).map { it.toZielfeldField() },
                    throwDartsForCurrentField = prev.dropLast(1),
                    completedFields = state.completedFields.dropLast(1),
                    lastCompletedFieldDarts = emptyList()
                )
            )
        }
    }

    private fun recordZielfeldThrow(dart: DartInput, thrownField: String) {
        val state = (_uiState.value as? TrainingUiState.Running)?.modeState as? ModeState.Zielfeld
            ?: return
        val newThrows = state.throwsForCurrentField + thrownField
        val newDarts = state.throwDartsForCurrentField + dart
        if (thrownField == state.currentField) {
            val newCompleted = state.completedFields + Pair(state.currentField, newThrows.size)
            val nextIndex = state.currentFieldIndex + 1
            if (nextIndex >= state.targetFields.size) {
                finishZielfeld(newCompleted)
            } else {
                _uiState.value = TrainingUiState.Running(
                    state.copy(
                        currentFieldIndex = nextIndex,
                        throwsForCurrentField = emptyList(),
                        throwDartsForCurrentField = emptyList(),
                        completedFields = newCompleted,
                        lastCompletedFieldDarts = newDarts
                    )
                )
            }
        } else {
            _uiState.value = TrainingUiState.Running(
                state.copy(
                    throwsForCurrentField = newThrows,
                    throwDartsForCurrentField = newDarts
                )
            )
        }
    }

    private fun finishZielfeld(completedFields: List<Pair<String, Int>>) {
        saveAndFinish(completedFields.sumOf { it.second }, completedFields.size)
    }

    // ── Around the Clock ──────────────────────────────────────────────────────

    fun recordAtcDart(dart: DartInput) {
        val state = (_uiState.value as? TrainingUiState.Running)?.modeState as? ModeState.AroundTheClock
            ?: return
        val isHit = dart.field == state.currentNumber &&
            (!state.requiresDoubleForCurrent || dart.multiplier == ScoreMultiplier.DOUBLE)
        if (dart.tapX != null && dart.tapY != null) {
            val (tx, ty) = targetCenterForAtcNumber(state.currentNumber, state.requiresDoubleForCurrent)
            viewModelScope.launch {
                trainingThrowDao.insert(
                    TrainingThrowEntity(
                        playerId  = playerIdArg,
                        targetNx  = tx,
                        targetNy  = ty,
                        actualNx  = if (isHit) tx else dart.tapX,
                        actualNy  = if (isHit) ty else dart.tapY,
                        thrownAt  = System.currentTimeMillis()
                    )
                )
            }
        }
        recordAtcThrow(dart, isHit)
    }

    fun undoAtcDart() {
        val state = (_uiState.value as? TrainingUiState.Running)?.modeState as? ModeState.AroundTheClock
            ?: return
        if (state.totalDarts == 0) return
        if (state.dartsOnCurrentNumber == 0 && state.completedNumbers.isNotEmpty()) {
            // Last dart was a hit → restore previous number
            _uiState.value = TrainingUiState.Running(
                state.copy(
                    currentNumber = state.completedNumbers.last(),
                    dartsOnCurrentNumber = state.prevDartsOnNumber,
                    totalDarts = state.totalDarts - 1,
                    completedNumbers = state.completedNumbers.dropLast(1),
                    lastDart = null,
                    prevDartsOnNumber = 0
                )
            )
        } else {
            // Last dart was a miss
            _uiState.value = TrainingUiState.Running(
                state.copy(
                    dartsOnCurrentNumber = state.dartsOnCurrentNumber - 1,
                    totalDarts = state.totalDarts - 1,
                    lastDart = null
                )
            )
        }
    }

    private fun recordAtcThrow(dart: DartInput, isHit: Boolean) {
        val state = (_uiState.value as? TrainingUiState.Running)?.modeState as? ModeState.AroundTheClock
            ?: return
        val newTotal = state.totalDarts + 1
        if (isHit) {
            val newCompleted = state.completedNumbers + state.currentNumber
            val nextNumber = state.currentNumber + 1
            if (nextNumber > 20) {
                saveAndFinish(newTotal, newCompleted.size)
            } else {
                _uiState.value = TrainingUiState.Running(
                    state.copy(
                        currentNumber = nextNumber,
                        dartsOnCurrentNumber = 0,
                        totalDarts = newTotal,
                        completedNumbers = newCompleted,
                        lastDart = dart,
                        prevDartsOnNumber = state.dartsOnCurrentNumber
                    )
                )
            }
        } else {
            _uiState.value = TrainingUiState.Running(
                state.copy(
                    dartsOnCurrentNumber = state.dartsOnCurrentNumber + 1,
                    totalDarts = newTotal,
                    lastDart = dart
                )
            )
        }
    }

    // ── Scoring Rounds ────────────────────────────────────────────────────────

    fun recordScoringDart(dart: DartInput) {
        val state = (_uiState.value as? TrainingUiState.Running)?.modeState as? ModeState.ScoringRounds
            ?: return
        if (state.isFinished) return
        val newPendingDarts = state.pendingDarts + dart
        if (newPendingDarts.size >= 3) {
            val score = newPendingDarts.sumOf { it.scoreValue }
            val newScores = state.roundScores + score
            if (newScores.size >= state.totalRounds) {
                saveAndFinish((newScores.average() * 10).toInt(), newScores.size)
            } else {
                _uiState.value = TrainingUiState.Running(
                    state.copy(
                        currentRound = state.currentRound + 1,
                        roundScores = newScores,
                        pendingDarts = emptyList(),
                        lastRoundDarts = newPendingDarts
                    )
                )
            }
        } else {
            _uiState.value = TrainingUiState.Running(state.copy(pendingDarts = newPendingDarts))
        }
    }

    fun undoScoringDart() {
        val state = (_uiState.value as? TrainingUiState.Running)?.modeState as? ModeState.ScoringRounds
            ?: return
        if (state.pendingDarts.isNotEmpty()) {
            _uiState.value = TrainingUiState.Running(
                state.copy(pendingDarts = state.pendingDarts.dropLast(1))
            )
        } else if (state.roundScores.isNotEmpty() && state.lastRoundDarts.isNotEmpty()) {
            // Undo the last auto-committed round (3rd dart triggered commit)
            _uiState.value = TrainingUiState.Running(
                state.copy(
                    currentRound = state.currentRound - 1,
                    roundScores = state.roundScores.dropLast(1),
                    pendingDarts = state.lastRoundDarts.dropLast(1),
                    lastRoundDarts = emptyList()
                )
            )
        }
    }

    // ── Common ────────────────────────────────────────────────────────────────

    private fun saveAndFinish(result: Int, fieldsCompleted: Int) {
        viewModelScope.launch {
            val playerName = allPlayers.first().find { it.id == playerIdArg }?.name ?: ""
            trainingDao.insert(
                TrainingSessionEntity(
                    playerId = playerIdArg,
                    mode = mode.name,
                    difficulty = difficulty.name,
                    completedAt = System.currentTimeMillis(),
                    result = result,
                    fieldsCompleted = fieldsCompleted
                )
            )
            _uiState.value = TrainingUiState.Finished(
                TrainingResult(
                    mode = mode,
                    difficulty = difficulty,
                    playerName = playerName,
                    primaryResult = result,
                    fieldsCompleted = fieldsCompleted
                )
            )
        }
    }

    fun restart() {
        startSession()
    }
}

private fun DartInput.toZielfeldField(): String = when {
    field == 0 -> "Miss"
    field == 50 -> "Bullseye"
    field == 25 -> "Bull"
    multiplier == ScoreMultiplier.SINGLE -> "S$field"
    multiplier == ScoreMultiplier.DOUBLE -> "D$field"
    multiplier == ScoreMultiplier.TRIPLE -> "T$field"
    else -> "Miss"
}
