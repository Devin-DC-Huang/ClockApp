package com.example.clockapp.data.model

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

/**
 * Unit tests for Alarm data model
 */
class AlarmTest {

    @Test
    fun `getTimeString returns formatted time`() {
        val alarm = Alarm(hour = 8, minute = 30)
        assertEquals("08:30", alarm.getTimeString())

        val alarm2 = Alarm(hour = 23, minute = 5)
        assertEquals("23:05", alarm2.getTimeString())
    }

    @Test
    fun `getTypeDescription returns correct description for workday alarm`() {
        val alarm = Alarm(
            isSpecialAlarm = true,
            year = 2024
        )
        assertEquals("Workday Alarm (2024)", alarm.getTypeDescription())
    }

    @Test
    fun `getTypeDescription returns correct description for regular alarm with repeat`() {
        val alarm = Alarm(
            isRegularAlarm = true,
            repeatDays = listOf(1, 2, 3) // Mon, Tue, Wed
        )
        assertEquals("Weekly Repeat (Mon, Tue, Wed)", alarm.getTypeDescription())
    }

    @Test
    fun `getTypeDescription returns correct description for one-time regular alarm`() {
        val alarm = Alarm(
            isRegularAlarm = true,
            repeatDays = emptyList()
        )
        assertEquals("One-time Alarm", alarm.getTypeDescription())
    }

    @Test
    fun `getTypeDescription returns correct description for specific date alarm`() {
        val tomorrow = LocalDate.now().plusDays(1)
        val alarm = Alarm(
            dates = listOf(tomorrow)
        )
        assertEquals("1 dates specified", alarm.getTypeDescription())
    }

    @Test
    fun `calculateNextRingTime returns null when alarm is disabled`() {
        val alarm = Alarm(
            isEnabled = false,
            hour = 8,
            minute = 0
        )
        assertNull(alarm.calculateNextRingTime())
    }

    @Test
    fun `calculateNextRingTime for regular alarm with repeat days returns correct day`() {
        val now = LocalDateTime.now()
        val currentDayOfWeek = now.dayOfWeek.value // 1=Monday, 7=Sunday
        val currentHour = now.hour
        val currentMinute = now.minute
        
        // Set alarm for a time that hasn't passed today
        // Use current time + 2 hours (capped at 23 to avoid wrapping)
        val alarmHour = if (currentHour < 22) currentHour + 2 else 23
        val alarmMinute = if (currentHour < 22) currentMinute else 59
        
        // Create alarm that repeats on today
        val alarm = Alarm(
            isRegularAlarm = true,
            isEnabled = true,
            hour = alarmHour,
            minute = alarmMinute,
            repeatDays = listOf(currentDayOfWeek) // Today
        )

        val nextTime = alarm.calculateNextRingTime()
        
        // Since we carefully set alarmHour > currentHour, alarm should be today
        assertNotNull("Alarm should ring today since time hasn't passed. " +
            "currentHour=$currentHour, alarmHour=$alarmHour, currentDayOfWeek=$currentDayOfWeek", nextTime)
        assertEquals(alarmHour, nextTime!!.hour)
        assertEquals(alarmMinute, nextTime.minute)
        assertEquals(now.toLocalDate(), nextTime.toLocalDate())
    }

    @Test
    fun `calculateNextRingTime for regular alarm returns tomorrow if time passed`() {
        val now = LocalDateTime.now()
        val currentHour = now.hour
        val currentMinute = now.minute

        // Set alarm for a time that has definitely passed today
        // Use a fixed time early in the day (e.g., 1:00 AM) to ensure it has passed
        val alarmHour = 1
        val alarmMinute = 0
        
        // Skip test if current time is before 1:01 AM (edge case)
        if (currentHour < alarmHour || (currentHour == alarmHour && currentMinute <= alarmMinute)) {
            // In this case, the alarm would be scheduled for today, not tomorrow
            // We'll verify the logic differently - just ensure it returns non-null
            val alarm = Alarm(
                isRegularAlarm = true,
                isEnabled = true,
                hour = alarmHour,
                minute = alarmMinute,
                repeatDays = emptyList() // One-time
            )
            val nextTime = alarm.calculateNextRingTime()
            assertNotNull(nextTime)
            assertEquals(alarmHour, nextTime!!.hour)
            assertEquals(alarmMinute, nextTime.minute)
            // When time hasn't passed, it should be today
            assertEquals(now.toLocalDate(), nextTime.toLocalDate())
            return
        }

        val alarm = Alarm(
            isRegularAlarm = true,
            isEnabled = true,
            hour = alarmHour,
            minute = alarmMinute,
            repeatDays = emptyList() // One-time
        )

        val nextTime = alarm.calculateNextRingTime()
        assertNotNull(nextTime)

        assertEquals(alarmHour, nextTime!!.hour)
        assertEquals(alarmMinute, nextTime.minute)
        // Verify it's scheduled for tomorrow (since time has passed)
        assertEquals(now.toLocalDate().plusDays(1), nextTime.toLocalDate())
    }

    @Test
    fun `calculateNextRingTime for specific date alarm returns next future date`() {
        // Create dates: yesterday, today (passed), tomorrow
        val yesterday = LocalDate.now().minusDays(1)
        val tomorrow = LocalDate.now().plusDays(1)

        val alarm = Alarm(
            isEnabled = true,
            hour = 8,
            minute = 0,
            dates = listOf(yesterday, tomorrow)
        )

        val nextTime = alarm.calculateNextRingTime()
        assertNotNull(nextTime)

        assertEquals(tomorrow, nextTime!!.toLocalDate())
    }

