package com.dartsapp.ui.screens.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dartsapp.data.db.entity.PlayerEntity
import com.dartsapp.data.repository.PlayerRepository
import com.dartsapp.domain.usecase.player.CreatePlayerResult
import com.dartsapp.domain.usecase.player.CreatePlayerUseCase
import com.dartsapp.domain.usecase.player.DeletePlayerUseCase
import com.dartsapp.domain.usecase.player.GetPlayersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PlayerManagementEvent {
    object PlayerCreated : PlayerManagementEvent()
    object PlayerRenamed : PlayerManagementEvent()
    object NameEmpty : PlayerManagementEvent()
    object NameTaken : PlayerManagementEvent()
}

@HiltViewModel
class PlayerManagementViewModel @Inject constructor(
    getPlayersUseCase: GetPlayersUseCase,
    private val createPlayerUseCase: CreatePlayerUseCase,
    private val deletePlayerUseCase: DeletePlayerUseCase,
    private val playerRepository: PlayerRepository
) : ViewModel() {

    val players: StateFlow<List<PlayerEntity>> = getPlayersUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _event = MutableStateFlow<PlayerManagementEvent?>(null)
    val event: StateFlow<PlayerManagementEvent?> = _event.asStateFlow()

    fun createPlayer(name: String) {
        viewModelScope.launch {
            val result = createPlayerUseCase(name)
            _event.value = when (result) {
                is CreatePlayerResult.Success -> PlayerManagementEvent.PlayerCreated
                is CreatePlayerResult.NameEmpty -> PlayerManagementEvent.NameEmpty
                is CreatePlayerResult.NameTaken -> PlayerManagementEvent.NameTaken
            }
        }
    }

    fun renamePlayer(player: PlayerEntity, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            _event.value = PlayerManagementEvent.NameEmpty
            return
        }
        viewModelScope.launch {
            val existing = playerRepository.getPlayerByName(trimmed)
            if (existing != null && existing.id != player.id) {
                _event.value = PlayerManagementEvent.NameTaken
                return@launch
            }
            playerRepository.updatePlayer(player.copy(name = trimmed))
            _event.value = PlayerManagementEvent.PlayerRenamed
        }
    }

    fun deletePlayer(player: PlayerEntity) {
        viewModelScope.launch {
            deletePlayerUseCase(player)
        }
    }

    fun clearEvent() {
        _event.value = null
    }
}
