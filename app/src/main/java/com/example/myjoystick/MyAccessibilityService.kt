package com.example.myjoystick

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.hypot

class MyAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var controlPanelRoot: LinearLayout? = null
    private var cursorView: View? = null
    private var triggerButton: View? = null

    private var cursorX = 540f
    private var cursorY = 1200f
    private lateinit var cursorParams: WindowManager.LayoutParams
    private lateinit var rootParams: WindowManager.LayoutParams
    private var overlayType = 0

    private val rebuildReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == SettingsManager.ACTION_REBUILD) rebuildPanel()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // === 1. КУРСОР ===
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

        // === 2. ФИКСИРОВАННАЯ ПОЗИЦИЯ ПАНЕЛИ (тачпад не падает при сворачивании) ===
        val dm = resources.displayMetrics
        val screenHeight = dm.heightPixels
        val tH = SettingsManager.getTrackpadH(this)
        // Резервируем место под кнопки (примерно 200dp), чтобы в развёрнутом виде панель была у нижнего края
        val fixedTopY = (screenHeight - tH - 200 - 40).coerceAtLeast(0)

        rootParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 20
            y = fixedTopY
        }

        buildPanel()

        // === 3. ТРИГГЕР (иконка 🕹️) ===
        triggerButton = TextView(this).apply {
            text = "🕹️"
            textSize = 28f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(200, 0, 150, 255))
            setTextColor(Color.WHITE)
            isClickable = true
            setOnClickListener { toggleRootVisibility() }
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

        val filter = IntentFilter(SettingsManager.ACTION_REBUILD)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(rebuildReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(rebuildReceiver, filter)
        }

        Toast.makeText(this, "✅ Пульт готов!", Toast.LENGTH_SHORT).show()
    }

    private fun buildPanel() {
        val alpha = SettingsManager.getAlpha(this)
        val tW = SettingsManager.getTrackpadW(this)
        val tH = SettingsManager.getTrackpadH(this)
        val bW = SettingsManager.getButtonsW(this)
        val isExpanded = SettingsManager.isExpanded(this)

        controlPanelRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.argb(230, 20, 20, 20))
            setPadding(15, 15, 15, 15)
            this.alpha = alpha
            visibility = View.VISIBLE
        }

        // === 4. ТАЧПАД ===
        val trackpad = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(tW, tH).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = 10
            }
            val border = GradientDrawable().apply {
                setColor(Color.argb(255, 50, 50, 50))
                setStroke(4, Color.argb(255, 100, 100, 100))
            }
            background = border

            val hint = TextView(this@MyAccessibilityService).apply {
                text = "ВОДИ = курсор\nТАП = выбор\nДЕРЖИ = меню"
                gravity = Gravity.CENTER
                setTextColor(Color.argb(150, 255, 255, 255))
                textSize = 13f
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                isClickable = false; isFocusable = false; isEnabled = false
            }
            addView(hint)

            val toggleBtn = TextView(this@MyAccessibilityService).apply {
                text = if (isExpanded) "🔼" else "🔽"
                textSize = 20f
                gravity = Gravity.CENTER
                setBackgroundColor(Color.argb(180, 100, 100, 100))
                setTextColor(Color.WHITE)
                layoutParams = FrameLayout.LayoutParams(80, 80).apply {
                    gravity = Gravity.BOTTOM or Gravity.END
                    setMargins(0, 0, 10, 10)
                }
                isClickable = true
                setOnClickListener {
                    SettingsManager.saveExpanded(this@MyAccessibilityService, !isExpanded)
                    rebuildPanel()
                }
            }
            addView(toggleBtn)
            isClickable = true; isFocusable = true; isFocusableInTouchMode = true
        }

        // === 5. ЛОГИКА УМНОГО ТАЧПАДА ===
        var touchStartTime = 0L
        var lastTouchX = 0f
        var lastTouchY = 0f
        var isDragging = false
        var longPressFired = false
        val longPressHandler = Handler(Looper.getMainLooper())
        val longPressRunnable = Runnable {
            if (!isDragging) {
                longPressFired = true
                performLongPress(cursorX + 40f, cursorY + 40f)
                Toast.makeText(this@MyAccessibilityService, "📋 Меню", Toast.LENGTH_SHORT).show()
            }
        }

        trackpad.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.x
                    lastTouchY = event.y
                    touchStartTime = System.currentTimeMillis()
                    isDragging = false
                    longPressFired = false
                    longPressHandler.postDelayed(longPressRunnable, 600L) // 600 мс для долгого нажатия
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    // Если сдвинули больше чем на 20 пикселей, считаем это драгом
                    if (!isDragging && hypot(dx.toDouble(), dy.toDouble()) > 20.0) {
                        isDragging = true
                        longPressHandler.removeCallbacks(longPressRunnable)
                    }
                    if (isDragging) {
                        cursorX += dx * 2.0f
                        cursorY += dy * 2.0f
                        cursorX = cursorX.coerceIn(0f, 1080f)
                        cursorY = cursorY.coerceIn(0f, 3000f)
                        cursorParams.x = cursorX.toInt()
                        cursorParams.y = cursorY.toInt()
                        windowManager?.updateViewLayout(cursorView, cursorParams)
                    }
                    lastTouchX = event.x
                    lastTouchY = event.y
                    true
                }
                MotionEvent.ACTION_UP -> {
                    longPressHandler.removeCallbacks(longPressRunnable)
                    val duration = System.currentTimeMillis() - touchStartTime
                    // Если не драг, не сработал долгий тап, и держали меньше 300 мс — это короткий тап
                    if (!isDragging && !longPressFired && duration < 300L) {
                        performTap(cursorX + 40f, cursorY + 40f)
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    longPressHandler.removeCallbacks(longPressRunnable)
                    true
                }
                else -> false
            }
        }
        controlPanelRoot?.addView(trackpad)

        // === 6. ПАНЕЛЬ КНОПОК (Сворачиваемая, БЕЗ ПУСТОТЫ) ===
        val buttonPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            // WRAP_CONTENT по высоте убирает пустоту!
            layoutParams = LinearLayout.LayoutParams(bW, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            visibility = if (isExpanded) View.VISIBLE else View.GONE
        }

        // Ряд 1 (фиксированная высота 150dp для удобства пальца)
        val row1 = LinearLayout(this).apply { 
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 150)
        }
        row1.addView(createBtn("⬅️", Color.argb(255, 60, 60, 60)) { performSwipe(400, 0) })
        row1.addView(createBtn("➡️", Color.argb(255, 60, 60, 60)) { performSwipe(-400, 0) })
        row1.addView(createBtn("➖", Color.argb(255, 60, 60, 60)) { performPinch(false) })
        row1.addView(createBtn("➕", Color.argb(255, 60, 60, 60)) { performPinch(true) })
        buttonPanel.addView(row1)

        // Ряд 2 (фиксированная высота 150dp)
        val row2 = LinearLayout(this).apply { 
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 150)
        }
        row2.addView(createBtn("⬆️", Color.argb(255, 60, 60, 60)) { performSwipe(0, 500) })
        row2.addView(createBtn("⬇️", Color.argb(255, 60, 60, 60)) { performSwipe(0, -500) })
        row2.addView(createBtn("🏠", Color.argb(255, 100, 100, 0)) { teleportCursor(540f, 1200f) })
        row2.addView(createBtn("⬆⬆", Color.argb(255, 100, 0, 100)) { teleportCursor(540f, 80f) })
        // Кнопка "👆 Тап" УБРАНА отсюда, она теперь на тачпаде!
        buttonPanel.addView(row2)

        controlPanelRoot?.addView(buttonPanel)
        controlPanelRoot?.addView(createBtn("❌ Скрыть", Color.argb(255, 150, 50, 50)) { toggleRootVisibility() })

        windowManager?.addView(controlPanelRoot, rootParams)
    }

    private fun rebuildPanel() {
        try { controlPanelRoot?.let { windowManager?.removeView(it) } } catch (e: Exception) {}
        buildPanel()
        Toast.makeText(this, "🔄 Пульт перестроен!", Toast.LENGTH_SHORT).show()
    }

    private fun createBtn(text: String, bgColor: Int, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 18f
            gravity = Gravity.CENTER
            setBackgroundColor(bgColor)
            setTextColor(Color.WHITE)
            setPadding(10, 10, 10, 10)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                setMargins(5, 5, 5, 5)
            }
            isClickable = true; isFocusable = true; isFocusableInTouchMode = true
            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> { v.setBackgroundColor(Color.argb(255, 150, 150, 150)); true }
                    MotionEvent.ACTION_UP -> { v.setBackgroundColor(bgColor); onClick(); false }
                    MotionEvent.ACTION_CANCEL -> { v.setBackgroundColor(bgColor); false }
                    else -> false
                }
            }
        }
    }

    private fun toggleRootVisibility() {
        val isVisible = controlPanelRoot?.visibility == View.VISIBLE
        controlPanelRoot?.visibility = if (isVisible) View.GONE else View.VISIBLE
        triggerButton?.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    private fun teleportCursor(x: Float, y: Float) {
        cursorX = x; cursorY = y
        cursorParams.x = x.toInt(); cursorParams.y = y.toInt()
        windowManager?.updateViewLayout(cursorView, cursorParams)
        Toast.makeText(this, "📍 Курсор перемещён!", Toast.LENGTH_SHORT).show()
    }

    private fun performTap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 100)).build(), object : GestureResultCallback() {
            override fun onCompleted(gesture: GestureDescription?) { Toast.makeText(this@MyAccessibilityService, "✅ Тап!", Toast.LENGTH_SHORT).show() }
            override fun onCancelled(gesture: GestureDescription?) {}
        }, null)
    }

    private fun performLongPress(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 1000)).build(), object : GestureResultCallback() {
            override fun onCompleted(gesture: GestureDescription?) { Toast.makeText(this@MyAccessibilityService, "✅ Долгий тап!", Toast.LENGTH_SHORT).show() }
            override fun onCancelled(gesture: GestureDescription?) {}
        }, null)
    }

    private fun performSwipe(dx: Int, dy: Int) {
        val path = Path().apply { moveTo(540f, 1200f); lineTo(540f + dx, 1200f + dy) }
        dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 300)).build(), object : GestureResultCallback() {
            override fun onCompleted(gesture: GestureDescription?) { Toast.makeText(this@MyAccessibilityService, "✅ Скролл!", Toast.LENGTH_SHORT).show() }
            override fun onCancelled(gesture: GestureDescription?) {}
        }, null)
    }

    private fun performPinch(zoomIn: Boolean) {
        val cx = cursorX + 40f; val cy = cursorY + 40f
        val p1 = Path(); val p2 = Path()
        if (zoomIn) { p1.moveTo(cx - 50, cy); p1.lineTo(cx - 250, cy); p2.moveTo(cx + 50, cy); p2.lineTo(cx + 250, cy) }
        else { p1.moveTo(cx - 250, cy); p1.lineTo(cx - 50, cy); p2.moveTo(cx + 250, cy); p2.lineTo(cx + 50, cy) }
        dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p1, 0, 300)).addStroke(GestureDescription.StrokeDescription(p2, 0, 300)).build(), object : GestureResultCallback() {
            override fun onCompleted(gesture: GestureDescription?) { Toast.makeText(this@MyAccessibilityService, if (zoomIn) "🔍 Увеличено!" else "🔎 Уменьшено!", Toast.LENGTH_SHORT).show() }
            override fun onCancelled(gesture: GestureDescription?) {}
        }, null)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(rebuildReceiver) } catch (e: Exception) {}
        try {
            windowManager?.removeView(triggerButton)
            windowManager?.removeView(controlPanelRoot)
            windowManager?.removeView(cursorView)
        } catch (e: Exception) {}
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
}
