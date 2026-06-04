package com.medha.app.ui

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.medha.app.BuildConfig
import com.medha.app.R
import com.medha.app.data.Prefs
import com.medha.app.databinding.ActivitySettingsBinding
import com.medha.app.firebase.UserRepository
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: Prefs
    private val repository = UserRepository()

    private val modeValues = listOf("always", "screen_off", "busy", "schedule")
    private val languageValues = listOf("bengali", "english", "mixed")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = Prefs(this)

        binding.toolbar.setNavigationOnClickListener { finish() }

        // Response delay
        binding.sliderDelay.value = prefs.responseDelay.toFloat()
        binding.tvDelayValue.text = prefs.responseDelay.toString()
        binding.sliderDelay.addOnChangeListener { _, value, _ ->
            binding.tvDelayValue.text = value.toInt().toString()
            prefs.responseDelay = value.toInt()
        }

        // Mode spinner
        binding.spinnerMode.adapter = ArrayAdapter.createFromResource(
            this, R.array.auto_reply_modes, android.R.layout.simple_spinner_dropdown_item
        )
        binding.spinnerMode.setSelection(modeValues.indexOf(prefs.autoReplyMode).coerceAtLeast(0))
        binding.spinnerMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                prefs.autoReplyMode = modeValues[pos]
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        // Language spinner
        binding.spinnerLanguage.adapter = ArrayAdapter.createFromResource(
            this, R.array.languages, android.R.layout.simple_spinner_dropdown_item
        )
        binding.spinnerLanguage.setSelection(languageValues.indexOf(prefs.language).coerceAtLeast(0))
        binding.spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                prefs.language = languageValues[pos]
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        // Approval toggle
        binding.swApproval.isChecked = prefs.requireApproval
        binding.swApproval.setOnCheckedChangeListener { _, c -> prefs.requireApproval = c }

        // TTS toggle
        binding.swTts.isChecked = prefs.ttsEnabled
        binding.swTts.setOnCheckedChangeListener { _, c -> prefs.ttsEnabled = c }

        // Schedule
        binding.swSchedule.isChecked = prefs.scheduleEnabled
        binding.swSchedule.setOnCheckedChangeListener { _, c ->
            prefs.scheduleEnabled = c
            updateScheduleVisibility(c)
        }
        updateScheduleVisibility(prefs.scheduleEnabled)

        binding.sliderStart.value = prefs.scheduleStartHour.toFloat()
        binding.tvStartValue.text = formatHour(prefs.scheduleStartHour)
        binding.sliderStart.addOnChangeListener { _, value, _ ->
            prefs.scheduleStartHour = value.toInt()
            binding.tvStartValue.text = formatHour(value.toInt())
        }

        binding.sliderEnd.value = prefs.scheduleEndHour.toFloat()
        binding.tvEndValue.text = formatHour(prefs.scheduleEndHour)
        binding.sliderEnd.addOnChangeListener { _, value, _ ->
            prefs.scheduleEndHour = value.toInt()
            binding.tvEndValue.text = formatHour(value.toInt())
        }

        binding.tvVersion.text = getString(R.string.settings_version) + ": " + BuildConfig.VERSION_NAME
    }

    override fun onPause() {
        super.onPause()
        // Persist the user's config to Firestore as a backup.
        val config = mapOf(
            "responseDelay" to prefs.responseDelay,
            "autoReplyMode" to prefs.autoReplyMode,
            "language" to prefs.language,
            "requireApproval" to prefs.requireApproval,
            "scheduleEnabled" to prefs.scheduleEnabled,
            "scheduleStartHour" to prefs.scheduleStartHour,
            "scheduleEndHour" to prefs.scheduleEndHour
        )
        lifecycleScope.launch { repository.saveConfig(config) }
    }

    private fun updateScheduleVisibility(visible: Boolean) {
        binding.scheduleGroup.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun formatHour(hour: Int): String = String.format("%02d:00", hour)
}
