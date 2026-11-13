package com.example.myjoystick

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.Toast

class MyAccessibilityService : AccessibilityService() {

    private var floatingButton: Button? = null
    private var windowManager: WindowManager? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        
        // 1. Настраиваем параметры сервиса
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        serviceInfo = info

        // 2. ПРОВЕРКА: Есть ли права на оверлей?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                // Если прав нет, мы НЕ рисуем кнопку, чтобы приложение не упало
                return 
            }
        }

        // 3. Создаём плавающее окно ("Маячок")
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.BOTTOM
            x = 50
            y = 200
        }

        // 4. Рисуем кнопку
        floatingButton = Button(this).apply {
            text = "🕹️"
            textSize = 24f
            setBackgroundColor(0x80000000.toInt())
            setOnClickListener {
                Toast.makeText(this@MyAccessibilityService, "Джойстик работает!", Toast.LENGTH_SHORT).show()
            }
        }

        // 5. Добавляем кнопку на экран
        try {
            windowManager?.addView(floatingButton, params)
        } catch (e: Exception) {
            // Если вдруг всё равно упадёт, мы это перехватим
            e.printStackTrace()
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        if (floatingButton != null) {
            windowManager?.removeView(floatingButton)
            floatingButton = null
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Пока пусто
    }
}