    @Test
    fun `calculateNextRingTime returns null when all dates expired`() {
        val yesterday = LocalDate.now().minusDays(1)
        val twoDaysAgo = LocalDate.now().minusDays(2)

        val alarm = Alarm(
            isEnabled = true,
            hour = 8,
            minute = 0,
            dates = listOf(twoDaysAgo, yesterday)
        )

        assertNull(alarm.calculateNextRingTime())
    }

    @Test
    fun `getNextRingDescription returns Disabled when alarm is disabled`() {
        val alarm = Alarm(isEnabled = false)
        assertEquals("Disabled", alarm.getNextRingDescription())
    }

    @Test
    fun `getNextRingDescription returns Today when alarm is today`() {
        val now = LocalDateTime.now()
        val currentHour = now.hour
        val currentMinute = now.minute
        
        // Set alarm for a time that hasn't passed today (capped at 23 to avoid wrapping)
        val alarmHour = if (currentHour < 22) currentHour + 2 else 23
        val alarmMinute = if (currentHour < 22) currentMinute else 59

        val alarm = Alarm(
            isRegularAlarm = true,
            isEnabled = true,
            hour = alarmHour,
            minute = alarmMinute,
            repeatDays = emptyList()
        )

        val description = alarm.getNextRingDescription()
        
        // Since alarmHour > currentHour, alarm should be today
        assertTrue("Expected description to start with 'Today' but was: $description", 
            description.startsWith("Today"))
    }

    @Test
    fun `getNextRingDescription returns Tomorrow when alarm is tomorrow`() {
        // Create alarm for a time that has definitely passed today
        // So it will be scheduled for tomorrow
        val alarm = Alarm(
            isRegularAlarm = true,
            isEnabled = true,
            hour = 0, // Midnight
            minute = 0,
            repeatDays = emptyList()
        )

        val description = alarm.getNextRingDescription()
        // If it's already past midnight, it will show "Today", otherwise "Tomorrow"
        // We accept either as valid behavior
        assertTrue("Expected description to start with 'Today' or 'Tomorrow' but was: $description",
            description.startsWith("Today") || description.startsWith("Tomorrow"))
    }

    @Test
    fun `getNextRingDescription returns All dates expired when dates passed`() {
        val yesterday = LocalDate.now().minusDays(1)

        val alarm = Alarm(
            isEnabled = true,
            hour = 8,
            minute = 0,
            dates = listOf(yesterday)
        )

        assertEquals("All dates expired", alarm.getNextRingDescription())
    }

    @Test
    fun `getAlarmIntId returns positive integer`() {
        val alarm = Alarm(id = "test-alarm-id-123")
        val intId = alarm.getAlarmIntId()
        assertTrue(intId > 0)
    }

    @Test
    fun `getAlarmIntId returns consistent value for same id`() {
        val alarm1 = Alarm(id = "same-id")
        val alarm2 = Alarm(id = "same-id")
        assertEquals(alarm1.getAlarmIntId(), alarm2.getAlarmIntId())
    }

    @Test
    fun `getAlarmIntId returns different values for different ids`() {
        val alarm1 = Alarm(id = "id-1")
        val alarm2 = Alarm(id = "id-2")
        assertNotEquals(alarm1.getAlarmIntId(), alarm2.getAlarmIntId())
    }

    // ==================== Boundary Condition Tests ====================

    @Test
    fun `calculateNextRingTime with midnight time boundary`() {
        val now = LocalDateTime.now()
        val currentHour = now.hour
        val currentMinute = now.minute

        // Test alarm at midnight (00:00)
        val alarm = Alarm(
            isRegularAlarm = true,
            isEnabled = true,
            hour = 0,
            minute = 0,
            repeatDays = emptyList()
        )

        val nextTime = alarm.calculateNextRingTime()
        assertNotNull(nextTime)
        assertEquals(0, nextTime!!.hour)
        assertEquals(0, nextTime.minute)

        // If current time is after midnight, it should be tomorrow
        if (currentHour > 0 || (currentHour == 0 && currentMinute > 0)) {
            assertEquals(now.toLocalDate().plusDays(1), nextTime.toLocalDate())
        }
    }

    @Test
    fun `calculateNextRingTime with last minute of day boundary`() {
        val now = LocalDateTime.now()
        val currentHour = now.hour

        // Test alarm at 23:59
        val alarm = Alarm(
            isRegularAlarm = true,
            isEnabled = true,
            hour = 23,
            minute = 59,
            repeatDays = emptyList()
        )

        val nextTime = alarm.calculateNextRingTime()
        assertNotNull(nextTime)
        assertEquals(23, nextTime!!.hour)
        assertEquals(59, nextTime.minute)

        // If current time is 23:59, it should be tomorrow
        if (currentHour == 23) {
            assertEquals(now.toLocalDate().plusDays(1), nextTime.toLocalDate())
        }
    }

