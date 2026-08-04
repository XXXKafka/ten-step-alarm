package com.tenstep.alarm.data

import com.tenstep.alarm.alarm.AlarmScheduling
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for alarms. Every mutation keeps the AlarmManager in
 * sync so the next scheduled alarm always matches the stored rows.
 */
class AlarmRepository(
    private val dao: AlarmDao,
    private val scheduler: AlarmScheduling
) {

    fun observeAlarms(): Flow<List<AlarmEntity>> = dao.observeAll()

    /** Number of currently enabled alarms (drives the alarm guard service). */
    fun observeEnabledCount(): Flow<Int> = dao.observeEnabledCount()

    suspend fun getAlarm(id: Long): AlarmEntity? = dao.getById(id)

    /** Insert or update an alarm and (re)schedule it. Returns the alarm id. */
    suspend fun upsert(alarm: AlarmEntity): Long {
        val id = if (alarm.id == 0L) dao.insert(alarm) else {
            dao.update(alarm)
            alarm.id
        }
        dao.getById(id)?.let { scheduler.schedule(it) }
        return id
    }

    suspend fun delete(alarm: AlarmEntity) {
        scheduler.cancel(alarm.id)
        dao.delete(alarm)
    }

    suspend fun setEnabled(alarm: AlarmEntity, enabled: Boolean) {
        val updated = alarm.copy(enabled = enabled)
        dao.update(updated)
        if (enabled) {
            scheduler.schedule(updated)
        } else {
            scheduler.cancel(alarm.id)
        }
    }

    /** Schedule the next occurrence of a repeating alarm (after it fired). */
    suspend fun reschedule(alarm: AlarmEntity) {
        scheduler.cancel(alarm.id)
        scheduler.schedule(alarm)
    }

    /** Recreate all enabled alarms (used after reboot / permission changes). */
    suspend fun rescheduleAll() {
        dao.getEnabled().forEach { alarm ->
            scheduler.cancel(alarm.id)
            scheduler.schedule(alarm)
        }
    }
}