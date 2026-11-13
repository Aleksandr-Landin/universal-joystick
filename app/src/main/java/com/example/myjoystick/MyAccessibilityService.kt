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

        // === КУРСОР (прицел) ===
        val cursorParams = WindowManager.LayoutParams(
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

        // === ПАНЕЛЬ УПРАВЛЕНИЯ ===
        controlPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.argb(220, 30, 30, 30))
            setPadding(10, 10, 10, 10)
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

        // Строка 1: Свайпы влево/вправо
        val rowSwipe = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        rowSwipe.addView(makePanelButton("⬅️") { performSwipe(-400, 0) }, 1f)
        rowSwipe.addView(makePanelButton("➡️") { performSwipe(400, 0) }, 1f)
        controlPanel?.addView(rowSwipe)

        // Строка 2: Скролл вверх/вниз
        val rowScroll = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        rowScroll.addView(makePanelButton("⬆️") { performSwipe(0, -500) }, 1f)
        rowScroll.addView(makePanelButton("⬇️") { performSwipe(0, 500) }, 1f)
        controlPanel?.addView(rowScroll)

        // Строка 3: ЗУМ (Увеличить/Уменьшить)
        val rowZoom = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        rowZoom.addView(makePanelButton("➖") { performPinch(false) }, 1f) // Уменьшить
        rowZoom.addView(makePanelButton("➕") { performPinch(true) }, 1f)  // Увеличить
        controlPanel?.addView(rowZoom)

        // Строка 4: Главная кнопка (Тап / Меню)
        val mainButton = Button(this).apply {
            text = "👆 Тап / Меню"
            textSize = 16f
            setBackgroundColor(Color.argb(255, 0, 100, 200))
            setTextColor(Color.WHITE)
            setPadding(10, 25, 10, 25)

            setOnClickListener {
                performTap(cursorX + 40f, cursorY + 40f)
            }
            setOnLongClickListener {
                performLongPress(cursorX + 40f, cursorY + 40f)
                true
            }
        }
        controlPanel?.addView(mainButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 10; bottomMargin = 10 })

        // Строка 5: Свернуть
        controlPanel?.addView(makePanelButton("❌ Свернуть") { togglePanel(false) }, 1f)

        windowManager?.addView(controlPanel, panelParams)

        // === ТРИГГЕР ===
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
    }

    private fun makePanelButton(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            textSize = 18f
            setBackgroundColor(Color.argb(255, 60, 60, 60))
            setTextColor(Color.WHITE)
            setPadding(10, 20, 10, 20)
            setOnClickListener { onClick() }
        }
    }

    private fun LinearLayout.addView(btn: Button, weight: Float) {
        addView(btn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight).apply {
            setMargins(5, 0, 5, 5)
        })
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

    // === НОВАЯ ФУНКЦИЯ: ЭМУЛЯЦИЯ ЩИПКА (ЗУМ) ===
    private fun performPinch(zoomIn: Boolean) {
        // Центр курсора
        val cx = cursorX + 40f
        val cy = cursorY + 40f

        val path1 = Path() // Палец 1 (левый)
        val path2 = Path() // Палец 2 (правый)

        if (zoomIn) {
            // Для увеличения: пальцы начинаются близко к центру и расходятся
            path1.moveTo(cx - 30, cy)
            path1.lineTo(cx - 150, cy)
            path2.moveTo(cx + 30, cy)
            path2.lineTo(cx + 150, cy)
            Toast.makeText(this, "🔍 Увеличить", Toast.LENGTH_SHORT).show()
        } else {
            // Для уменьшения: пальцы начинаются далеко и сходятся к центру
            path1.moveTo(cx - 150, cy)
            path1.lineTo(cx - 30, cy)
            path2.moveTo(cx + 150, cy)
            path2.lineTo(cx + 30, cy)
            Toast.makeText(this, "🔎 Уменьшить", Toast.LENGTH_SHORT).show()
        }

        // Создаём жест с ДВУМЯ одновременными касаниями
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path1, 0, 300)) // 300мс длительность
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