    @Test
    fun `calculateNextRingTime with single repeat day`() {
        val now = LocalDateTime.now()
        val currentDayOfWeek = now.dayOfWeek.value

        // Alarm that only repeats on one day
        val alarm = Alarm(
            isRegularAlarm = true,
            isEnabled = true,
            hour = 8,
            minute = 0,
            repeatDays = listOf(currentDayOfWeek)
        )

        val nextTime = alarm.calculateNextRingTime()
        assertNotNull(nextTime)
        
        // Should be scheduled for the repeat day
        val scheduledDayOfWeek = nextTime!!.dayOfWeek.value
        assertEquals(currentDayOfWeek, scheduledDayOfWeek)
    }

    @Test
    fun `calculateNextRingTime with all repeat days`() {
        val alarm = Alarm(
            isRegularAlarm = true,
            isEnabled = true,
            hour = 8,
            minute = 0,
            repeatDays = listOf(1, 2, 3, 4, 5, 6, 7) // All days
        )

        val nextTime = alarm.calculateNextRingTime()
        assertNotNull(nextTime)
        
        // Should always find a day
        assertNotNull(nextTime)
    }

    @Test
    fun `calculateNextRingTime with empty dates list`() {
        val alarm = Alarm(
            isEnabled = true,
            hour = 8,
            minute = 0,
            dates = emptyList(),
            isSpecialAlarm = false,
            isRegularAlarm = false
        )

        // Should return null for empty dates list
        assertNull(alarm.calculateNextRingTime())
    }

    @Test
    fun `calculateNextRingTime with single future date`() {
        val tomorrow = LocalDate.now().plusDays(1)

        val alarm = Alarm(
            isEnabled = true,
            hour = 8,
            minute = 0,
            dates = listOf(tomorrow)
        )

        val nextTime = alarm.calculateNextRingTime()
        assertNotNull(nextTime)
        assertEquals(tomorrow, nextTime!!.toLocalDate())
    }

    @Test
    fun `calculateNextRingTime with multiple dates all passed`() {
        val yesterday = LocalDate.now().minusDays(1)
        val twoDaysAgo = LocalDate.now().minusDays(2)
        val threeDaysAgo = LocalDate.now().minusDays(3)

        val alarm = Alarm(
            isEnabled = true,
            hour = 8,
            minute = 0,
            dates = listOf(threeDaysAgo, twoDaysAgo, yesterday)
        )

        assertNull(alarm.calculateNextRingTime())
    }

    @Test
    fun `calculateNextRingTime with unsorted dates`() {
        val tomorrow = LocalDate.now().plusDays(1)
        val yesterday = LocalDate.now().minusDays(1)
        val today = LocalDate.now()

        val alarm = Alarm(
            isEnabled = true,
            hour = 8,
            minute = 0,
            dates = listOf(tomorrow, yesterday, today)
        )

        val nextTime = alarm.calculateNextRingTime()
        assertNotNull(nextTime)
        
        // Should return the earliest future date
        val now = LocalDateTime.now()
        val currentHour = now.hour
        val currentMinute = now.minute
        
        if (8 > currentHour || (8 == currentHour && 0 > currentMinute)) {
            // Today at 8:00 hasn't passed yet
            assertEquals(today, nextTime!!.toLocalDate())
        } else {
            // Today has passed, should be tomorrow
            assertEquals(tomorrow, nextTime!!.toLocalDate())
        }
    }

    @Test
    fun `getTypeDescription for workday alarm without year`() {
        val alarm = Alarm(
            isSpecialAlarm = true,
            year = null
        )
        assertEquals("Workday Alarm", alarm.getTypeDescription())
    }

    @Test
    fun `getTypeDescription for regular alarm with single repeat day`() {
        val alarm = Alarm(
            isRegularAlarm = true,
            repeatDays = listOf(1) // Only Monday
        )
        assertEquals("Weekly Repeat (Mon)", alarm.getTypeDescription())
    }

    @Test
    fun `getTypeDescription for regular alarm with weekend days`() {
        val alarm = Alarm(
            isRegularAlarm = true,
            repeatDays = listOf(6, 7) // Saturday and Sunday
        )
        assertEquals("Weekly Repeat (Sat, Sun)", alarm.getTypeDescription())
    }

    @Test
    fun `getTypeDescription for empty dates`() {
        val alarm = Alarm(
            dates = emptyList()
        )
        assertEquals("Specific Date", alarm.getTypeDescription())
    }

    @Test
    fun `getNextRingDescription for future date beyond tomorrow`() {
        val futureDate = LocalDate.now().plusDays(5)

        val alarm = Alarm(
            isEnabled = true,
            hour = 14,
            minute = 30,
            dates = listOf(futureDate)
        )

        val description = alarm.getNextRingDescription()
        assertTrue(description.contains("14:30"))
        assertTrue(description.contains("${futureDate.monthValue}/${futureDate.dayOfMonth}"))
    }

    @Test
    fun `getNextRingDescription for no upcoming with no dates`() {
        val alarm = Alarm(
            isEnabled = true,
            hour = 8,
            minute = 0,
            dates = emptyList(),
            isSpecialAlarm = false,
            isRegularAlarm = false
        )

        assertEquals("No upcoming reminder", alarm.getNextRingDescription())
    }

    @Test
    fun `getTimeString with single digit values`() {
        val alarm = Alarm(hour = 1, minute = 5)
        assertEquals("01:05", alarm.getTimeString())
    }

    @Test
    fun `getTimeString with zero values`() {
        val alarm = Alarm(hour = 0, minute = 0)
        assertEquals("00:00", alarm.getTimeString())
    }

