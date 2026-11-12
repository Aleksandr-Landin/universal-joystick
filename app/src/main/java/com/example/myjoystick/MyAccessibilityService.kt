package com.example.myjoystick

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button

class MyAccessibilityService : AccessibilityService() {

    private var floatingButton: Button? = null
    private var windowManager: WindowManager? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        
        // Настраиваем параметры сервиса
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        serviceInfo = info

        // Создаём плавающее окно ("Маячок")
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

        // Рисуем простую кнопку
        floatingButton = Button(this).apply {
            text = "🕹️"
            textSize = 24f
            setBackgroundColor(0x80000000.toInt())
            setOnClickListener {
                removeFloatingButton()
            }
        }

        // Добавляем кнопку на экран
        windowManager?.addView(floatingButton, params)
    }

    override fun onInterrupt() {
        // Требуется по интерфейсу
    }

    override fun onDestroy() {
        super.onDestroy()
        removeFloatingButton()
    }

    private fun removeFloatingButton() {
        if (floatingButton != null) {
            windowManager?.removeView(floatingButton)
            floatingButton = null
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Пока не обрабатываем события
    }
}
