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

        fun addField(label: String, key: String, defaultVal: Int): EditText {
            val tv = TextView(this).apply { text = label; textSize = 18f; setPadding(0, 20, 0, 0) }
            val et = EditText(this).apply {
                hint = "Например, $defaultVal"
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                setText(prefs.getInt(key, defaultVal).toString())
            }
            layout.addView(tv)
            layout.addView(et)
            return et
        }

        val etW = addField("Ширина пульта (dp):", "panel_width", 280)
        val etH = addField("Высота пульта (dp):", "panel_height", 400)

        val currentAlpha = (SettingsManager.getAlpha(this) * 100).toInt()
        val tvAlpha = TextView(this).apply { text = "Прозрачность: $currentAlpha%"; textSize = 18f; setPadding(0, 30, 0, 0) }
        val seekBar = SeekBar(this).apply { max = 100; progress = currentAlpha }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvAlpha.text = "Прозрачность: $progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        layout.addView(tvAlpha)
        layout.addView(seekBar)

        val btnSave = Button(this).apply {
            text = "Сохранить и применить"
            setPadding(0, 40, 0, 0)
        }
        layout.addView(btnSave)
        scrollView.addView(layout)
        setContentView(scrollView)

        btnSave.setOnClickListener {
            val w = etW.text.toString().toIntOrNull() ?: 280
            val h = etH.text.toString().toIntOrNull() ?: 400
            val alpha = seekBar.progress

            SettingsManager.saveSettings(this, w, h, alpha)
            sendBroadcast(Intent(SettingsManager.ACTION_REBUILD))

            Toast.makeText(this, "Настройки применены! 🚀", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
