package com.tenstep.alarm.data

import app.cash.turbine.test
import com.tenstep.alarm.alarm.AlarmScheduling
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmRepositoryTest {

    private class FakeScheduler : AlarmScheduling {
        val scheduled = mutableListOf<Long>()
        val cancelled = mutableListOf<Long>()
        val snoozed = mutableListOf<Pair<Long, Int>>()
        var exact = true

        override fun schedule(alarm: AlarmEntity) { scheduled += alarm.id }
        override fun scheduleSnooze(alarmId: Long, minutes: Int) { snoozed += alarmId to minutes }
        override fun cancel(alarmId: Long) { cancelled += alarmId }
        override fun canScheduleExact(): Boolean = exact
    }

    private class FakeDao : AlarmDao {
        private val state = MutableStateFlow<List<AlarmEntity>>(emptyList())

        override fun observeAll(): Flow<List<AlarmEntity>> = state
        override fun observeEnabledCount(): Flow<Int> = state.map { list -> list.count { it.enabled } }
        override suspend fun getById(id: Long): AlarmEntity? = state.value.firstOrNull { it.id == id }
        override suspend fun getEnabled(): List<AlarmEntity> = state.value.filter { it.enabled }
        override suspend fun insert(alarm: AlarmEntity): Long {
            val id = (state.value.maxOfOrNull { it.id } ?: 0L) + 1
            state.value = state.value + alarm.copy(id = id)
            return id
        }
        override suspend fun update(alarm: AlarmEntity) {
            state.value = state.value.map { if (it.id == alarm.id) alarm else it }
        }
        override suspend fun delete(alarm: AlarmEntity) {
            state.value = state.value.filterNot { it.id == alarm.id }
        }
    }

    private fun alarm(
        id: Long = 0L,
        hour: Int = 7,
        enabled: Boolean = true,
        daysOfWeek: Int = 0
    ) = AlarmEntity(
        id = id,
        hour = hour,
        minute = 30,
        daysOfWeek = daysOfWeek,
        label = "test",
        ringtoneUri = "",
        volume = 70,
        vibrate = true,
        snoozeEnabled = true,
        enabled = enabled,
        oneShot = daysOfWeek == 0
    )

    @Test
    fun `upsert schedules a new alarm`() = runTest {
        val dao = FakeDao()
        val scheduler = FakeScheduler()
        val repo = AlarmRepository(dao, scheduler)

        val id = repo.upsert(alarm())

        assertTrue(id > 0L)
        assertEquals(listOf(id), scheduler.scheduled)
    }

    @Test
    fun `upsert reschedules an existing alarm`() = runTest {
        val dao = FakeDao()
        val scheduler = FakeScheduler()
        val repo = AlarmRepository(dao, scheduler)
        val id = repo.upsert(alarm())

        repo.upsert(alarm(id = id, hour = 8))

        assertEquals(listOf(id, id), scheduler.scheduled)
        assertEquals(8, dao.getById(id)?.hour)
    }

    @Test
    fun `delete cancels and removes`() = runTest {
        val dao = FakeDao()
        val scheduler = FakeScheduler()
        val repo = AlarmRepository(dao, scheduler)
        val id = repo.upsert(alarm())

        repo.delete(dao.getById(id)!!)

        assertEquals(listOf(id), scheduler.cancelled)
        assertEquals(null, dao.getById(id))
    }

    @Test
    fun `setEnabled false cancels and true schedules`() = runTest {
        val dao = FakeDao()
        val scheduler = FakeScheduler()
        val repo = AlarmRepository(dao, scheduler)
        val id = repo.upsert(alarm())

        repo.setEnabled(dao.getById(id)!!, enabled = false)
        assertEquals(listOf(id), scheduler.cancelled)

        repo.setEnabled(dao.getById(id)!!, enabled = true)
        assertEquals(listOf(id, id), scheduler.scheduled)
    }

    @Test
    fun `enabled count flow reflects changes`() = runTest {
        val dao = FakeDao()
        val scheduler = FakeScheduler()
        val repo = AlarmRepository(dao, scheduler)
        val id = repo.upsert(alarm())

        repo.observeEnabledCount().test {
            assertEquals(1, awaitItem())
            repo.setEnabled(dao.getById(id)!!, enabled = false)
            assertEquals(0, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `rescheduleAll only schedules enabled alarms`() = runTest {
        val dao = FakeDao()
        val scheduler = FakeScheduler()
        val repo = AlarmRepository(dao, scheduler)
        val id1 = repo.upsert(alarm())
        repo.upsert(alarm(enabled = false))

        scheduler.scheduled.clear()
        repo.rescheduleAll()

        assertEquals(listOf(id1), scheduler.scheduled)
    }
}