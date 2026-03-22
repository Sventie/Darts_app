package com.dartsapp.ui.screens.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dartsapp.data.db.entity.PlayerEntity
import com.dartsapp.domain.model.FieldHitFrequency
import com.dartsapp.domain.model.PlayerStats
import com.dartsapp.domain.usecase.player.GetPlayersUseCase
import com.dartsapp.domain.usecase.stats.GetFieldFrequencyUseCase
import com.dartsapp.domain.usecase.stats.GetPlayerStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
                    stats ?: PlayerStats(p.id, p.name, 0, 0, 0.0, 0.0, 0, 0)
                }
            }) { it.toList() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@HiltViewModel
class StatsDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getPlayerStatsUseCase: GetPlayerStatsUseCase,
    getFieldFrequencyUseCase: GetFieldFrequencyUseCase
) : ViewModel() {

    private val playerId: Long = checkNotNull(savedStateHandle["playerId"])

    val stats: StateFlow<PlayerStats?> = getPlayerStatsUseCase(playerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val fieldFrequencies: StateFlow<List<FieldHitFrequency>> = getFieldFrequencyUseCase(playerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
