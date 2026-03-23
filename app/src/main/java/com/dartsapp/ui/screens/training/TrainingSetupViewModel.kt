package com.dartsapp.ui.screens.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dartsapp.data.db.dao.TrainingDao
import com.dartsapp.data.db.entity.TrainingSessionEntity
import com.dartsapp.data.db.entity.PlayerEntity
import com.dartsapp.domain.model.TrainingDifficulty
import com.dartsapp.domain.model.TrainingMode
import com.dartsapp.domain.usecase.player.GetPlayersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TrainingSetupViewModel @Inject constructor(
    getPlayersUseCase: GetPlayersUseCase,
    private val trainingDao: TrainingDao
) : ViewModel() {

    val players: StateFlow<List<PlayerEntity>> = getPlayersUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPlayerId = MutableStateFlow<Long?>(null)
    val selectedPlayerId: StateFlow<Long?> = _selectedPlayerId.asStateFlow()

    private val _selectedMode = MutableStateFlow<TrainingMode?>(null)
    val selectedMode: StateFlow<TrainingMode?> = _selectedMode.asStateFlow()

    private val _selectedDifficulty = MutableStateFlow(TrainingDifficulty.BEGINNER)
    val selectedDifficulty: StateFlow<TrainingDifficulty> = _selectedDifficulty.asStateFlow()

    val recentSessions: StateFlow<List<TrainingSessionEntity>> =
        _selectedPlayerId.flatMapLatest { playerId ->
            if (playerId == null) flowOf(emptyList())
            else trainingDao.getRecentByPlayer(playerId, limit = 5)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectPlayer(playerId: Long) {
        _selectedPlayerId.value = if (_selectedPlayerId.value == playerId) null else playerId
    }

    fun selectMode(mode: TrainingMode) {
        _selectedMode.value = if (_selectedMode.value == mode) null else mode
    }

    fun selectDifficulty(difficulty: TrainingDifficulty) {
        _selectedDifficulty.value = difficulty
    }

    val canStart: Boolean
        get() = _selectedPlayerId.value != null && _selectedMode.value != null
}