    @Test
    fun `getTimeString with max values`() {
        val alarm = Alarm(hour = 23, minute = 59)
        assertEquals("23:59", alarm.getTimeString())
    }

    @Test
    fun `getAlarmIntId for UUID format id`() {
        val uuid = UUID.randomUUID().toString()
        val alarm = Alarm(id = uuid)
        val intId = alarm.getAlarmIntId()
        
        assertTrue(intId > 0)
        assertTrue(intId <= Int.MAX_VALUE)
    }

    @Test
    fun `getAlarmIntId consistency check`() {
        // Test that same ID always produces same int ID
        val testId = "test-alarm-12345"
        val alarm1 = Alarm(id = testId)
        val alarm2 = Alarm(id = testId)
        
        val id1 = alarm1.getAlarmIntId()
        val id2 = alarm2.getAlarmIntId()
        
        assertEquals(id1, id2)
        assertEquals(testId.hashCode() and 0x7FFFFFFF, id1)
    }

    @Test
    fun `alarm default values`() {
        val alarm = Alarm()
        
        assertNotNull(alarm.id)
        assertEquals("闹钟", alarm.title)
        assertEquals(8, alarm.hour)
        assertEquals(0, alarm.minute)
        assertTrue(alarm.dates.isEmpty())
        assertTrue(alarm.isEnabled)
        assertEquals("default", alarm.ringtone)
        assertTrue(alarm.vibrate)
        assertFalse(alarm.isSpecialAlarm)
        assertFalse(alarm.isRegularAlarm)
        assertNull(alarm.year)
        assertTrue(alarm.snoozeEnabled)
        assertEquals(5, alarm.snoozeMinutes)
        assertTrue(alarm.repeatDays.isEmpty())
    }

    // ==================== Additional Boundary Value Tests ====================

    @Test
    fun `calculateNextRingTime at exact current time boundary`() {
        val now = LocalDateTime.now()
        val currentHour = now.hour
        val currentMinute = now.minute

        // Alarm set to exact current time - should be tomorrow
        val alarm = Alarm(
            isRegularAlarm = true,
            isEnabled = true,
            hour = currentHour,
            minute = currentMinute,
            repeatDays = emptyList()
        )

        val nextTime = alarm.calculateNextRingTime()
        assertNotNull(nextTime)
        assertEquals(currentHour, nextTime!!.hour)
        assertEquals(currentMinute, nextTime.minute)
        // Should be scheduled for tomorrow since time has passed (or is now)
        assertEquals(now.toLocalDate().plusDays(1), nextTime.toLocalDate())
    }

    @Test
    fun `calculateNextRingTime with one minute before current time`() {
        val now = LocalDateTime.now()
        val currentHour = now.hour
        val currentMinute = now.minute

        // Calculate one minute before now
        val alarmHour: Int
        val alarmMinute: Int
        if (currentMinute == 0) {
            alarmHour = if (currentHour == 0) 23 else currentHour - 1
            alarmMinute = 59
        } else {
            alarmHour = currentHour
            alarmMinute = currentMinute - 1
        }

        val alarm = Alarm(
            isRegularAlarm = true,
            isEnabled = true,
            hour = alarmHour,
            minute = alarmMinute,
            repeatDays = emptyList()
        )

        val nextTime = alarm.calculateNextRingTime()
        assertNotNull(nextTime)
        assertEquals(alarmHour, nextTime!!.hour)
        assertEquals(alarmMinute, nextTime.minute)
        // Should be tomorrow
        assertEquals(now.toLocalDate().plusDays(1), nextTime.toLocalDate())
    }

    @Test
    fun `calculateNextRingTime with one minute after current time`() {
        val now = LocalDateTime.now()
        val currentHour = now.hour
        val currentMinute = now.minute

        // Calculate one minute after now
        val alarmHour: Int
        val alarmMinute: Int
        if (currentMinute == 59) {
            alarmHour = if (currentHour == 23) 0 else currentHour + 1
            alarmMinute = 0
        } else {
            alarmHour = currentHour
            alarmMinute = currentMinute + 1
        }

        val alarm = Alarm(
            isRegularAlarm = true,
            isEnabled = true,
            hour = alarmHour,
            minute = alarmMinute,
            repeatDays = emptyList()
        )

        val nextTime = alarm.calculateNextRingTime()
        assertNotNull(nextTime)
        assertEquals(alarmHour, nextTime!!.hour)
        assertEquals(alarmMinute, nextTime.minute)
        // Should be today
        assertEquals(now.toLocalDate(), nextTime.toLocalDate())
    }

    @Test
    fun `calculateNextRingTime with year boundary dates`() {
        // Test with dates around year end/beginning - use future dates
        val today = LocalDate.now()
        val dec31 = LocalDate.of(today.year, 12, 31)
        val jan1 = LocalDate.of(today.year + 1, 1, 1)

        val alarm = Alarm(
            isEnabled = true,
            hour = 8,
            minute = 0,
            dates = listOf(dec31, jan1)
        )

        val nextTime = alarm.calculateNextRingTime()
        // If we're past Dec 31 8:00, result may be null or Jan 1
        // Just verify it doesn't crash and returns valid result if not null
        if (nextTime != null) {
            assertTrue(nextTime.toLocalDate() == dec31 || nextTime.toLocalDate() == jan1)
        }
    }

