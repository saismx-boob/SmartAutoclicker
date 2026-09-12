package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.ActionStep
import com.example.model.ActionType
import com.example.model.AiMemoryEntity
import com.example.model.ConditionType
import com.example.model.ExecutionLogEntity
import com.example.model.ScenarioEntity
import com.example.model.TargetType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ScenarioEntity::class,
        ExecutionLogEntity::class,
        AiMemoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scenarioDao(): ScenarioDao
    abstract fun executionLogDao(): ExecutionLogDao
    abstract fun aiMemoryDao(): AiMemoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "autoclick_ai_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }

            private suspend fun populateInitialData(database: AppDatabase) {
                val scenarioDao = database.scenarioDao()
                val logDao = database.executionLogDao()
                val memoryDao = database.aiMemoryDao()

                // Scenario 1: Récolte & Récompenses
                val steps1 = listOf(
                    ActionStep(
                        stepNumber = 1,
                        name = "Détecter et cliquer 'Réclamer'",
                        actionType = ActionType.CLICK,
                        targetType = TargetType.TEXT_MATCH,
                        targetText = "Réclamer",
                        delayBeforeMs = 400L,
                        humanizeJitterRadius = 10,
                        humanizeTimingVariance = 15
                    ),
                    ActionStep(
                        stepNumber = 2,
                        name = "Attente d'animation",
                        actionType = ActionType.WAIT_DELAY,
                        durationMs = 1200L,
                        delayBeforeMs = 200L
                    ),
                    ActionStep(
                        stepNumber = 3,
                        name = "Confirmer la réception",
                        actionType = ActionType.CLICK,
                        targetType = TargetType.TEXT_MATCH,
                        targetText = "Confirmer",
                        conditionType = ConditionType.IF_TEXT_PRESENT,
                        conditionParam = "Confirmer",
                        delayBeforeMs = 500L
                    )
                )
                scenarioDao.insertScenario(
                    ScenarioEntity(
                        title = "🌾 Récolte & Récompenses Quotidiennes",
                        description = "Détecte automatiquement les boutons de récompenses, applique des clics humains et confirme.",
                        stepsJson = JsonUtils.stepsToJson(steps1),
                        loopCount = 5,
                        intervalBetweenLoopsMs = 2000L,
                        scheduleDescription = "Boucle (5x)",
                        totalExecutions = 14,
                        successExecutions = 14
                    )
                )

                // Scenario 2: Défilement & Lecture Humaine
                val steps2 = listOf(
                    ActionStep(
                        stepNumber = 1,
                        name = "Défilement vertical fluide",
                        actionType = ActionType.SWIPE,
                        targetType = TargetType.COORDINATES,
                        targetX = 540f,
                        targetY = 1600f,
                        swipeEndX = 540f,
                        swipeEndY = 700f,
                        durationMs = 380L,
                        delayBeforeMs = 800L,
                        humanizeJitterRadius = 25,
                        humanizeTimingVariance = 20
                    ),
                    ActionStep(
                        stepNumber = 2,
                        name = "Pause lecture aléatoire",
                        actionType = ActionType.WAIT_DELAY,
                        durationMs = 2500L,
                        delayBeforeMs = 300L
                    ),
                    ActionStep(
                        stepNumber = 3,
                        name = "Vérifier notification",
                        actionType = ActionType.CLICK,
                        targetType = TargetType.TEXT_MATCH,
                        targetText = "Nouveau",
                        conditionType = ConditionType.IF_TEXT_PRESENT,
                        conditionParam = "Nouveau",
                        delayBeforeMs = 600L
                    )
                )
                scenarioDao.insertScenario(
                    ScenarioEntity(
                        title = "🔄 Défilement & Lecture Humaine",
                        description = "Parcourt des fils d'actualités avec vitesse variable et micro-pauses indétectables.",
                        stepsJson = JsonUtils.stepsToJson(steps2),
                        loopCount = 0, // Infinite
                        intervalBetweenLoopsMs = 1500L,
                        scheduleDescription = "Boucle continue",
                        totalExecutions = 42,
                        successExecutions = 41
                    )
                )

                // Scenario 3: Agent Autonome IA
                val steps3 = listOf(
                    ActionStep(
                        stepNumber = 1,
                        name = "Observation visuelle IA",
                        actionType = ActionType.WAIT_DELAY,
                        targetType = TargetType.SCREEN_CHANGE,
                        durationMs = 500L
                    ),
                    ActionStep(
                        stepNumber = 2,
                        name = "Clic intelligent adaptatif",
                        actionType = ActionType.CLICK,
                        targetType = TargetType.TEXT_MATCH,
                        targetText = "Suivant",
                        delayBeforeMs = 700L
                    )
                )
                scenarioDao.insertScenario(
                    ScenarioEntity(
                        title = "🧠 Agent Autonome : Complétion de Tâche",
                        description = "L'intelligence artificielle analyse le flux d'écran et adapte ses actions en temps réel.",
                        stepsJson = JsonUtils.stepsToJson(steps3),
                        loopCount = 1,
                        isAiControlled = true,
                        aiGoalPrompt = "Complète le processus d'onboarding ou de formulaire jusqu'au bouton Terminer",
                        scheduleDescription = "Guidé par IA",
                        totalExecutions = 8,
                        successExecutions = 7
                    )
                )

                // Scenario 4: Bloc Décisionnel Si/Alors/Sinon
                val steps4 = listOf(
                    ActionStep(
                        stepNumber = 1,
                        name = "Si Bouton 'Valider' visible ➔ Clic, Sinon ➔ Glisser",
                        actionType = ActionType.BRANCH_IF_ELSE,
                        conditionType = ConditionType.IF_IMAGE_PRESENT,
                        conditionParam = "ic_check",
                        targetImageTemplate = "ic_check",
                        targetImageName = "Bouton Valider (✓)",
                        targetType = TargetType.IMAGE_MATCH,
                        thenActionType = ActionType.CLICK,
                        targetX = 540f,
                        targetY = 1200f,
                        thenDurationMs = 300L,
                        elseActionType = ActionType.SWIPE,
                        elseDurationMs = 500L,
                        elseTargetX = 540f,
                        elseTargetY = 1600f
                    ),
                    ActionStep(
                        stepNumber = 2,
                        name = "Pause de stabilisation",
                        actionType = ActionType.WAIT_DELAY,
                        durationMs = 1000L
                    )
                )
                scenarioDao.insertScenario(
                    ScenarioEntity(
                        title = "🔀 Décision Intelligente (Si / Alors / Sinon)",
                        description = "Vérifie si le bouton Valider apparaît à l'écran : si oui il clique dessus, sinon il fait défiler l'écran pour le chercher.",
                        stepsJson = JsonUtils.stepsToJson(steps4),
                        loopCount = 3,
                        intervalBetweenLoopsMs = 1000L,
                        scheduleDescription = "3 cycles",
                        totalExecutions = 12,
                        successExecutions = 12
                    )
                )

                // Seed some logs
                logDao.insertLog(
                    ExecutionLogEntity(
                        scenarioId = 1,
                        scenarioTitle = "🌾 Récolte & Récompenses Quotidiennes",
                        level = "INFO",
                        message = "Démarrage du scénario (Mode humain activé: gigue 10px)"
                    )
                )
                logDao.insertLog(
                    ExecutionLogEntity(
                        scenarioId = 1,
                        scenarioTitle = "🌾 Récolte & Récompenses Quotidiennes",
                        level = "DÉTECTION",
                        message = "Cible 'Réclamer' détectée aux coordonnées (540, 1140)"
                    )
                )
                logDao.insertLog(
                    ExecutionLogEntity(
                        scenarioId = 1,
                        scenarioTitle = "🌾 Récolte & Récompenses Quotidiennes",
                        level = "ACTION",
                        message = "Clic naturel exécuté à (543, 1144) avec durée 84ms"
                    )
                )
                logDao.insertLog(
                    ExecutionLogEntity(
                        scenarioId = 1,
                        scenarioTitle = "🌾 Récolte & Récompenses Quotidiennes",
                        level = "SUCCÈS",
                        message = "Cycle 1 complété avec succès"
                    )
                )

                // Seed some AI memories
                memoryDao.insertMemory(
                    AiMemoryEntity(
                        scenarioTitle = "🌾 Récolte & Récompenses Quotidiennes",
                        goal = "Récupérer la récompense quotidienne",
                        observedState = "Bouton vert 'Réclamer' visible en bas d'écran",
                        actionDecision = "Clic centré avec micro-délai de 400ms pour laisser finir l'animation",
                        outcome = "SUCCESS",
                        learnedRule = "Attendre au moins 350ms après ouverture de pop-up pour éviter les clics ignorés."
                    )
                )
                memoryDao.insertMemory(
                    AiMemoryEntity(
                        scenarioTitle = "🔄 Défilement & Lecture Humaine",
                        goal = "Simuler un comportement d'utilisateur naturel",
                        observedState = "Fin de page atteinte",
                        actionDecision = "Pause de 2.5s puis swipe inversé court pour simuler relecture",
                        outcome = "SUCCESS",
                        learnedRule = "Varier l'angle de swipe de ±5 degrés pour éviter la signature de script rectiligne."
                    )
                )
            }
        }
    }
}
