package com.dartsapp.ui.screens.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dartsapp.data.repository.GameRepository
import com.dartsapp.di.ActiveGameStore
import com.dartsapp.domain.model.ActiveGame
import com.dartsapp.domain.model.DartInput
import com.dartsapp.domain.usecase.game.BustResult
import com.dartsapp.domain.usecase.game.CheckBustUseCase
import com.dartsapp.domain.usecase.game.ProcessRoundUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BustInfo(
    val playerName: String,
    val attemptedScore: Int,
    val remainingBefore: Int
)

sealed class GameUiState {
    object Loading : GameUiState()
    data class Playing(
        val activeGame: ActiveGame,
        val currentPlayerName: String,
        val currentPlayerScore: Int,
        val currentRoundDarts: List<DartInput>,
        val roundTotal: Int,
        val projectedScore: Int,
        val isBust: Boolean
    ) : GameUiState()
    data class GameOver(val gameId: Long, val winnerName: String) : GameUiState()
    object Abandoned : GameUiState()
    data class Error(val message: String) : GameUiState()
}

@HiltViewModel
class GameViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val checkBustUseCase: CheckBustUseCase,
    private val processRoundUseCase: ProcessRoundUseCase,
    private val gameRepository: GameRepository,
    private val activeGameStore: ActiveGameStore
) : ViewModel() {

    private val gameId: Long = checkNotNull(savedStateHandle["gameId"])

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _bustEvent = MutableSharedFlow<BustInfo>()
    val bustEvent: SharedFlow<BustInfo> = _bustEvent.asSharedFlow()

    init {
        val activeGame = activeGameStore.get(gameId)
        if (activeGame != null) {
            _uiState.value = buildPlayingState(activeGame, emptyList())
        } else {
            _uiState.value = GameUiState.Error("Game not found")
        }
    }

    fun onDartEntered(dartInput: DartInput) {
        val state = _uiState.value as? GameUiState.Playing ?: return
        val darts = state.currentRoundDarts + dartInput
        val bustResult = checkBustUseCase(
            currentScore = state.currentPlayerScore,
            roundDarts = darts,
            closeCondition = state.activeGame.config.closeCondition
        )

        when {
            bustResult is BustResult.Bust -> {
                viewModelScope.launch {
                    _bustEvent.emit(
                        BustInfo(
                            playerName = state.currentPlayerName,
                            attemptedScore = darts.sumOf { it.scoreValue },
                            remainingBefore = state.currentPlayerScore
                        )
                    )
                    confirmRoundInternal(darts, bustResult, state)
                }
            }
            bustResult is BustResult.Win || darts.size == 3 -> {
                viewModelScope.launch {
                    confirmRoundInternal(darts, bustResult, state)
                }
            }
            else -> {
                val roundTotal = darts.sumOf { it.scoreValue }
                _uiState.value = state.copy(
                    currentRoundDarts = darts,
                    roundTotal = roundTotal,
                    projectedScore = state.currentPlayerScore - roundTotal,
                    isBust = false
                )
            }
        }
    }

    fun abandonGame() {
        activeGameStore.remove(gameId)
        _uiState.value = GameUiState.Abandoned
    }

    fun onUndoLastDart() {
        val state = _uiState.value as? GameUiState.Playing ?: return
        if (state.currentRoundDarts.isEmpty()) return
        val darts = state.currentRoundDarts.dropLast(1)
        val roundTotal = darts.sumOf { it.scoreValue }
        _uiState.value = state.copy(
            currentRoundDarts = darts,
            roundTotal = roundTotal,
            projectedScore = state.currentPlayerScore - roundTotal,
            isBust = false
        )
    }

    private suspend fun confirmRoundInternal(
        darts: List<DartInput>,
        bustResult: BustResult,
        state: GameUiState.Playing
    ) {
        val activeGame = state.activeGame
        val currentPlayer = activeGame.currentPlayer

        val result = processRoundUseCase(
            participantId = currentPlayer.participantId,
            gameId = activeGame.gameId,
            roundNumber = activeGame.roundNumber,
            scoreBefore = currentPlayer.scoreBeforeRound,
            darts = darts,
            bustResult = bustResult
        )

        if (result.isWin) {
            gameRepository.finishGame(activeGame.gameId, currentPlayer.playerId)
            gameRepository.updatePlacement(currentPlayer.participantId, 1)
            _uiState.value = GameUiState.GameOver(
                gameId = activeGame.gameId,
                winnerName = currentPlayer.playerName
            )
            return
        }

        // Advance to next player
        val updatedPlayers = activeGame.players.mapIndexed { idx, player ->
            if (idx == activeGame.currentPlayerIndex) {
                player.copy(
                    remainingScore = result.scoreAfter,
                    currentRoundDarts = emptyList(),
                    scoreBeforeRound = result.scoreAfter
                )
            } else player
        }

        val nextIndex = (activeGame.currentPlayerIndex + 1) % activeGame.players.size
        val nextRound = if (nextIndex == 0) activeGame.roundNumber + 1 else activeGame.roundNumber

        val updatedGame = activeGame.copy(
            players = updatedPlayers,
            currentPlayerIndex = nextIndex,
            roundNumber = nextRound
        )

        _uiState.value = buildPlayingState(updatedGame, emptyList())
    }

    private fun buildPlayingState(game: ActiveGame, darts: List<DartInput>): GameUiState.Playing {
        val player = game.currentPlayer
        val roundTotal = darts.sumOf { it.scoreValue }
        return GameUiState.Playing(
            activeGame = game,
            currentPlayerName = player.playerName,
            currentPlayerScore = player.remainingScore,
            currentRoundDarts = darts,
            roundTotal = roundTotal,
            projectedScore = player.remainingScore - roundTotal,
            isBust = false
        )
    }
}
