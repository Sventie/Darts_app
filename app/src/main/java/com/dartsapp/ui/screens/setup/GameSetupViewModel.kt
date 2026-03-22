package com.dartsapp.ui.screens.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dartsapp.data.db.entity.PlayerEntity
import com.dartsapp.data.repository.GameRepository
import com.dartsapp.domain.model.CloseCondition
import com.dartsapp.domain.model.GameConfig
import com.dartsapp.domain.model.PlayerStats
import com.dartsapp.di.ActiveGameStore
import com.dartsapp.domain.usecase.game.StartGameUseCase
import com.dartsapp.domain.usecase.player.GetPlayersUseCase
import com.dartsapp.domain.usecase.stats.GetPlayerStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class GameSetupViewModel @Inject constructor(
    getPlayersUseCase: GetPlayersUseCase,
    getPlayerStatsUseCase: GetPlayerStatsUseCase,
    private val startGameUseCase: StartGameUseCase,
    private val activeGameStore: ActiveGameStore,
    private val gameRepository: GameRepository
) : ViewModel() {

    val players: StateFlow<List<PlayerEntity>> = getPlayersUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Stats per playerId; null = player has no recorded games yet. */
    val playerStats: StateFlow<Map<Long, PlayerStats?>> = players
        .flatMapLatest { list ->
            if (list.isEmpty()) {
                flowOf(emptyMap())
            } else {
                combine(list.map { getPlayerStatsUseCase(it.id) }) { array ->
                    list.indices.associate { i -> list[i].id to array[i] }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _selectedPlayerIds = MutableStateFlow<List<Long>>(emptyList())
    val selectedPlayerIds: StateFlow<List<Long>> = _selectedPlayerIds.asStateFlow()

    private val _startingScore = MutableStateFlow(101)
    val startingScore: StateFlow<Int> = _startingScore.asStateFlow()

    private val _closeCondition = MutableStateFlow(CloseCondition.SINGLE_OUT)
    val closeCondition: StateFlow<CloseCondition> = _closeCondition.asStateFlow()

    private val _startedGameId = MutableStateFlow<Long?>(null)
    val startedGameId: StateFlow<Long?> = _startedGameId.asStateFlow()

    init {
        viewModelScope.launch {
            val lastIds = gameRepository.getLastGamePlayerIds()
            if (lastIds.isNotEmpty()) {
                _selectedPlayerIds.value = lastIds
            }
        }
    }

    fun togglePlayer(playerId: Long) {
        val current = _selectedPlayerIds.value.toMutableList()
        if (playerId in current) current.remove(playerId) else current.add(playerId)
        _selectedPlayerIds.value = current
    }

    fun setStartingScore(score: Int) {
        _startingScore.value = score
    }

    fun setCloseCondition(condition: CloseCondition) {
        _closeCondition.value = condition
    }

    fun startGame() {
        val selectedIds = _selectedPlayerIds.value
        if (selectedIds.isEmpty()) return
        viewModelScope.launch {
            val game = startGameUseCase(
                GameConfig(
                    playerIds = selectedIds,
                    startingScore = _startingScore.value,
                    closeCondition = _closeCondition.value
                )
            )
            activeGameStore.put(game)
            _startedGameId.value = game.gameId
        }
    }

    fun reorderPlayers(fromIndex: Int, toIndex: Int) {
        val current = _selectedPlayerIds.value.toMutableList()
        current.add(toIndex, current.removeAt(fromIndex))
        _selectedPlayerIds.value = current
    }

    fun randomizePlayerOrder() {
        _selectedPlayerIds.value = _selectedPlayerIds.value.shuffled()
    }

    fun clearStartedGame() {
        _startedGameId.value = null
    }
}
