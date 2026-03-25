package com.dartsapp.ui.screens.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dartsapp.data.repository.GameRepository
import com.dartsapp.di.ActiveGameStore
import com.dartsapp.domain.model.ActiveGame
import com.dartsapp.domain.model.ActivePlayer
import com.dartsapp.domain.model.DartInput
import com.dartsapp.domain.usecase.game.BustResult
import com.dartsapp.domain.usecase.game.CheckBustUseCase
import com.dartsapp.domain.usecase.game.CheckoutSuggestion
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

/**
 * Snapshot of the last committed round used to support cross-turn undo.
 */
data class LastCommittedRound(
    val roundId: Long,
    val participantId: Long,
    val playerIndex: Int,
    val scoreBeforeRound: Int,
    val darts: List<DartInput>,
    val roundNumber: Int,
    /** True when this round was a winning (checkout) round. */
    val wasWin: Boolean = false,
    /** The placement that was assigned on checkout (only relevant when wasWin = true). */
    val placement: Int? = null
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
        val isBust: Boolean,
        val checkoutSuggestion: List<String>?,
        val lastCommittedRound: LastCommittedRound?,
        /** Non-null while the "placement dialog" should be shown. */
        val playerJustFinished: ActivePlayer? = null,
        /** True when every player has a placement – only "end game" button shown. */
        val allPlayersFinished: Boolean = false
    ) : GameUiState()
    data class GameOver(val gameId: Long) : GameUiState()
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
            _uiState.value = buildPlayingState(activeGame, emptyList(), lastCommittedRound = null)
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
                val projected = state.currentPlayerScore - roundTotal
                _uiState.value = state.copy(
                    currentRoundDarts = darts,
                    roundTotal = roundTotal,
                    projectedScore = projected,
                    isBust = false,
                    checkoutSuggestion = CheckoutSuggestion.suggest(
                        remaining = projected,
                        dartsLeft = 3 - darts.size,
                        closeCondition = state.activeGame.config.closeCondition
                    )
                )
            }
        }
    }

    fun abandonGame() {
        activeGameStore.remove(gameId)
        _uiState.value = GameUiState.Abandoned
    }

    /** User chose to keep playing after a player finished – advance to the next active player. */
    fun continueAfterPlacement() {
        val state = _uiState.value as? GameUiState.Playing ?: return
        val game = state.activeGame
        val (nextIdx, newRound) = findNextActivePlayer(game.currentPlayerIndex, game.players)
        if (nextIdx == -1) return // should not happen; button is hidden when allPlayersFinished
        val updatedGame = game.copy(
            currentPlayerIndex = nextIdx,
            roundNumber = if (newRound) game.roundNumber + 1 else game.roundNumber
        )
        // Preserve lastCommittedRound so undo still works after the placement dialog is dismissed
        _uiState.value = buildPlayingState(updatedGame, emptyList(), state.lastCommittedRound)
    }

    /** User chose to end the game from the placement dialog. */
    fun endGame() {
        activeGameStore.remove(gameId)
        _uiState.value = GameUiState.GameOver(gameId = gameId)
    }

    /**
     * Undo the last dart.
     *
     * - If darts have been entered this turn: remove the last dart (in-round undo).
     * - Otherwise: undo the entire last committed round (cross-turn undo), reverting
     *   the previous player's score and removing the round from the database so
     *   statistics stay accurate.
     */
    fun onUndoLastDart() {
        val state = _uiState.value as? GameUiState.Playing ?: return

        if (state.currentRoundDarts.isNotEmpty()) {
            // In-round undo – no DB changes needed
            val darts = state.currentRoundDarts.dropLast(1)
            val roundTotal = darts.sumOf { it.scoreValue }
            val projected = state.currentPlayerScore - roundTotal
            _uiState.value = state.copy(
                currentRoundDarts = darts,
                roundTotal = roundTotal,
                projectedScore = projected,
                isBust = false,
                checkoutSuggestion = CheckoutSuggestion.suggest(
                    remaining = projected,
                    dartsLeft = 3 - darts.size,
                    closeCondition = state.activeGame.config.closeCondition
                )
            )
        } else if (state.lastCommittedRound != null) {
            viewModelScope.launch { undoLastCommittedRound(state) }
        }
    }

    private suspend fun undoLastCommittedRound(state: GameUiState.Playing) {
        val last = state.lastCommittedRound ?: return

        if (last.wasWin) {
            // Revert checkout: delete round, restore score, clear placement (and game-finish if #1)
            gameRepository.undoWinRound(
                roundId = last.roundId,
                participantId = last.participantId,
                scoreBefore = last.scoreBeforeRound,
                gameId = state.activeGame.gameId,
                wasFirstPlace = last.placement == 1
            )
        } else {
            // Revert DB: delete round (CASCADE removes dart_throws) and restore score
            gameRepository.undoRound(last.roundId, last.participantId, last.scoreBeforeRound)
        }

        // Restore the player's score (and placement if it was a checkout) in memory
        val activeGame = state.activeGame
        val restoredPlayers = activeGame.players.mapIndexed { idx, player ->
            if (idx == last.playerIndex) {
                player.copy(
                    remainingScore = last.scoreBeforeRound,
                    scoreBeforeRound = last.scoreBeforeRound,
                    currentRoundDarts = emptyList(),
                    placement = if (last.wasWin) null else player.placement
                )
            } else player
        }

        val restoredGame = activeGame.copy(
            players = restoredPlayers,
            currentPlayerIndex = last.playerIndex,
            roundNumber = last.roundNumber
        )

        // Rebuild state with the last dart already removed so the single undo press
        // immediately removes that dart rather than just switching back to the player.
        _uiState.value = buildPlayingState(
            game = restoredGame,
            darts = last.darts.dropLast(1),
            lastCommittedRound = null
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
            val placement = activeGame.players.count { it.placement != null } + 1
            // First-place finish finalises the game in the DB
            if (placement == 1) {
                gameRepository.finishGame(activeGame.gameId, currentPlayer.playerId)
            }
            gameRepository.updatePlacement(currentPlayer.participantId, placement)

            // Capture the winning round for cross-turn undo (wasWin = true)
            val committed = LastCommittedRound(
                roundId = result.roundId,
                participantId = currentPlayer.participantId,
                playerIndex = activeGame.currentPlayerIndex,
                scoreBeforeRound = currentPlayer.scoreBeforeRound,
                darts = darts,
                roundNumber = activeGame.roundNumber,
                wasWin = true,
                placement = placement
            )

            val finishedPlayer = currentPlayer.copy(
                remainingScore = 0,
                currentRoundDarts = emptyList(),
                scoreBeforeRound = 0,
                placement = placement
            )
            val updatedPlayers = activeGame.players.mapIndexed { idx, player ->
                if (idx == activeGame.currentPlayerIndex) finishedPlayer else player
            }
            val allDone = updatedPlayers.all { it.placement != null }
            val updatedGame = activeGame.copy(players = updatedPlayers)

            _uiState.value = buildPlayingState(updatedGame, emptyList(), committed).copy(
                playerJustFinished = finishedPlayer,
                allPlayersFinished = allDone
            )
            return
        }

        // Capture the completed round for cross-turn undo
        val committed = LastCommittedRound(
            roundId = result.roundId,
            participantId = currentPlayer.participantId,
            playerIndex = activeGame.currentPlayerIndex,
            scoreBeforeRound = currentPlayer.scoreBeforeRound,
            darts = darts,
            roundNumber = activeGame.roundNumber
        )

        // Update the current player's score and store last round darts for display
        val updatedPlayers = activeGame.players.mapIndexed { idx, player ->
            if (idx == activeGame.currentPlayerIndex) {
                player.copy(
                    remainingScore = result.scoreAfter,
                    currentRoundDarts = emptyList(),
                    scoreBeforeRound = result.scoreAfter,
                    lastRoundDarts = darts
                )
            } else player
        }

        // Advance to next active (non-finished) player
        val (nextIndex, newRound) = findNextActivePlayer(activeGame.currentPlayerIndex, updatedPlayers)
        val nextRound = if (newRound) activeGame.roundNumber + 1 else activeGame.roundNumber

        val updatedGame = activeGame.copy(
            players = updatedPlayers,
            currentPlayerIndex = nextIndex,
            roundNumber = nextRound
        )

        _uiState.value = buildPlayingState(updatedGame, emptyList(), lastCommittedRound = committed)
    }

    /**
     * Returns the index of the next player without a placement, and whether we crossed
     * index 0 (= a new round starts).
     */
    private fun findNextActivePlayer(currentIndex: Int, players: List<ActivePlayer>): Pair<Int, Boolean> {
        val size = players.size
        var idx = currentIndex
        var crossedOrigin = false
        for (i in 1..size) {
            idx = (idx + 1) % size
            if (idx == 0) crossedOrigin = true
            if (players[idx].placement == null) return Pair(idx, crossedOrigin)
        }
        return Pair(-1, false)
    }

    private fun buildPlayingState(
        game: ActiveGame,
        darts: List<DartInput>,
        lastCommittedRound: LastCommittedRound?
    ): GameUiState.Playing {
        // Clear lastRoundDarts for the player who is now throwing so the card stays clean
        val clearedPlayers = game.players.mapIndexed { idx, p ->
            if (idx == game.currentPlayerIndex) p.copy(lastRoundDarts = emptyList()) else p
        }
        val game = game.copy(players = clearedPlayers)
        val player = game.currentPlayer
        val roundTotal = darts.sumOf { it.scoreValue }
        val projected = player.remainingScore - roundTotal
        return GameUiState.Playing(
            activeGame = game,
            currentPlayerName = player.playerName,
            currentPlayerScore = player.remainingScore,
            currentRoundDarts = darts,
            roundTotal = roundTotal,
            projectedScore = projected,
            isBust = false,
            checkoutSuggestion = CheckoutSuggestion.suggest(
                remaining = projected,
                dartsLeft = 3 - darts.size,
                closeCondition = game.config.closeCondition
            ),
            lastCommittedRound = lastCommittedRound
        )
    }
}
