package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.AiMemoryEntity
import com.example.model.ExecutionLogEntity
import com.example.model.ScenarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScenarioDao {
    @Query("SELECT * FROM scenarios ORDER BY id DESC")
    fun getAllScenarios(): Flow<List<ScenarioEntity>>

    @Query("SELECT * FROM scenarios WHERE id = :id")
    suspend fun getScenarioById(id: Long): ScenarioEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenario(scenario: ScenarioEntity): Long

    @Update
    suspend fun updateScenario(scenario: ScenarioEntity)

    @Query("DELETE FROM scenarios WHERE id = :id")
    suspend fun deleteScenarioById(id: Long)

    @Query("UPDATE scenarios SET totalExecutions = totalExecutions + 1, successExecutions = successExecutions + :successInc, lastRunAt = :timestamp WHERE id = :id")
    suspend fun recordExecutionStats(id: Long, successInc: Int, timestamp: Long)
}

@Dao
interface ExecutionLogDao {
    @Query("SELECT * FROM execution_logs ORDER BY timestamp DESC LIMIT 300")
    fun getRecentLogs(): Flow<List<ExecutionLogEntity>>

    @Query("SELECT * FROM execution_logs WHERE scenarioId = :scenarioId ORDER BY timestamp DESC LIMIT 150")
    fun getLogsForScenario(scenarioId: Long): Flow<List<ExecutionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ExecutionLogEntity): Long

    @Query("DELETE FROM execution_logs")
    suspend fun clearLogs()
}

@Dao
interface AiMemoryDao {
    @Query("SELECT * FROM ai_memories ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<AiMemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: AiMemoryEntity): Long

    @Query("DELETE FROM ai_memories WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)

    @Query("DELETE FROM ai_memories")
    suspend fun clearAllMemories()
}
