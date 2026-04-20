package com.example.clockapp.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.google.android.material.materialswitch.MaterialSwitch
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.clockapp.R
import com.example.clockapp.data.model.Alarm
import com.example.clockapp.data.model.CalendarData

/**
 * Alarm list adapter
 */
class AlarmAdapter(
    private val onItemClick: (Alarm) -> Unit,
    private val onToggleChanged: (Alarm, Boolean) -> Unit,
    private val onDeleteClick: (Alarm) -> Unit,
    private val onCopyClick: (Alarm) -> Unit,
    private var calendarData: CalendarData? = null
) : ListAdapter<Alarm, AlarmAdapter.AlarmViewHolder>(AlarmDiffCallback()) {

    /**
     * Update calendar data for workday alarm calculation
     */
    fun updateCalendarData(data: CalendarData?) {
        calendarData = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        holder.bind(getItem(position), calendarData)
    }

    inner class AlarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvNextRing: TextView = itemView.findViewById(R.id.tvNextRing)
        private val switchEnable: MaterialSwitch = itemView.findViewById(R.id.switchEnable)
        private val ivDelete: ImageView = itemView.findViewById(R.id.ivDelete)
        private val ivCopy: ImageView = itemView.findViewById(R.id.ivCopy)
        private val ivTypeIcon: ImageView = itemView.findViewById(R.id.ivTypeIcon)

        fun bind(alarm: Alarm, calendarData: CalendarData?) {
            tvTime.text = alarm.getTimeString()
            tvTitle.text = alarm.title
            tvNextRing.text = alarm.getNextRingDescription(calendarData)

            // 根据闹钟类型显示不同的图标
            ivTypeIcon.setImageResource(when {
                alarm.isSpecialAlarm -> R.drawable.ic_work_outline
                alarm.isRegularAlarm -> R.drawable.ic_repeat
                else -> R.drawable.ic_calendar_today
            })

            // Remove listener to avoid loop trigger
            switchEnable.setOnCheckedChangeListener(null)
            switchEnable.isChecked = alarm.isEnabled

            // Set listener
            switchEnable.setOnCheckedChangeListener { _, isChecked ->
                onToggleChanged(alarm, isChecked)
            }

            // Click entire item to edit
            itemView.setOnClickListener {
                onItemClick(alarm)
            }

            // Copy button click
            ivCopy.setOnClickListener {
                onCopyClick(alarm)
            }

            // Delete button click
            ivDelete.setOnClickListener {
                onDeleteClick(alarm)
            }
        }
    }

    class AlarmDiffCallback : DiffUtil.ItemCallback<Alarm>() {
        override fun areItemsTheSame(oldItem: Alarm, newItem: Alarm): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Alarm, newItem: Alarm): Boolean {
            return oldItem == newItem
        }
    }
}
