package com.fit5046.ergodesk.data.repository

import android.content.Context
import com.fit5046.ergodesk.data.local.ErgoDeskDatabase
import com.fit5046.ergodesk.data.local.StretchDao
import com.fit5046.ergodesk.data.model.StretchItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository providing a clean API for Stretch routines backed by Room database.
 */
class StretchRepository(
    private val stretchDao: StretchDao
) {
    val allStretches: Flow<List<StretchItemEntity>> = stretchDao.getAllStretches()

    fun getStretchesByArea(area: String): Flow<List<StretchItemEntity>> {
        return if (area.equals("All", ignoreCase = true)) {
            stretchDao.getAllStretches()
        } else {
            stretchDao.getStretchesByArea(area)
        }
    }

    fun getStretchById(id: Long): Flow<StretchItemEntity?> {
        return stretchDao.getStretchById(id)
    }

    suspend fun insertStretch(stretch: StretchItemEntity): Long {
        return stretchDao.insertStretch(stretch)
    }

    suspend fun deleteStretch(stretch: StretchItemEntity) {
        stretchDao.deleteStretch(stretch)
    }

    suspend fun deleteById(id: Long) {
        stretchDao.deleteById(id)
    }

    /**
     * Seeds initial default ergonomic routines into Room database on first launch.
     */
    suspend fun seedDefaultStretchesIfEmpty() {
        if (stretchDao.getCount() == 0) {
            val defaults = listOf(
                StretchItemEntity(
                    title = "Seated Spinal Twist",
                    targetArea = "Lower back",
                    durationSeconds = 60,
                    difficulty = "Easy",
                    instructions = "Sit upright on the front edge of your chair with both feet flat on the floor.\n" +
                            "Place your left hand on your right knee and right hand on the chair back/armrest.\n" +
                            "Inhale to lengthen your spine, exhale and gently rotate your torso to the right.\n" +
                            "Hold for 30 seconds breathing steadily, then repeat on the left side.",
                    isCustom = false
                ),
                StretchItemEntity(
                    title = "Desk Neck Extension & Flexion",
                    targetArea = "Neck",
                    durationSeconds = 45,
                    difficulty = "Easy",
                    instructions = "Relax your shoulders completely and gently drop your chin down to your chest for 15s.\n" +
                            "Slowly raise your head back up and gaze gently toward the ceiling for 15s.\n" +
                            "Return to neutral, then gently tilt your right ear toward your right shoulder for 15s.",
                    isCustom = false
                ),
                StretchItemEntity(
                    title = "Shoulder Blade Squeeze",
                    targetArea = "Shoulders",
                    durationSeconds = 60,
                    difficulty = "Easy",
                    instructions = "Sit tall and bend your elbows at 90 degrees by your sides.\n" +
                            "Pull your elbows straight back while firmly squeezing your shoulder blades together.\n" +
                            "Hold the squeeze for 5 seconds, relax for 2 seconds, and repeat 8-10 times.",
                    isCustom = false
                ),
                StretchItemEntity(
                    title = "Wrist & Finger Flexor Stretch",
                    targetArea = "Wrists",
                    durationSeconds = 30,
                    difficulty = "Quick",
                    instructions = "Extend your right arm in front of you with palm facing forward and fingers up.\n" +
                            "Use your left hand to gently pull your right fingers back toward your body for 15s.\n" +
                            "Point fingers downward, pull gently back for 15s, then switch to your other hand.",
                    isCustom = false
                ),
                StretchItemEntity(
                    title = "Seated Chest Opener",
                    targetArea = "Chest",
                    durationSeconds = 45,
                    difficulty = "Easy",
                    instructions = "Sit forward on your chair and interlace your fingers behind your lower back.\n" +
                            "Roll your shoulders back and gently lift your chest upward toward the ceiling.\n" +
                            "Take 5 slow, deep breaths, feeling your chest and anterior shoulder muscles open up.",
                    isCustom = false
                ),
                StretchItemEntity(
                    title = "Seated Hamstring Reach",
                    targetArea = "Legs",
                    durationSeconds = 60,
                    difficulty = "Medium",
                    instructions = "Extend your right leg straight forward with your heel on the floor and toes up.\n" +
                            "Hinge forward from your hips with a flat back until you feel a mild stretch behind the thigh.\n" +
                            "Hold for 30 seconds breathing normally, then switch to your left leg.",
                    isCustom = false
                )
            )
            stretchDao.insertAll(defaults)
        }
    }

    companion object {
        @Volatile
        private var instance: StretchRepository? = null

        fun getInstance(context: Context): StretchRepository {
            return instance ?: synchronized(this) {
                val database = ErgoDeskDatabase.getDatabase(context)
                val newInstance = StretchRepository(database.stretchDao())
                instance = newInstance
                newInstance
            }
        }
    }
}