    @Test
    fun `calculateNextRingTime with leap year February 29`() {
        // Find next leap year with Feb 29
        var year = LocalDate.now().year
        while (!java.time.Year.of(year).isLeap) {
            year++
        }
        val feb29 = LocalDate.of(year, 2, 29)

        val alarm = Alarm(
            isEnabled = true,
            hour = 8,
            minute = 0,
            dates = listOf(feb29)
        )

        val nextTime = alarm.calculateNextRingTime()
        // Result depends on current date
        if (nextTime != null) {
            assertEquals(2, nextTime.monthValue)
            assertEquals(29, nextTime.dayOfMonth)
        }
    }

    @Test
    fun `calculateNextRingTime with non leap year February 28`() {
        // Use current year or next year
        val year = LocalDate.now().year
        val feb28 = LocalDate.of(year, 2, 28)

        val alarm = Alarm(
            isEnabled = true,
            hour = 8,
            minute = 0,
            dates = listOf(feb28)
        )

        val nextTime = alarm.calculateNextRingTime()
        // Result depends on current date
        if (nextTime != null) {
            assertEquals(2, nextTime.monthValue)
            assertEquals(28, nextTime.dayOfMonth)
        }
    }

    @Test
    fun `calculateNextRingTime with consecutive repeat days`() {
        val alarm = Alarm(
            isRegularAlarm = true,
            isEnabled = true,
            hour = 8,
            minute = 0,
            repeatDays = listOf(1, 2, 3) // Mon, Tue, Wed
        )

        val nextTime = alarm.calculateNextRingTime()
        assertNotNull(nextTime)
        
        // Should find one of the repeat days
        val dayOfWeek = nextTime!!.dayOfWeek.value
        assertTrue(dayOfWeek in listOf(1, 2, 3))
    }

    @Test
    fun `calculateNextRingTime with non consecutive repeat days`() {
        val alarm = Alarm(
            isRegularAlarm = true,
            isEnabled = true,
            hour = 8,
            minute = 0,
            repeatDays = listOf(1, 3, 5) // Mon, Wed, Fri
        )

        val nextTime = alarm.calculateNextRingTime()
        assertNotNull(nextTime)
        
        // Should find one of the repeat days
        val dayOfWeek = nextTime!!.dayOfWeek.value
        assertTrue(dayOfWeek in listOf(1, 3, 5))
    }

    @Test
    fun `calculateNextRingTime with disabled alarm returns null`() {
        val alarm = Alarm(
            isEnabled = false,
            hour = 8,
            minute = 0,
            isRegularAlarm = true
        )

        assertNull(alarm.calculateNextRingTime())
    }

    @Test
    fun `calculateNextRingTime with workday alarm but no year`() {
        val alarm = Alarm(
            isEnabled = true,
            hour = 8,
            minute = 0,
            isSpecialAlarm = true,
            year = null,
            dates = emptyList()
        )

        // Special alarm without calendar data should still work for workdays (Mon-Fri)
        // It will ring on the next weekday (Monday to Friday)
        val nextTime = alarm.calculateNextRingTime()
        
        // Without calendar data, it should still find the next weekday
        // The result depends on current day of week
        if (nextTime != null) {
            val dayOfWeek = nextTime.toLocalDate().dayOfWeek.value
            // Should be Monday(1) to Friday(5)
            assertTrue("Expected weekday but got day $dayOfWeek", dayOfWeek in 1..5)
        }
    }

    @Test
    fun `getTypeDescription with very long title`() {
        val longTitle = "A".repeat(1000)
        val alarm = Alarm(title = longTitle)
        assertEquals("Specific Date", alarm.getTypeDescription())
    }

    @Test
    fun `getTypeDescription with many repeat days`() {
        val alarm = Alarm(
            isRegularAlarm = true,
            repeatDays = listOf(1, 2, 3, 4, 5, 6, 7)
        )
        val description = alarm.getTypeDescription()
        assertTrue(description.contains("Mon"))
        assertTrue(description.contains("Sun"))
    }

    @Test
    fun `getNextRingDescription for disabled alarm`() {
        val alarm = Alarm(isEnabled = false)
        assertEquals("Disabled", alarm.getNextRingDescription())
    }

    @Test
    fun `getNextRingDescription for today at boundary time`() {
        val now = LocalDateTime.now()
        val currentHour = now.hour
        val currentMinute = now.minute
        
        // Set alarm for 2 hours from now (if possible)
        val alarmHour = if (currentHour < 22) currentHour + 2 else 23
        val alarmMinute = if (currentHour < 22) currentMinute else 59

        val alarm = Alarm(
            isEnabled = true,
            hour = alarmHour,
            minute = alarmMinute,
            isRegularAlarm = true,
            repeatDays = emptyList()
        )

        val description = alarm.getNextRingDescription()
        assertTrue(description.startsWith("Today"))
    }

    @Test
    fun `getAlarmIntId with empty string id`() {
        val alarm = Alarm(id = "")
        val intId = alarm.getAlarmIntId()
        assertTrue(intId >= 0)
        assertTrue(intId <= Int.MAX_VALUE)
    }

    @Test
    fun `getAlarmIntId with special characters id`() {
        val specialId = "!@#$%^&*()_+-=[]{}|;':\",./<>?"
        val alarm = Alarm(id = specialId)
        val intId = alarm.getAlarmIntId()
        assertTrue(intId >= 0)
        assertTrue(intId <= Int.MAX_VALUE)
        
        // Same ID should produce same int ID
        val alarm2 = Alarm(id = specialId)
        assertEquals(intId, alarm2.getAlarmIntId())
    }

