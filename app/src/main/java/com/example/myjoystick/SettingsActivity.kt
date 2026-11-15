package com.example.myjoystick

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
        }

        val prefs = getSharedPreferences("joystick_prefs", MODE_PRIVATE)

        // 1. Ширина пульта
        val tvWidth = TextView(this).apply { text = "Ширина пульта (в dp):"; textSize = 18f }
        val etWidth = EditText(this).apply {
            hint = "Например, 250"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt("panel_width", 250).toString())
        }

        // 2. Высота тачпада
        val tvTrackpad = TextView(this).apply { text = "Высота тачпада (в dp):"; textSize = 18f; setPadding(0, 30, 0, 0) }
        val etTrackpad = EditText(this).apply {
            hint = "Например, 150"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt("trackpad_height", 150).toString())
        }

        // 3. Высота кнопки
        val tvButton = TextView(this).apply { text = "Высота одной кнопки (в dp):"; textSize = 18f; setPadding(0, 30, 0, 0) }
        val etButton = EditText(this).apply {
            hint = "Например, 50"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt("button_height", 50).toString())
        }

        // 4. Прозрачность
        val currentAlpha = (SettingsManager.getAlpha(this@SettingsActivity) * 100).toInt()
        val tvAlpha = TextView(this).apply { text = "Прозрачность: $currentAlpha%"; textSize = 18f; setPadding(0, 30, 0, 0) }
        val seekBar = SeekBar(this).apply { max = 100; progress = currentAlpha }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvAlpha.text = "Прозрачность: $progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 5. Кнопка Сохранить
        val btnSave = Button(this).apply {
            text = "Сохранить и применить"
            setPadding(0, 50, 0, 0)
        }

        layout.addView(tvWidth)
        layout.addView(etWidth)
        layout.addView(tvTrackpad)
        layout.addView(etTrackpad)
        layout.addView(tvButton)
        layout.addView(etButton)
        layout.addView(tvAlpha)
        layout.addView(seekBar)
        layout.addView(btnSave)
        scrollView.addView(layout)
        setContentView(scrollView)

        btnSave.setOnClickListener {
            val width = etWidth.text.toString().toIntOrNull() ?: 250
            val trackpad = etTrackpad.text.toString().toIntOrNull() ?: 150
            val button = etButton.text.toString().toIntOrNull() ?: 50
            val alpha = seekBar.progress

            SettingsManager.saveSettings(this, width, trackpad, button, alpha)
            sendBroadcast(Intent(SettingsManager.ACTION_REBUILD))

            Toast.makeText(this, "Настройки применены! 🚀", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
