package com.dartsapp.ui.screens.training

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dartsapp.data.db.dao.TrainingDao
import com.dartsapp.data.db.entity.PlayerEntity
import com.dartsapp.data.db.entity.TrainingSessionEntity
import com.dartsapp.domain.model.TrainingDifficulty
import com.dartsapp.domain.model.TrainingMode
import com.dartsapp.domain.model.generateTargetFields
import com.dartsapp.domain.model.requiresDouble
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
    /** Total darts (Zielfeld/AtC) or average score (Scoring Rounds) */
    val primaryResult: Int,
    /** Completed fields / rounds */
    val fieldsCompleted: Int
)

sealed class ModeState {
    data class Zielfeld(
        val targetFields: List<String>,
        val currentFieldIndex: Int,
        val throwsForCurrentField: List<String>,
        val completedFields: List<Pair<String, Int>> // field to dart count
    ) : ModeState() {
        val currentField: String get() = targetFields[currentFieldIndex]
        val totalDartsSoFar: Int get() = completedFields.sumOf { it.second } + throwsForCurrentField.size
    }

    data class AroundTheClock(
        val currentNumber: Int,
        val dartsOnCurrentNumber: Int,
        val totalDarts: Int,
        val completedNumbers: List<Int>,
        val difficulty: TrainingDifficulty
    ) : ModeState() {
        val requiresDoubleForCurrent: Boolean
            get() = requiresDouble(currentNumber, difficulty)
    }

    data class ScoringRounds(
        val currentRound: Int,
        val roundScores: List<Int>,
        val pendingInput: String,
        val targetAverage: Int
    ) : ModeState() {
        val totalRounds: Int get() = 10
        val runningAverage: Double
            get() = if (roundScores.isEmpty()) 0.0 else roundScores.average()
        val isFinished: Boolean get() = roundScores.size >= totalRounds
    }
}

@HiltViewModel
class TrainingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getPlayersUseCase: GetPlayersUseCase,
    private val trainingDao: TrainingDao
) : ViewModel() {

    private val modeArg: String = savedStateHandle["mode"] ?: TrainingMode.ZIELFELD.name
    private val difficultyArg: String = savedStateHandle["difficulty"] ?: TrainingDifficulty.BEGINNER.name
    private val playerIdArg: Long = savedStateHandle["playerId"] ?: 0L

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
                    pendingInput = "",
                    targetAverage = difficulty.targetAverage()
                )
            }
        )
    }

    /** Zielfeld: record a thrown field (e.g. "T20", "S5", "Bull") */
    fun recordZielfeldThrow(thrownField: String) {
        val state = (_uiState.value as? TrainingUiState.Running)?.modeState as? ModeState.Zielfeld
            ?: return
        val newThrows = state.throwsForCurrentField + thrownField
        if (thrownField == state.currentField) {
            // Hit! Move to next field
            val newCompleted = state.completedFields + Pair(state.currentField, newThrows.size)
            val nextIndex = state.currentFieldIndex + 1
            if (nextIndex >= state.targetFields.size) {
                // All fields completed
                finishZielfeld(newCompleted)
            } else {
                _uiState.value = TrainingUiState.Running(
                    state.copy(
                        currentFieldIndex = nextIndex,
                        throwsForCurrentField = emptyList(),
                        completedFields = newCompleted
                    )
                )
            }
        } else {
            _uiState.value = TrainingUiState.Running(
                state.copy(throwsForCurrentField = newThrows)
            )
        }
    }

    private fun finishZielfeld(completedFields: List<Pair<String, Int>>) {
        val totalDarts = completedFields.sumOf { it.second }
        saveAndFinish(totalDarts, completedFields.size)
    }

    /** Around the Clock: record whether the dart was a hit */
    fun recordAtcThrow(isHit: Boolean) {
        val state = (_uiState.value as? TrainingUiState.Running)?.modeState as? ModeState.AroundTheClock
            ?: return
        val newTotal = state.totalDarts + 1
        val newDartsOnCurrent = state.dartsOnCurrentNumber + 1
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
                        completedNumbers = newCompleted
                    )
                )
            }
        } else {
            _uiState.value = TrainingUiState.Running(
                state.copy(
                    dartsOnCurrentNumber = newDartsOnCurrent,
                    totalDarts = newTotal
                )
            )
        }
    }

    /** Scoring Rounds: update the pending input digit */
    fun scoringAppendDigit(digit: String) {
        val state = (_uiState.value as? TrainingUiState.Running)?.modeState as? ModeState.ScoringRounds
            ?: return
        if (state.isFinished) return
        val newInput = (state.pendingInput + digit).trimStart('0').ifEmpty { "0" }
        val value = newInput.toIntOrNull() ?: return
        if (value > 180) return
        _uiState.value = TrainingUiState.Running(state.copy(pendingInput = newInput))
    }

    fun scoringDeleteDigit() {
        val state = (_uiState.value as? TrainingUiState.Running)?.modeState as? ModeState.ScoringRounds
            ?: return
        val newInput = state.pendingInput.dropLast(1)
        _uiState.value = TrainingUiState.Running(state.copy(pendingInput = newInput))
    }

    fun scoringConfirmRound() {
        val state = (_uiState.value as? TrainingUiState.Running)?.modeState as? ModeState.ScoringRounds
            ?: return
        if (state.isFinished) return
        val score = state.pendingInput.toIntOrNull() ?: 0
        val newScores = state.roundScores + score
        if (newScores.size >= state.totalRounds) {
            val avg = (newScores.average() * 10).toInt()
            saveAndFinish(avg, newScores.size)
        } else {
            _uiState.value = TrainingUiState.Running(
                state.copy(
                    currentRound = state.currentRound + 1,
                    roundScores = newScores,
                    pendingInput = ""
                )
            )
        }
    }

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
