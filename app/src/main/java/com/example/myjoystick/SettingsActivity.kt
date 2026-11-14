package com.example.myjoystick

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

        val tvWidth = TextView(this).apply { text = "Ширина пульта (в dp):"; textSize = 18f }
        val etWidth = EditText(this).apply { 
            hint = "Например, 250"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(SettingsManager.getWidthDp(this@SettingsActivity).toString())
        }

        val tvHeight = TextView(this).apply { text = "Высота пульта (в dp):"; textSize = 18f; setPadding(0, 30, 0, 0) }
        val etHeight = EditText(this).apply { 
            hint = "Например, 350"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(SettingsManager.getHeightDp(this@SettingsActivity).toString())
        }

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

        val btnSave = Button(this).apply { 
            text = "Сохранить и применить"
            setPadding(0, 50, 0, 0)
        }

        layout.addView(tvWidth)
        layout.addView(etWidth)
        layout.addView(tvHeight)
        layout.addView(etHeight)
        layout.addView(tvAlpha)
        layout.addView(seekBar)
        layout.addView(btnSave)
        scrollView.addView(layout)
        setContentView(scrollView)

        btnSave.setOnClickListener {
            val width = etWidth.text.toString().toIntOrNull() ?: 250
            val height = etHeight.text.toString().toIntOrNull() ?: 350
            val alpha = seekBar.progress

            SettingsManager.saveSettings(this, width, height, alpha)
            
            Toast.makeText(this, "Настройки сохранены! Перезапусти сервис.", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
