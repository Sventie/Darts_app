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
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StatsListViewModel @Inject constructor(
    getPlayersUseCase: GetPlayersUseCase
) : ViewModel() {
    val players: StateFlow<List<PlayerEntity>> = getPlayersUseCase()
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
