package com.example.myjoystick

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textView = findViewById<TextView>(R.id.textView)
        val button = findViewById<Button>(R.id.button)

        // Проверяем, есть ли права на отображение поверх других окон
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                textView.text = "Привет, Саша! 👋\n\nДля работы джойстика нужно разрешение 'Поверх других приложений'. Нажми кнопку ниже."
                button.text = "Дать разрешение"
                
                button.setOnClickListener {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                }
                return // Прерываем выполнение, чтобы не крашнулось
            }
        }

        // Если права есть, предлагаем включить сервис
        textView.text = "Привет, Саша! 👋\n\nРазрешения получены. Теперь включи сервис в настройках."
        button.text = "Открыть настройки"
        
        button.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
    }
}
