package com.dartsapp.di

import com.dartsapp.domain.model.ActiveGame
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveGameStore @Inject constructor() {
    private val games = mutableMapOf<Long, ActiveGame>()

    fun put(game: ActiveGame) { games[game.gameId] = game }
    fun get(gameId: Long): ActiveGame? = games[gameId]
    fun remove(gameId: Long) { games.remove(gameId) }
}
