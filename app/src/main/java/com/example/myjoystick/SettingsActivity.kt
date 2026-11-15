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

        // 1. Ширина тачпада
        val tvTrackpadWidth = TextView(this).apply { text = "Ширина тачпада (в dp):"; textSize = 18f }
        val etTrackpadWidth = EditText(this).apply {
            hint = "Например, 250"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt("trackpad_width", 250).toString())
        }

        // 2. Высота тачпада
        val tvTrackpadHeight = TextView(this).apply { text = "Высота тачпада (в dp):"; textSize = 18f; setPadding(0, 30, 0, 0) }
        val etTrackpadHeight = EditText(this).apply {
            hint = "Например, 150"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt("trackpad_height", 150).toString())
        }

        // 3. Ширина панели с кнопками
        val tvButtonPanelWidth = TextView(this).apply { text = "Ширина панели с кнопками (в dp):"; textSize = 18f; setPadding(0, 30, 0, 0) }
        val etButtonPanelWidth = EditText(this).apply {
            hint = "Например, 250"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt("button_panel_width", 250).toString())
        }

        // 4. Высота панели с кнопками
        val tvButtonPanelHeight = TextView(this).apply { text = "Высота панели с кнопками (в dp):"; textSize = 18f; setPadding(0, 30, 0, 0) }
        val etButtonPanelHeight = EditText(this).apply {
            hint = "Например, 200"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt("button_panel_height", 200).toString())
        }

        val btnSave = Button(this).apply {
            text = "Сохранить и применить"
            setPadding(0, 50, 0, 0)
        }

        layout.addView(tvTrackpadWidth)
        layout.addView(etTrackpadWidth)
        layout.addView(tvTrackpadHeight)
        layout.addView(etTrackpadHeight)
        layout.addView(tvButtonPanelWidth)
        layout.addView(etButtonPanelWidth)
        layout.addView(tvButtonPanelHeight)
        layout.addView(etButtonPanelHeight)
        layout.addView(btnSave)

        scrollView.addView(layout)
        setContentView(scrollView)

        btnSave.setOnClickListener {
            val trackpadWidth = etTrackpadWidth.text.toString().toIntOrNull() ?: 250
            val trackpadHeight = etTrackpadHeight.text.toString().toIntOrNull() ?: 150
            val buttonPanelWidth = etButtonPanelWidth.text.toString().toIntOrNull() ?: 250
            val buttonPanelHeight = etButtonPanelHeight.text.toString().toIntOrNull() ?: 200

            SettingsManager.saveSettings(this, trackpadWidth, trackpadHeight, buttonPanelWidth, buttonPanelHeight)
            sendBroadcast(Intent(SettingsManager.ACTION_REBUILD))
            Toast.makeText(this, "Настройки применены! 🚀", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
