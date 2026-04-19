package com.example.clockapp.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.clockapp.data.db.AlarmDatabase
import com.example.clockapp.data.model.Alarm
import com.example.clockapp.service.AlarmScheduler
import kotlinx.coroutines.launch

/**
 * Main view model
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val alarmDao = AlarmDatabase.getDatabase(application).alarmDao()
    val alarms: LiveData<List<Alarm>> = alarmDao.getAllAlarms()

    /**
     * Update alarm state
     */
    fun updateAlarmState(alarm: Alarm, isEnabled: Boolean) {
        viewModelScope.launch {
            val updatedAlarm = alarm.copy(isEnabled = isEnabled)
            alarmDao.updateAlarm(updatedAlarm)

            // Schedule or cancel alarm
            if (isEnabled) {
                AlarmScheduler.scheduleAlarm(getApplication(), updatedAlarm)
            } else {
                AlarmScheduler.cancelAlarm(getApplication(), updatedAlarm)
            }
        }
    }

    /**
     * Delete alarm
     */
    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            // Cancel alarm first
            AlarmScheduler.cancelAlarm(getApplication(), alarm)

            // Delete from database
            alarmDao.deleteAlarm(alarm)
        }
    }
}