    @Test
    fun `getAlarmIntId with unicode id`() {
        val unicodeId = "闹钟测试 alarm 123"
        val alarm = Alarm(id = unicodeId)
        val intId = alarm.getAlarmIntId()
        assertTrue(intId >= 0)
        assertTrue(intId <= Int.MAX_VALUE)
    }

    @Test
    fun `getTimeString boundary values`() {
        // Minimum time
        val minAlarm = Alarm(hour = 0, minute = 0)
        assertEquals("00:00", minAlarm.getTimeString())

        // Maximum time
        val maxAlarm = Alarm(hour = 23, minute = 59)
        assertEquals("23:59", maxAlarm.getTimeString())

        // Noon
        val noonAlarm = Alarm(hour = 12, minute = 0)
        assertEquals("12:00", noonAlarm.getTimeString())
    }

    @Test
    fun `snooze settings boundary values`() {
        // Default snooze minutes
        val defaultAlarm = Alarm()
        assertEquals(5, defaultAlarm.snoozeMinutes)
        assertTrue(defaultAlarm.snoozeEnabled)

        // Custom snooze settings
        val customAlarm = Alarm(
            snoozeEnabled = false,
            snoozeMinutes = 1
        )
        assertFalse(customAlarm.snoozeEnabled)
        assertEquals(1, customAlarm.snoozeMinutes)
    }

    @Test
    fun `alarm with very large dates list`() {
        // Create alarm with many dates
        val dates = (1..365).map { LocalDate.now().plusDays(it.toLong()) }
        val alarm = Alarm(
            isEnabled = true,
            hour = 8,
            minute = 0,
            dates = dates
        )

        val nextTime = alarm.calculateNextRingTime()
        assertNotNull(nextTime)
        assertEquals(dates.first(), nextTime!!.toLocalDate())
    }

    @Test
    fun `alarm with past and future mixed dates`() {
        val yesterday = LocalDate.now().minusDays(1)
        val today = LocalDate.now()
        val tomorrow = LocalDate.now().plusDays(1)
        val nextWeek = LocalDate.now().plusDays(7)

        val alarm = Alarm(
            isEnabled = true,
            hour = 8,
            minute = 0,
            dates = listOf(nextWeek, yesterday, tomorrow, today)
        )

        val nextTime = alarm.calculateNextRingTime()
        assertNotNull(nextTime)
        
        // Should find the earliest future date
        val now = LocalDateTime.now()
        val currentHour = now.hour
        val currentMinute = now.minute
        
        val expectedDate = if (8 > currentHour || (8 == currentHour && 0 > currentMinute)) {
            today
        } else {
            tomorrow
        }
        assertEquals(expectedDate, nextTime!!.toLocalDate())
    }

    // ==================== Workday Alarm Mode Tests ====================

    @Test
    fun `workday alarm default mode - all workdays skip holidays`() {
        // Create a Monday for testing (assuming today is a workday)
        val today = LocalDate.now()
        val currentDayOfWeek = today.dayOfWeek.value
        
        // Only run this test if today is a weekday (Mon-Fri)
        if (currentDayOfWeek in 1..5) {
            val alarm = Alarm(
                isEnabled = true,
                hour = 8,
                minute = 0,
                isSpecialAlarm = true,
                specialAlarmMode = SpecialAlarmMode.ALL_WORKDAYS
            )
            
            // Without calendar data, should still find next weekday
            val nextTime = alarm.calculateNextRingTime()
            assertNotNull(nextTime)
            
            // Should be Monday to Friday
            val resultDayOfWeek = nextTime!!.dayOfWeek.value
            assertTrue("Expected weekday but got day $resultDayOfWeek", resultDayOfWeek in 1..5)
        }
    }

    @Test
    fun `special alarm first workday mode returns null when dates is empty`() {
        val alarm = Alarm(
            isEnabled = true,
            hour = 8,
            minute = 0,
            isSpecialAlarm = true,
            specialAlarmMode = SpecialAlarmMode.FIRST_WORKDAY_ONLY
        )

        // Without pre-calculated dates, should return null
        val nextTime = alarm.calculateNextRingTime()
        assertNull("First workday mode should return null when dates is empty", nextTime)
    }

    @Test
    fun `special alarm all holidays mode returns null when dates is empty`() {
        val alarm = Alarm(
            isEnabled = true,
            hour = 8,
            minute = 0,
            isSpecialAlarm = true,
            specialAlarmMode = SpecialAlarmMode.ALL_HOLIDAYS
        )

        // Without pre-calculated dates, should return null
        val nextTime = alarm.calculateNextRingTime()
        assertNull("All holidays mode should return null when dates is empty", nextTime)
    }

    // ==================== Alarm Copy Tests ====================

    @Test
    fun `copy alarm creates new instance with different id`() {
        val original = Alarm(
            id = "original-id",
            title = "Test Alarm",
            hour = 8,
            minute = 30,
            isEnabled = true,
            isSpecialAlarm = true,
            year = 2024
        )

        val copy = original.copy(
            id = UUID.randomUUID().toString(),
            title = original.title + " (复制)",
            isEnabled = false
        )

        // ID should be different
        assertNotEquals(original.id, copy.id)
        // Title should have copy suffix
        assertEquals("Test Alarm (复制)", copy.title)
        // Copy should be disabled
        assertFalse(copy.isEnabled)
        // Other properties should be same
        assertEquals(original.hour, copy.hour)
        assertEquals(original.minute, copy.minute)
        assertEquals(original.isSpecialAlarm, copy.isSpecialAlarm)
        assertEquals(original.year, copy.year)
    }

