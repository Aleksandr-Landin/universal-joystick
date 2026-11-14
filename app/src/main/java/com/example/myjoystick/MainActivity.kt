package com.example.myjoystick

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
            gravity = android.view.Gravity.CENTER
        }

        val textView = TextView(this).apply {
            text = "Привет, Сань! 👋\n\nДобро пожаловать в Universal Joystick."
            textSize = 18f
            setPadding(0, 0, 0, 40)
            gravity = android.view.Gravity.CENTER
        }

        val btnAccessibility = Button(this).apply {
            text = "1. Включить сервис специальных возможностей"
            setPadding(0, 20, 0, 20)
        }

        val btnSettings = Button(this).apply {
            text = "2. Настройки размера и прозрачности"
            setPadding(0, 20, 0, 20)
        }

        layout.addView(textView)
        layout.addView(btnAccessibility)
        layout.addView(btnSettings)
        setContentView(layout)

        btnAccessibility.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Сначала дай разрешение 'Поверх других приложений'", Toast.LENGTH_LONG).show()
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    startActivity(intent)
                    return@setOnClickListener
                }
            }
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }
}
