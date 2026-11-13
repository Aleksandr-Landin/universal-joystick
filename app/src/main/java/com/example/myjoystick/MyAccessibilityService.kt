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
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MyAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var cursorView: View? = null
    private var testButton: Button? = null

    private var cursorX = 500f
    private var cursorY = 800f
    private lateinit var cursorParams: WindowManager.LayoutParams

    override fun onServiceConnected() {
        super.onServiceConnected()

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        serviceInfo = info

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // === КУРСОР ===
        cursorParams = WindowManager.LayoutParams(
            80, 80, overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = cursorX.toInt()
            y = cursorY.toInt()
        }

        cursorView = TextView(this).apply {
            text = "➤"
            textSize = 36f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(180, 255, 50, 50))
            setTextColor(Color.WHITE)
        }
        windowManager?.addView(cursorView, cursorParams)

        // === ПРОСТАЯ КНОПКА ДЛЯ ТЕСТА ===
        testButton = Button(this).apply {
            text = "ТЕСТ КЛИК"
            textSize = 20f
            setBackgroundColor(Color.argb(255, 0, 200, 0)) // Ярко-зелёная
            setTextColor(Color.WHITE)
            setPadding(30, 20, 30, 20)
            
            // ВАЖНО: явно устанавливаем кликабельность
            isClickable = true
            isFocusable = true
            isFocusableInTouchMode = true
            
            setOnClickListener {
                Toast.makeText(this@MyAccessibilityService, 
                    "✅ КНОПКА РАБОТАЕТ!", Toast.LENGTH_LONG).show()
            }
            
            // Визуальная обратная связь
            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.setBackgroundColor(Color.argb(255, 0, 150, 0))
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.setBackgroundColor(Color.argb(255, 0, 200, 0))
                        false
                    }
                }
                false
            }
        }

        val buttonParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            // ВАЖНО: только эти два флага
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = 100
            y = 500
        }

        windowManager?.addView(testButton, buttonParams)

        Toast.makeText(this, "✅ Сервис запущен! Нажми зелёную кнопку", Toast.LENGTH_LONG).show()
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        try {
            windowManager?.removeView(cursorView)
            windowManager?.removeView(testButton)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
}