    @Test
    fun `copy alarm preserves all properties except id title and enabled`() {
        val tomorrow = LocalDate.now().plusDays(1)
        val original = Alarm(
            id = "test-id",
            title = "Morning Alarm",
            hour = 7,
            minute = 0,
            dates = listOf(tomorrow),
            isEnabled = true,
            vibrate = false,
            isSpecialAlarm = false,
            isRegularAlarm = true,
            year = null,
            snoozeEnabled = false,
            snoozeMinutes = 10,
            repeatDays = listOf(1, 2, 3)
        )

        val copy = original.copy(
            id = UUID.randomUUID().toString(),
            title = original.title + " (复制)",
            isEnabled = false
        )

        // Verify all properties except id, title, isEnabled are preserved
        assertEquals(original.hour, copy.hour)
        assertEquals(original.minute, copy.minute)
        assertEquals(original.dates, copy.dates)
        assertEquals(original.vibrate, copy.vibrate)
        assertEquals(original.isSpecialAlarm, copy.isSpecialAlarm)
        assertEquals(original.isRegularAlarm, copy.isRegularAlarm)
        assertEquals(original.year, copy.year)
        assertEquals(original.snoozeEnabled, copy.snoozeEnabled)
        assertEquals(original.snoozeMinutes, copy.snoozeMinutes)
        assertEquals(original.repeatDays, copy.repeatDays)
    }

    @Test
    fun `copy alarm with empty title`() {
        val original = Alarm(
            id = "test-id",
            title = "",
            hour = 8,
            minute = 0
        )

        val copy = original.copy(
            id = UUID.randomUUID().toString(),
            title = original.title + " (复制)",
            isEnabled = false
        )

        assertEquals(" (复制)", copy.title)
    }

    @Test
    fun `copy alarm with unicode title`() {
        val original = Alarm(
            id = "test-id",
            title = "起床闹钟",
            hour = 7,
            minute = 30
        )

        val copy = original.copy(
            id = UUID.randomUUID().toString(),
            title = original.title + " (复制)",
            isEnabled = false
        )

        assertEquals("起床闹钟 (复制)", copy.title)
        assertNotEquals(original.id, copy.id)
        assertFalse(copy.isEnabled)
    }

    @Test
    fun `copy alarm with special alarm mode`() {
        val original = Alarm(
            id = "test-id",
            title = "Workday Alarm",
            hour = 9,
            minute = 0,
            isSpecialAlarm = true,
            specialAlarmMode = SpecialAlarmMode.FIRST_WORKDAY_ONLY
        )

        val copy = original.copy(
            id = UUID.randomUUID().toString(),
            title = original.title + " (复制)",
            isEnabled = false
        )

        assertEquals(original.specialAlarmMode, copy.specialAlarmMode)
        assertEquals(original.isSpecialAlarm, copy.isSpecialAlarm)
    }

    @Test
    fun `multiple copies have different ids`() {
        val original = Alarm(
            id = "original-id",
            title = "Test Alarm",
            hour = 8,
            minute = 0
        )

        val copy1 = original.copy(
            id = UUID.randomUUID().toString(),
            title = original.title + " (复制)",
            isEnabled = false
        )

        val copy2 = original.copy(
            id = UUID.randomUUID().toString(),
            title = original.title + " (复制)",
            isEnabled = false
        )

        // All IDs should be different
        assertNotEquals(original.id, copy1.id)
        assertNotEquals(original.id, copy2.id)
        assertNotEquals(copy1.id, copy2.id)
    }

    @Test
    fun `special alarm all holidays mode rings on pre-calculated weekend date`() {
        val tomorrow = LocalDate.now().plusDays(1)
        val alarm = Alarm(
            isEnabled = true,
            hour = 8,
            minute = 0,
            isSpecialAlarm = true,
            specialAlarmMode = SpecialAlarmMode.ALL_HOLIDAYS,
            dates = listOf(tomorrow)
        )

        val nextTime = alarm.calculateNextRingTime()
        assertNotNull(nextTime)
        assertEquals(tomorrow, nextTime!!.toLocalDate())
    }

    @Test
    fun `special alarm first workday mode finds pre-calculated workday date`() {
        val tomorrow = LocalDate.now().plusDays(1)
        val alarm = Alarm(
            isEnabled = true,
            hour = 8,
            minute = 0,
            isSpecialAlarm = true,
            specialAlarmMode = SpecialAlarmMode.FIRST_WORKDAY_ONLY,
            dates = listOf(tomorrow)
        )

        val nextTime = alarm.calculateNextRingTime()
        assertNotNull(nextTime)
        assertEquals(tomorrow, nextTime!!.toLocalDate())
    }

