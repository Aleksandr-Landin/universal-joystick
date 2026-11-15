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
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MyAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var triggerButton: View? = null
    private var controlPanel: LinearLayout? = null
    private var cursorView: View? = null
    private var expandButton: View? = null  // Кнопка ⚙️ для выдвижения панели

    private var cursorX = 540f
    private var cursorY = 1200f
    private lateinit var cursorParams: WindowManager.LayoutParams

    private lateinit var panelParams: WindowManager.LayoutParams
    private var overlayType = 0
    private var isPanelExpanded = false

    private val rebuildReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == SettingsManager.ACTION_REBUILD) {
                rebuildPanel()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        serviceInfo = info

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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

        // === ПАРАМЕТРЫ ПАНЕЛИ ===
        isPanelExpanded = SettingsManager.isPanelExpanded(this)
        panelParams = WindowManager.LayoutParams(
            SettingsManager.getWidthPx(this),
            WindowManager.LayoutParams.WRAP_CONTENT,  // Высота подстраивается под содержимое
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = 20
            y = 20
        }

        buildPanel()

        // === ТРИГГЕР ===
        triggerButton = TextView(this).apply {
            text = "🕹️"
            textSize = 28f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(200, 0, 150, 255))
            setTextColor(Color.WHITE)
            isClickable = true
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

        val filter = IntentFilter(SettingsManager.ACTION_REBUILD)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(rebuildReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(rebuildReceiver, filter)
        }

        Toast.makeText(this, "✅ Пульт собран!", Toast.LENGTH_SHORT).show()
    }

    private fun buildPanel() {
        val panelAlpha = SettingsManager.getAlpha(this)
        val trackpadHeightPx = SettingsManager.getTrackpadHeightPx(this)
        val buttonHeightPx = SettingsManager.getButtonHeightPx(this)

        controlPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.argb(230, 20, 20, 20))
            setPadding(15, 15, 15, 15)
            alpha = panelAlpha
        }

        // --- ТАЧПАД + КНОПКА РАЗВЁРТЫВАНИЯ ---
        val trackpadContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                trackpadHeightPx
            ).apply { bottomMargin = 10 }

            val border = GradientDrawable()
            border.setColor(Color.argb(255, 50, 50, 50))
            border.setStroke(4, Color.argb(255, 100, 100, 100))
            background = border

            val hint = TextView(this@MyAccessibilityService).apply {
                text = "ВОДИ ЗДЕСЬ для курсора"
                gravity = Gravity.CENTER
                setTextColor(Color.argb(150, 255, 255, 255))
                textSize = 14f
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                isClickable = false
                isFocusable = false
                isEnabled = false
            }
            addView(hint)

            // Кнопка ⚙️ в правом нижнем углу тачпада
            expandButton = TextView(this@MyAccessibilityService).apply {
                text = "⚙️"
                textSize = 20f
                gravity = Gravity.CENTER
                setBackgroundColor(Color.argb(180, 100, 100, 100))
                setTextColor(Color.WHITE)
                layoutParams = FrameLayout.LayoutParams(80, 80).apply {
                    gravity = Gravity.BOTTOM or Gravity.END
                    setMargins(0, 0, 10, 10)
                }
                isClickable = true
                setOnClickListener { toggleExpand() }
            }
            addView(expandButton)

            isClickable = true
            isFocusable = true
            isFocusableInTouchMode = true
        }

        var lastTouchX = 0f
        var lastTouchY = 0f
        val sensitivity = 2.0f

        trackpadContainer.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.x
                    lastTouchY = event.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    cursorX += dx * sensitivity
                    cursorY += dy * sensitivity
                    cursorX = cursorX.coerceIn(0f, 1080f)
                    cursorY = cursorY.coerceIn(0f, 3000f)
                    cursorParams.x = cursorX.toInt()
                    cursorParams.y = cursorY.toInt()
                    windowManager?.updateViewLayout(cursorView, cursorParams)
                    lastTouchX = event.x
                    lastTouchY = event.y
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    true
                }
            }
            false
        }
        controlPanel?.addView(trackpadContainer)

        // --- ПАНЕЛЬ КНОПОК (выдвижная) ---
        if (isPanelExpanded) {
            val buttonPanel = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                visibility = View.VISIBLE
            }

            // Ряд 1: Свайпы
            val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row1.addView(createBtn("⬅️", Color.argb(255, 60, 60, 60), buttonHeightPx) { performSwipe(400, 0) })
            row1.addView(createBtn("➡️", Color.argb(255, 60, 60, 60), buttonHeightPx) { performSwipe(-400, 0) })
            row1.addView(createBtn("➖", Color.argb(255, 60, 60, 60), buttonHeightPx) { performPinch(false) })
            row1.addView(createBtn("➕", Color.argb(255, 60, 60, 60), buttonHeightPx) { performPinch(true) })
            buttonPanel.addView(row1)

            // Ряд 2: Скролл + Телепорт + Тап
            val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row2.addView(createBtn("⬆️", Color.argb(255, 60, 60, 60), buttonHeightPx) { performSwipe(0, 500) })
            row2.addView(createBtn("⬇️", Color.argb(255, 60, 60, 60), buttonHeightPx) { performSwipe(0, -500) })
            row2.addView(createBtn("🏠", Color.argb(255, 100, 100, 0), buttonHeightPx) { teleportCursor(540f, 1200f) })
            row2.addView(createBtn("⬆⬆", Color.argb(255, 100, 0, 100), buttonHeightPx) { teleportCursor(540f, 80f) })

            val mainBtn = createBtn("👆 Тап", Color.argb(255, 0, 100, 200), buttonHeightPx) {
                performTap(cursorX + 40f, cursorY + 40f)
            }
            mainBtn.setOnLongClickListener {
                performLongPress(cursorX + 40f, cursorY + 40f)
                true
            }
            row2.addView(mainBtn)
            buttonPanel.addView(row2)

            // Кнопка свернуть
            buttonPanel.addView(createBtn("❌ Свернуть", Color.argb(255, 150, 50, 50), buttonHeightPx) { toggleExpand() })

            controlPanel?.addView(buttonPanel)
        }

        windowManager?.addView(controlPanel, panelParams)
    }

    private fun toggleExpand() {
        isPanelExpanded = !isPanelExpanded
        SettingsManager.saveExpandedState(this, isPanelExpanded)
        rebuildPanel()
    }

    private fun rebuildPanel() {
        try {
            controlPanel?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        panelParams.width = SettingsManager.getWidthPx(this)
        buildPanel()
        Toast.makeText(this, "🔄 Пульт перестроен!", Toast.LENGTH_SHORT).show()
    }

    private fun createBtn(text: String, bgColor: Int, heightPx: Int, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 18f
            gravity = Gravity.CENTER
            setBackgroundColor(bgColor)
            setTextColor(Color.WHITE)
            setPadding(15, 20, 15, 20)
            layoutParams = LinearLayout.LayoutParams(0, heightPx, 1f).apply {
                setMargins(5, 5, 5, 5)
            }
            isClickable = true
            isFocusable = true
            isFocusableInTouchMode = true
            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.setBackgroundColor(Color.argb(255, 150, 150, 150))
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        v.setBackgroundColor(bgColor)
                        onClick()
                        false
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        v.setBackgroundColor(bgColor)
                        false
                    }
                }
                false
            }
            setOnClickListener { onClick() }
        }
    }

    private fun teleportCursor(x: Float, y: Float) {
        cursorX = x
        cursorY = y
        cursorParams.x = x.toInt()
        cursorParams.y = y.toInt()
        windowManager?.updateViewLayout(cursorView, cursorParams)
        Toast.makeText(this, "📍 Курсор перемещён!", Toast.LENGTH_SHORT).show()
    }

    private fun togglePanel(show: Boolean) {
        controlPanel?.visibility = if (show) View.VISIBLE else View.GONE
        triggerButton?.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun performTap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gesture: GestureDescription?) {
                Toast.makeText(this@MyAccessibilityService, "✅ Тап!", Toast.LENGTH_SHORT).show()
            }
            override fun onCancelled(gesture: GestureDescription?) {
                Toast.makeText(this@MyAccessibilityService, "❌ Тап не сработал", Toast.LENGTH_SHORT).show()
            }
        }, null)
    }

    private fun performLongPress(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 1000))
            .build()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gesture: GestureDescription?) {
                Toast.makeText(this@MyAccessibilityService, "✅ Меню!", Toast.LENGTH_SHORT).show()
            }
            override fun onCancelled(gesture: GestureDescription?) {
                Toast.makeText(this@MyAccessibilityService, "❌ Меню не сработало", Toast.LENGTH_SHORT).show()
            }
        }, null)
    }

    private fun performSwipe(dx: Int, dy: Int) {
        val startX = 540f; val startY = 1200f
        val path = Path().apply { moveTo(startX, startY); lineTo(startX + dx, startY + dy) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gesture: GestureDescription?) {
                Toast.makeText(this@MyAccessibilityService, "✅ Скролл!", Toast.LENGTH_SHORT).show()
            }
            override fun onCancelled(gesture: GestureDescription?) {
                Toast.makeText(this@MyAccessibilityService, "❌ Скролл не сработал", Toast.LENGTH_SHORT).show()
            }
        }, null)
    }

    private fun performPinch(zoomIn: Boolean) {
        val cx = cursorX + 40f
        val cy = cursorY + 40f
        val path1 = Path()
        val path2 = Path()

        if (zoomIn) {
            path1.moveTo(cx - 50, cy); path1.lineTo(cx - 250, cy)
            path2.moveTo(cx + 50, cy); path2.lineTo(cx + 250, cy)
        } else {
            path1.moveTo(cx - 250, cy); path1.lineTo(cx - 50, cy)
            path2.moveTo(cx + 250, cy); path2.lineTo(cx + 50, cy)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path1, 0, 300))
            .addStroke(GestureDescription.StrokeDescription(path2, 0, 300))
            .build()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gesture: GestureDescription?) {
                Toast.makeText(this@MyAccessibilityService,
                    if (zoomIn) "🔍 Увеличено!" else "🔎 Уменьшено!",
                    Toast.LENGTH_SHORT).show()
            }
            override fun onCancelled(gesture: GestureDescription?) {
                Toast.makeText(this@MyAccessibilityService, "❌ Зум не сработал", Toast.LENGTH_SHORT).show()
            }
        }, null)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(rebuildReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            windowManager?.removeView(triggerButton)
            windowManager?.removeView(controlPanel)
            windowManager?.removeView(cursorView)
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
}
