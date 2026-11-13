package com.example.myjoystick

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MyAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var triggerButton: View? = null
    private var controlPanel: LinearLayout? = null
    private var cursorView: View? = null

    // Координаты курсора
    private var cursorX = 500f
    private var cursorY = 800f

    override fun onServiceConnected() {
        super.onServiceConnected()
        Toast.makeText(this, "🕹️ Сервис запущен!", Toast.LENGTH_LONG).show()

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

        // === 1. КУРСОР (Прицел) ===
        val cursorParams = WindowManager.LayoutParams(
            80, 80, overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or // Курсор прозрачен для кликов!
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

        // === 2. ПАНЕЛЬ УПРАВЛЕНИЯ (Закреплённая) ===
        controlPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.argb(230, 20, 20, 20)) // Почти чёрный фон
            setPadding(15, 15, 15, 15)
        }

        val panelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = 20
            y = 200
        }

        // --- ЗОНА ТАЧПАДА ---
        val trackpad = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(250, 250).apply { 
                bottomMargin = 20 
            }
            setBackgroundColor(Color.argb(255, 40, 40, 40)) // Тёмно-серый фон
            
            // Рисуем рамку, чтобы было видно границы тачпада
            val border = GradientDrawable()
            border.setColor(Color.argb(255, 50, 50, 50))
            border.setStroke(4, Color.argb(255, 100, 100, 100))
            background = border
            
            // Добавим текст-подсказку
            val hint = TextView(this@MyAccessibilityService).apply {
                text = "ВОДИ ЗДЕСЬ\nдля курсора"
                gravity = Gravity.CENTER
                setTextColor(Color.argb(150, 255, 255, 255))
                textSize = 14f
            }
            (this as LinearLayout).addView(hint, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.MATCH_PARENT
            ))
        }

        // Логика тачпада
        var lastTouchX = 0f
        var lastTouchY = 0f
        val sensitivity = 2.0f // Курсор двигается в 2 раза быстрее пальца

        trackpad.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.x
                    lastTouchY = event.y
                    true // Забираем событие себе
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    
                    cursorX += dx * sensitivity
                    cursorY += dy * sensitivity
                    
                    // Ограничиваем курсор размерами экрана (примерно FullHD+)
                    cursorX = cursorX.coerceIn(0f, 1080f)
                    cursorY = cursorY.coerceIn(0f, 2400f)

                    cursorParams.x = cursorX.toInt()
                    cursorParams.y = cursorY.toInt()
                    windowManager?.updateViewLayout(cursorView, cursorParams)
                    
                    lastTouchX = event.x
                    lastTouchY = event.y
                    true
                }
            }
            false
        }
        controlPanel?.addView(trackpad)

        // --- РЯД 1: Свайпы и Зум ---
        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row1.addView(makeBtn("⬅️") { performSwipe(-400, 0) }, 1f)
        row1.addView(makeBtn("➡️") { performSwipe(400, 0) }, 1f)
        row1.addView(makeBtn("➖") { performPinch(false) }, 1f)
        row1.addView(makeBtn("➕") { performPinch(true) }, 1f)
        controlPanel?.addView(row1)

        // --- РЯД 2: Скролл и Главная кнопка ---
        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row2.addView(makeBtn("⬆️") { performSwipe(0, -500) }, 1f)
        row2.addView(makeBtn("⬇️") { performSwipe(0, 500) }, 1f)
        
        // Главная кнопка (Тап / Меню)
        val mainBtn = Button(this).apply {
            text = "👆"
            textSize = 20f
            setBackgroundColor(Color.argb(255, 0, 100, 200))
            setTextColor(Color.WHITE)
            setPadding(10, 20, 10, 20)
            
            setOnClickListener { performTap(cursorX + 40f, cursorY + 40f) }
            setOnLongClickListener { 
                performLongPress(cursorX + 40f, cursorY + 40f)
                true 
            }
        }
        row2.addView(mainBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f).apply {
            setMargins(10, 0, 0, 0)
        })
        controlPanel?.addView(row2)

        // --- КНОПКА СВЕРНУТЬ ---
        controlPanel?.addView(makeBtn("❌ Свернуть") { togglePanel(false) }, 1f)

        windowManager?.addView(controlPanel, panelParams)

        // === 3. ТРИГГЕР (Кнопка открытия) ===
        triggerButton = TextView(this).apply {
            text = "🕹️"
            textSize = 28f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(200, 0, 150, 255))
            setTextColor(Color.WHITE)
            setOnClickListener { togglePanel(true) }
        }

        val triggerParams = WindowManager.LayoutParams(
            100, 100, overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = 20
            y = 20
        }
        windowManager?.addView(triggerButton, triggerParams)

        Toast.makeText(this, "✅ Тачпад готов!", Toast.LENGTH_SHORT).show()
    }

    // Вспомогательная функция для создания кнопок
    private fun makeBtn(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            textSize = 18f
            setBackgroundColor(Color.argb(255, 60, 60, 60))
            setTextColor(Color.WHITE)
            setPadding(10, 20, 10, 20)
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(5, 0, 5, 10)
            }
        }
    }

    private fun togglePanel(show: Boolean) {
        controlPanel?.visibility = if (show) View.VISIBLE else View.GONE
        triggerButton?.visibility = if (show) View.GONE else View.VISIBLE
    }

    // === ЖЕСТЫ ===
    private fun performTap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        dispatchGesture(gesture, null, null)
        Toast.makeText(this, "👆 Тап!", Toast.LENGTH_SHORT).show()
    }

    private fun performLongPress(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 1000))
            .build()
        dispatchGesture(gesture, null, null)
        Toast.makeText(this, "📋 Меню!", Toast.LENGTH_SHORT).show()
    }

    private fun performSwipe(dx: Int, dy: Int) {
        val startX = 540f
        val startY = 1200f
        val path = Path().apply { moveTo(startX, startY); lineTo(startX + dx, startY + dy) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()
        dispatchGesture(gesture, null, null)
        Toast.makeText(this, "🔄 Скролл", Toast.LENGTH_SHORT).show()
    }

    private fun performPinch(zoomIn: Boolean) {
        val cx = cursorX + 40f
        val cy = cursorY + 40f
        val path1 = Path()
        val path2 = Path()

        if (zoomIn) {
            path1.moveTo(cx - 30, cy); path1.lineTo(cx - 150, cy)
            path2.moveTo(cx + 30, cy); path2.lineTo(cx + 150, cy)
            Toast.makeText(this, "🔍 Увеличить", Toast.LENGTH_SHORT).show()
        } else {
            path1.moveTo(cx - 150, cy); path1.lineTo(cx - 30, cy)
            path2.moveTo(cx + 150, cy); path2.lineTo(cx + 30, cy)
            Toast.makeText(this, "🔎 Уменьшить", Toast.LENGTH_SHORT).show()
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path1, 0, 300))
            .addStroke(GestureDescription.StrokeDescription(path2, 0, 300))
            .build()
        dispatchGesture(gesture, null, null)
    }

    override fun onInterrupt() {}
    override fun onDestroy() {
        super.onDestroy()
        try {
            windowManager?.removeView(triggerButton)
            windowManager?.removeView(controlPanel)
            windowManager?.removeView(cursorView)
        } catch (e: Exception) { e.printStackTrace() }
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
}
