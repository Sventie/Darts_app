package com.dartsapp.domain.usecase.stats

import com.dartsapp.data.db.dao.DartThrowDao
import com.dartsapp.domain.model.FieldHitFrequency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetFieldFrequencyUseCase @Inject constructor(
    private val dartThrowDao: DartThrowDao
) {
    operator fun invoke(playerId: Long): Flow<List<FieldHitFrequency>> {
        return dartThrowDao.getFieldFrequencyForPlayer(playerId).map { rows ->
            val grouped = rows.groupBy { it.field }
            grouped.map { (field, fieldRows) ->
                FieldHitFrequency(
                    field = field,
                    singleCount = fieldRows.firstOrNull { it.multiplier == "SINGLE" }?.count ?: 0,
                    doubleCount = fieldRows.firstOrNull { it.multiplier == "DOUBLE" }?.count ?: 0,
                    tripleCount = fieldRows.firstOrNull { it.multiplier == "TRIPLE" }?.count ?: 0
                )
            }.sortedBy { it.field }
        }
    }
}
