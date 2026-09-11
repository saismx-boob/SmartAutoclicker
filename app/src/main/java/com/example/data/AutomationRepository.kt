package com.example.data

import com.example.model.AiMemoryEntity
import com.example.model.ExecutionLogEntity
import com.example.model.ScenarioEntity
import kotlinx.coroutines.flow.Flow

class AutomationRepository(private val database: AppDatabase) {
    private val scenarioDao = database.scenarioDao()
    private val logDao = database.executionLogDao()
    private val aiMemoryDao = database.aiMemoryDao()

    val allScenarios: Flow<List<ScenarioEntity>> = scenarioDao.getAllScenarios()
    val recentLogs: Flow<List<ExecutionLogEntity>> = logDao.getRecentLogs()
    val allMemories: Flow<List<AiMemoryEntity>> = aiMemoryDao.getAllMemories()

    suspend fun getScenarioById(id: Long): ScenarioEntity? = scenarioDao.getScenarioById(id)

    suspend fun saveScenario(scenario: ScenarioEntity): Long {
        return if (scenario.id == 0L) {
            scenarioDao.insertScenario(scenario)
        } else {
            scenarioDao.updateScenario(scenario)
            scenario.id
        }
    }

    suspend fun deleteScenario(id: Long) = scenarioDao.deleteScenarioById(id)

    suspend fun recordScenarioExecution(id: Long, isSuccess: Boolean) {
        scenarioDao.recordExecutionStats(id, if (isSuccess) 1 else 0, System.currentTimeMillis())
    }

    suspend fun log(scenarioId: Long, scenarioTitle: String, level: String, message: String, details: String = "") {
        logDao.insertLog(
            ExecutionLogEntity(
                scenarioId = scenarioId,
                scenarioTitle = scenarioTitle,
                level = level,
                message = message,
                details = details
            )
        )
    }

    suspend fun clearLogs() = logDao.clearLogs()

    suspend fun addMemory(memory: AiMemoryEntity) = aiMemoryDao.insertMemory(memory)

    suspend fun deleteMemory(id: Long) = aiMemoryDao.deleteMemoryById(id)

    suspend fun clearMemories() = aiMemoryDao.clearAllMemories()
}
