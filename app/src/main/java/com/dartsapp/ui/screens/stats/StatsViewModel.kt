package com.dartsapp.ui.screens.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dartsapp.data.db.dao.TrainingDao
import com.dartsapp.data.db.entity.PlayerEntity
import com.dartsapp.domain.model.PlayerStats
import com.dartsapp.domain.usecase.player.GetPlayersUseCase
import com.dartsapp.domain.usecase.stats.GetHeatPositionsUseCase
import com.dartsapp.domain.usecase.stats.GetPlayerStatsUseCase
import com.dartsapp.domain.usecase.stats.GetTrainingDispersionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsOverviewViewModel @Inject constructor(
    getPlayersUseCase: GetPlayersUseCase,
    private val getPlayerStatsUseCase: GetPlayerStatsUseCase
) : ViewModel() {
    val allStats: StateFlow<List<PlayerStats>> = getPlayersUseCase()
        .flatMapLatest { players ->
            if (players.isEmpty()) flowOf(emptyList())
            else combine(players.map { p ->
                getPlayerStatsUseCase(p.id).map { stats ->
                    stats ?: PlayerStats(
                        playerId = p.id, playerName = p.name,
                        gamesPlayed = 0, wins = 0, secondPlace = 0, thirdPlace = 0,
                        avgScorePerDart = 0.0, avgScorePerRound = 0.0, first9Average = 0.0,
                        highestCheckout = 0, bustCount = 0, checkoutAttempts = 0,
                        highestRound = 0, roundsUnder10 = 0, totalRounds = 0,
                        totalDartsThrown = 0, doubleHits = 0, tripleHits = 0, outOfBounceCount = 0,
                        bestBuddyName = null, rivalName = null, easyWinName = null
                    )
                }
            }) { it.toList() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HeatmapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getPlayersUseCase: GetPlayersUseCase,
    private val getHeatPositionsUseCase: GetHeatPositionsUseCase,
    private val getTrainingDispersionUseCase: GetTrainingDispersionUseCase,
    private val trainingDao: TrainingDao
) : ViewModel() {

    private val initialPlayerId: Long = checkNotNull(savedStateHandle["playerId"])

    private val _selectedPlayerId = MutableStateFlow(initialPlayerId)
    val selectedPlayerId: StateFlow<Long> = _selectedPlayerId.asStateFlow()

    private val _fromGame = MutableStateFlow(1)
    val fromGame: StateFlow<Int> = _fromGame.asStateFlow()

    private val _toGame = MutableStateFlow(Int.MAX_VALUE)
    val toGame: StateFlow<Int> = _toGame.asStateFlow()

    private val _fromTraining = MutableStateFlow(1)
    val fromTraining: StateFlow<Int> = _fromTraining.asStateFlow()

    private val _toTraining = MutableStateFlow(Int.MAX_VALUE)
    val toTraining: StateFlow<Int> = _toTraining.asStateFlow()

    val allPlayers: StateFlow<List<PlayerEntity>> = getPlayersUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gameCount: StateFlow<Int> = _selectedPlayerId
        .flatMapLatest { getHeatPositionsUseCase.gameCount(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val trainingSessionCount: StateFlow<Int> = _selectedPlayerId
        .flatMapLatest { trainingDao.getCountForPlayer(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val playerName: StateFlow<String> = combine(allPlayers, _selectedPlayerId) { players, id ->
        players.firstOrNull { it.id == id }?.name ?: ""
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val hitPositions: StateFlow<List<GetHeatPositionsUseCase.HitPosition>> =
        combine(_selectedPlayerId, _fromGame, _toGame) { pid, from, to ->
            Triple(pid, from, to)
        }
        .flatMapLatest { (pid, from, to) -> getHeatPositionsUseCase(pid, from, to) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val effectiveTrainingTo: Flow<Int> =
        combine(_toTraining, trainingSessionCount) { to, count ->
            if (to == Int.MAX_VALUE) count else to.coerceAtMost(count)
        }

    /** RMS deviation from target, filtered by training session range. */
    val dispersion: StateFlow<Float> =
        combine(_selectedPlayerId, _fromTraining, effectiveTrainingTo) { pid, from, effectiveTo ->
            Triple(pid, from, effectiveTo)
        }
        .flatMapLatest { (pid, from, to) ->
            if (to == 0) flowOf(0f)
            else getTrainingDispersionUseCase(pid, from, to)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    /** Number of training throws used to compute the dispersion. */
    val trainingThrowCount: StateFlow<Int> =
        combine(_selectedPlayerId, _fromTraining, effectiveTrainingTo) { pid, from, effectiveTo ->
            Triple(pid, from, effectiveTo)
        }
        .flatMapLatest { (pid, from, to) ->
            if (to == 0) flowOf(0)
            else getTrainingDispersionUseCase.throwCount(pid, from, to)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun selectPlayer(playerId: Long) {
        _selectedPlayerId.value = playerId
        _fromGame.value = 1
        _toGame.value = Int.MAX_VALUE
        _fromTraining.value = 1
        _toTraining.value = Int.MAX_VALUE
    }

    fun setGameRange(from: Int, to: Int) {
        _fromGame.update { from }
        _toGame.update { to }
    }

    fun setTrainingRange(from: Int, to: Int) {
        _fromTraining.update { from }
        _toTraining.update { to }
    }
}