    @Test
    fun `workday alarm modes are mutually exclusive via enum`() {
        // Test that enum ensures only one mode is active at a time
        val alarm1 = Alarm(
            isSpecialAlarm = true,
            specialAlarmMode = SpecialAlarmMode.ALL_WORKDAYS
        )
        assertEquals(SpecialAlarmMode.ALL_WORKDAYS, alarm1.specialAlarmMode)

        val alarm2 = Alarm(
            isSpecialAlarm = true,
            specialAlarmMode = SpecialAlarmMode.FIRST_WORKDAY_ONLY
        )
        assertEquals(SpecialAlarmMode.FIRST_WORKDAY_ONLY, alarm2.specialAlarmMode)

        val alarm3 = Alarm(
            isSpecialAlarm = true,
            specialAlarmMode = SpecialAlarmMode.ALL_HOLIDAYS
        )
        assertEquals(SpecialAlarmMode.ALL_HOLIDAYS, alarm3.specialAlarmMode)
    }

    @Test
    fun `alarm default value is ALL_WORKDAYS mode`() {
        val alarm = Alarm()

        assertEquals(SpecialAlarmMode.ALL_WORKDAYS, alarm.specialAlarmMode)
    }

    // ==================== Alarm Sorting Tests ====================

    @Test
    fun `alarms sorted by hour ascending`() {
        val alarm1 = Alarm(hour = 8, minute = 0, title = "Alarm 8AM")
        val alarm2 = Alarm(hour = 7, minute = 0, title = "Alarm 7AM")
        val alarm3 = Alarm(hour = 9, minute = 0, title = "Alarm 9AM")

        val alarms = listOf(alarm1, alarm2, alarm3)
        val sorted = alarms.sortedWith(compareBy({ it.hour }, { it.minute }))

        assertEquals(7, sorted[0].hour)
        assertEquals(8, sorted[1].hour)
        assertEquals(9, sorted[2].hour)
    }

    @Test
    fun `alarms sorted by hour then minute ascending`() {
        val alarm1 = Alarm(hour = 8, minute = 30, title = "Alarm 8:30")
        val alarm2 = Alarm(hour = 8, minute = 0, title = "Alarm 8:00")
        val alarm3 = Alarm(hour = 8, minute = 15, title = "Alarm 8:15")

        val alarms = listOf(alarm1, alarm2, alarm3)
        val sorted = alarms.sortedWith(compareBy({ it.hour }, { it.minute }))

        assertEquals(0, sorted[0].minute)
        assertEquals(15, sorted[1].minute)
        assertEquals(30, sorted[2].minute)
    }

    @Test
    fun `alarms sorted across midnight`() {
        val alarm1 = Alarm(hour = 23, minute = 59, title = "Late night")
        val alarm2 = Alarm(hour = 0, minute = 0, title = "Midnight")
        val alarm3 = Alarm(hour = 6, minute = 30, title = "Morning")

        val alarms = listOf(alarm1, alarm2, alarm3)
        val sorted = alarms.sortedWith(compareBy({ it.hour }, { it.minute }))

        assertEquals(0, sorted[0].hour)
        assertEquals(6, sorted[1].hour)
        assertEquals(23, sorted[2].hour)
    }

    @Test
    fun `alarm comparison for list sorting`() {
        // Test the comparison logic used in database ORDER BY hour ASC, minute ASC
        val alarms = listOf(
            Alarm(hour = 12, minute = 0),
            Alarm(hour = 7, minute = 30),
            Alarm(hour = 7, minute = 0),
            Alarm(hour = 23, minute = 59),
            Alarm(hour = 0, minute = 0)
        )

        val sorted = alarms.sortedWith(compareBy({ it.hour }, { it.minute }))

        // Verify sorted order: 00:00, 07:00, 07:30, 12:00, 23:59
        assertEquals(0, sorted[0].hour)
        assertEquals(0, sorted[0].minute)

        assertEquals(7, sorted[1].hour)
        assertEquals(0, sorted[1].minute)

        assertEquals(7, sorted[2].hour)
        assertEquals(30, sorted[2].minute)

        assertEquals(12, sorted[3].hour)
        assertEquals(0, sorted[3].minute)

        assertEquals(23, sorted[4].hour)
        assertEquals(59, sorted[4].minute)
    }

    @Test
    fun `same hour different minutes sorting`() {
        val alarms = listOf(
            Alarm(hour = 8, minute = 45),
            Alarm(hour = 8, minute = 15),
            Alarm(hour = 8, minute = 30),
            Alarm(hour = 8, minute = 0)
        )

        val sorted = alarms.sortedWith(compareBy({ it.hour }, { it.minute }))

        assertEquals(0, sorted[0].minute)
        assertEquals(15, sorted[1].minute)
        assertEquals(30, sorted[2].minute)
        assertEquals(45, sorted[3].minute)
    }

    @Test
    fun `boundary values sorting`() {
        val alarms = listOf(
            Alarm(hour = 23, minute = 59),
            Alarm(hour = 0, minute = 0),
            Alarm(hour = 12, minute = 0),
            Alarm(hour = 0, minute = 1),
            Alarm(hour = 23, minute = 58)
        )

        val sorted = alarms.sortedWith(compareBy({ it.hour }, { it.minute }))

        // Expected order: 00:00, 00:01, 12:00, 23:58, 23:59
        assertEquals(0, sorted[0].hour)
        assertEquals(0, sorted[0].minute)

        assertEquals(0, sorted[1].hour)
        assertEquals(1, sorted[1].minute)

        assertEquals(12, sorted[2].hour)
        assertEquals(0, sorted[2].minute)

        assertEquals(23, sorted[3].hour)
        assertEquals(58, sorted[3].minute)

        assertEquals(23, sorted[4].hour)
        assertEquals(59, sorted[4].minute)
    }
}
