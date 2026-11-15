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

        // 1. Ширина пульта
        val tvWidth = TextView(this).apply { text = "Ширина пульта (в dp):"; textSize = 18f }
        val etWidth = EditText(this).apply {
            hint = "Например, 250"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(SettingsManager.getWidthPx(this@SettingsActivity).toString()) // Показываем в dp для удобства
            // Небольшой хак: переводим пиксели обратно в dp для отображения, но для простоты оставим как есть или используем вспомогательную функцию. 
            // Чтобы не усложнять, просто покажем дефолт, если пусто, но лучше читать из менеджера.
            // Упростим: прочитаем dp напрямую, добавив метод в менеджер, но пока оставим так, это не критично.
        }
        // Исправление для корректного отображения dp в поле:
        val widthDp = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics).let { 1 } // заглушка, ниже правильный способ
        // Правильный способ получить dp из настроек (добавим в менеджер, но пока просто парсим)
        // Для простоты оставим поля пустыми при первом запуске, но подтянем сохраненные.
        
        // Перезапишем логику полей, чтобы было чисто:
        val prefs = getSharedPreferences("joystick_prefs", MODE_PRIVATE)
        etWidth.setText(prefs.getInt("panel_width", 250).toString())
        
        val tvTrackpad = TextView(this).apply { text = "Размер тачпада (сторона квадрата, в dp):"; textSize = 18f; setPadding(0, 30, 0, 0) }
        val etTrackpad = EditText(this).apply {
            hint = "Например, 150"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt("trackpad_size", 150).toString())
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
        layout.addView(tvTrackpad)
        layout.addView(etTrackpad)
        layout.addView(tvAlpha)
        layout.addView(seekBar)
        layout.addView(btnSave)
        scrollView.addView(layout)
        setContentView(scrollView)

        btnSave.setOnClickListener {
            val width = etWidth.text.toString().toIntOrNull() ?: 250
            val trackpad = etTrackpad.text.toString().toIntOrNull() ?: 150
            val alpha = seekBar.progress

            SettingsManager.saveSettings(this, width, trackpad, alpha)
            sendBroadcast(Intent(SettingsManager.ACTION_REBUILD))

            Toast.makeText(this, "Настройки применены! 🚀", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
