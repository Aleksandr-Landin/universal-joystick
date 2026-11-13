package com.example.myjoystick

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import android.widget.Toast

class MyAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var joystick: View? = null
    private var cursor: View? = null
    private var clickButton: View? = null

    // Переменные для перетаскивания джойстика
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    // Смещение курсора относительно джойстика
    private val cursorOffsetX = -200  // Курсор слева от джойстика
    private val cursorOffsetY = -150  // Курсор выше джойстика

    override fun onServiceConnected() {
        super.onServiceConnected()
        Toast.makeText(this, "🕹️ Сервис запущен!", Toast.LENGTH_LONG).show()

        // Настройка сервиса
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        serviceInfo = info

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // 1. Параметры для ДЖОЙСТИКА
        val joystickParams = WindowManager.LayoutParams(
            150, 150,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.BOTTOM
            x = 50
            y = 300
        }

        // Создаём Джойстик
        joystick = TextView(this).apply {
            text = "🕹️"
            textSize = 40f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(200, 50, 50, 50))
            setTextColor(Color.WHITE)
        }

        // 2. Параметры для КУРСОРА (стрелочка)
        val cursorParams = WindowManager.LayoutParams(
            100, 100,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,  // Курсор пропускает клики сквозь себя!
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.BOTTOM
            x = joystickParams.x + cursorOffsetX
            y = joystickParams.y + cursorOffsetY
        }

        // Создаём Курсор (стрелочка)
        cursor = TextView(this).apply {
            text = "➤"
            textSize = 50f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(150, 255, 0, 0))  // Полупрозрачный красный
            setTextColor(Color.WHITE)
        }

        // 3. Параметры для КНОПКИ КЛИКА
        val clickButtonParams = WindowManager.LayoutParams(
            120, 120,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.BOTTOM
            x = joystickParams.x - 160
            y = joystickParams.y
        }

        // Создаём Кнопку Клика
        clickButton = TextView(this).apply {
            text = "👆"
            textSize = 35f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(200, 0, 100, 200))
            setTextColor(Color.WHITE)
        }

        // Логика перетаскивания джойстика
        joystick?.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = joystickParams.x
                    initialY = joystickParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    // Двигаем джойстик
                    joystickParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    joystickParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(joystick, joystickParams)

                    // Двигаем курсор вместе с джойстиком (с фиксированным смещением)
                    cursorParams.x = joystickParams.x + cursorOffsetX
                    cursorParams.y = joystickParams.y + cursorOffsetY
                    windowManager?.updateViewLayout(cursor, cursorParams)

                    // Двигаем кнопку клика
                    clickButtonParams.x = joystickParams.x - 160
                    clickButtonParams.y = joystickParams.y
                    windowManager?.updateViewLayout(clickButton, clickButtonParams)
                    true
                }
            }
            false
        }

        // Клик по кнопке эмулирует тап в координатах КУРСОРА
        clickButton?.setOnClickListener {
            val tapX = cursorParams.x + 50f  // Центр курсора (100/2)
            val tapY = cursorParams.y + 50f
            performTap(tapX, tapY)
        }

        // Добавляем все три элемента на экран
        try {
            windowManager?.addView(joystick, joystickParams)
            windowManager?.addView(cursor, cursorParams)
            windowManager?.addView(clickButton, clickButtonParams)
            Toast.makeText(this, "✅ Джойстик + Курсор + Клик готовы!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Функция эмуляции касания в указанных координатах
    private fun performTap(x: Float, y: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path()
            path.moveTo(x, y)
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
                .build()
            dispatchGesture(gesture, null, null)
            Toast.makeText(this, "👆 Клик в (${x.toInt()}, ${y.toInt()})", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        try {
            windowManager?.removeView(joystick)
            windowManager?.removeView(cursor)
            windowManager?.removeView(clickButton)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
}
