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
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.hypot

class MyAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var joystickRoot: FrameLayout? = null
    private var cursorView: View? = null
    private var triggerButton: View? = null

    private var cursorX = 540f
    private var cursorY = 1200f
    private lateinit var cursorParams: WindowManager.LayoutParams
    private var overlayType = 0

    // Меню редактирования
    private var editMenu: FrameLayout? = null
    private var savedFocusNode: AccessibilityNodeInfo? = null

    // Жесты тачпада
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var touchStartTime = 0L
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false
    private var longPressFired = false
    private val sensitivity = 2.0f
    private val longPressHandler = Handler(Looper.getMainLooper())
    private val longPressRunnable = Runnable {
        if (!isDragging) {
            longPressFired = true
            val node = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            savedFocusNode = node
            if (node != null && node.isEditable) {
                showEditMenu()
                Toast.makeText(this, "📝 Меню редактирования", Toast.LENGTH_SHORT).show()
            } else {
                performLongPress(cursorX + 40f, cursorY + 40f)
                Toast.makeText(this, "⚠️ Нет поля — системное меню", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val rebuildReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == SettingsManager.ACTION_REBUILD) rebuildJoystick()
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

        // === СОБИРАЕМ ГЕЙМПАД ===
        buildJoystick()

        // === ТРИГГЕР (иконка 🕹️ для скрытия/показа) ===
        triggerButton = TextView(this).apply {
            text = "🕹️"
            textSize = 28f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(200, 0, 150, 255))
            setTextColor(Color.WHITE)
            isClickable = true
            setOnClickListener { toggleJoystickVisibility() }
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

        // === МЕНЮ РЕДАКТИРОВАНИЯ ===
        buildEditMenu()

        val filter = IntentFilter(SettingsManager.ACTION_REBUILD)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(rebuildReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(rebuildReceiver, filter)
        }

        Toast.makeText(this, "✅ Геймпад готов!", Toast.LENGTH_SHORT).show()
    }

    // ================= СБОРКА ГЕЙМПАДА =================
    private fun buildJoystick() {
        val widthPx = SettingsManager.getWidthPx(this)
        val heightPx = SettingsManager.getHeightPx(this)
        val alpha = SettingsManager.getAlpha(this)

        // Корневой контейнер = тачпад + кнопки-островки
        joystickRoot = FrameLayout(this).apply {
            setBackgroundColor(Color.argb((alpha * 230).toInt(), 20, 20, 20))
            val border = GradientDrawable().apply {
                setColor(Color.argb(0, 0, 0, 0))
                setStroke(4, Color.argb(255, 100, 100, 100))
                cornerRadius = 30f
            }
            background = border
        }

        val rootParams = WindowManager.LayoutParams(
            widthPx, heightPx, overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = 20
            y = 20
        }

        // Подсказка в центре (не кликабельна)
        val hint = TextView(this).apply {
            text = "ВОДИ = курсор\nТАП = выбор\nДЕРЖИ = меню"
            gravity = Gravity.CENTER
            setTextColor(Color.argb(100, 255, 255, 255))
            textSize = 12f
        }
        val hintParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ).apply {
            // Отступы, чтобы не перекрывать кнопки по краям
            val marginPercent = 0.18f
            leftMargin = (widthPx * marginPercent).toInt()
            topMargin = (heightPx * 0.12f).toInt()
            rightMargin = (widthPx * marginPercent).toInt()
            bottomMargin = (heightPx * 0.12f).toInt()
        }
        hint.layoutParams = hintParams
        hint.isClickable = false
        hint.isFocusable = false
        joystickRoot?.addView(hint)

        // === РАЗМЕЩЕНИЕ КНОПОК-ОСТРОВКОВ ===
        val btnSize = (minOf(widthPx, heightPx) * 0.14f).toInt() // 14% от меньшей стороны
        val edgeMargin = (minOf(widthPx, heightPx) * 0.04f).toInt() // 4% отступ от края

        // Верхний ряд: зум
        addIslandButton("➖", Color.argb(255, 60, 60, 60)) { performPinch(false) }
            .also { v -> positionButton(v, widthPx * 0.28f - btnSize/2, heightPx * 0.04f, btnSize, btnSize) }
        addIslandButton("➕", Color.argb(255, 60, 60, 60)) { performPinch(true) }
            .also { v -> positionButton(v, widthPx * 0.72f - btnSize/2, heightPx * 0.04f, btnSize, btnSize) }

        // Левая колонка: свайпы
        addIslandButton("⬆️", Color.argb(255, 60, 60, 60)) { performSwipe(0, 500) }
            .also { v -> positionButton(v, edgeMargin.toFloat(), heightPx * 0.22f, btnSize, btnSize) }
        addIslandButton("⬇️", Color.argb(255, 60, 60, 60)) { performSwipe(0, -500) }
            .also { v -> positionButton(v, edgeMargin.toFloat(), heightPx * 0.42f, btnSize, btnSize) }
        addIslandButton("⬅️", Color.argb(255, 60, 60, 60)) { performSwipe(400, 0) }
            .also { v -> positionButton(v, edgeMargin.toFloat(), heightPx * 0.62f, btnSize, btnSize) }
        addIslandButton("➡️", Color.argb(255, 60, 60, 60)) { performSwipe(-400, 0) }
            .also { v -> positionButton(v, edgeMargin.toFloat(), heightPx * 0.82f, btnSize, btnSize) }

        // Правая колонка: действия
        addIslandButton("👆", Color.argb(255, 0, 100, 200)) { performTap(cursorX + 40f, cursorY + 40f) }
            .also { v -> positionButton(v, widthPx - btnSize - edgeMargin.toFloat(), heightPx * 0.22f, btnSize, btnSize) }
        addIslandButton("🏠", Color.argb(255, 100, 100, 0)) { teleportCursor(540f, 1200f) }
            .also { v -> positionButton(v, widthPx - btnSize - edgeMargin.toFloat(), heightPx * 0.42f, btnSize, btnSize) }
        addIslandButton("⬆⬆", Color.argb(255, 100, 0, 100)) { teleportCursor(540f, 80f) }
            .also { v -> positionButton(v, widthPx - btnSize - edgeMargin.toFloat(), heightPx * 0.62f, btnSize, btnSize) }

        // Нижняя кнопка: скрыть пульт
        addIslandButton("❌", Color.argb(255, 150, 50, 50)) { toggleJoystickVisibility() }
            .also { v -> positionButton(v, widthPx * 0.5f - btnSize/2, heightPx - btnSize - edgeMargin.toFloat(), btnSize, btnSize) }

        // === ЛОГИКА УМНОГО ТАЧПАДА (на корневом контейнере) ===
        joystickRoot?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchStartX = event.x
                    touchStartY = event.y
                    lastTouchX = event.x
                    lastTouchY = event.y
                    touchStartTime = System.currentTimeMillis()
                    isDragging = false
                    longPressFired = false
                    longPressHandler.postDelayed(longPressRunnable, 600L)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    if (!isDragging && hypot(dx.toDouble(), dy.toDouble()) > 20.0) {
                        isDragging = true
                        longPressHandler.removeCallbacks(longPressRunnable)
                    }
                    if (isDragging) {
                        cursorX += dx * sensitivity
                        cursorY += dy * sensitivity
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

        windowManager?.addView(joystickRoot, rootParams)
    }

    // Создать кнопку-островок и вернуть её (чтобы потом спозиционировать)
    private fun addIslandButton(text: String, bgColor: Int, onClick: () -> Unit): TextView {
        val btn = TextView(this).apply {
            this.text = text
            textSize = 18f
            gravity = Gravity.CENTER
            setBackgroundColor(bgColor)
            setTextColor(Color.WHITE)
            val shape = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = 16f
            }
            background = shape
            setPadding(8, 8, 8, 8)
            isClickable = true
            isFocusable = true
        }
        btn.setOnClickListener { onClick() }
        btn.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.setBackgroundColor(Color.argb(255, 150, 150, 150))
                    true // Перехватываем — не даём событию уйти в тачпад
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.setBackgroundColor(bgColor)
                    false
                }
                else -> false
            }
        }
        joystickRoot?.addView(btn)
        return btn
    }

    // Спозиционировать кнопку абсолютно внутри FrameLayout
    private fun positionButton(v: View, x: Float, y: Float, w: Int, h: Int) {
        val params = FrameLayout.LayoutParams(w, h).apply {
            leftMargin = x.toInt()
            topMargin = y.toInt()
        }
        v.layoutParams = params
    }

    private fun rebuildJoystick() {
        try { joystickRoot?.let { windowManager?.removeView(it) } } catch (e: Exception) {}
        buildJoystick()
        Toast.makeText(this, "🔄 Геймпад перестроен!", Toast.LENGTH_SHORT).show()
    }

    private fun toggleJoystickVisibility() {
        val isVisible = joystickRoot?.visibility == View.VISIBLE
        joystickRoot?.visibility = if (isVisible) View.GONE else View.VISIBLE
        triggerButton?.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    // ================= МЕНЮ РЕДАКТИРОВАНИЯ =================
    private fun buildEditMenu() {
        editMenu = FrameLayout(this).apply {
            setBackgroundColor(Color.argb(240, 30, 30, 30))
            val shape = GradientDrawable().apply {
                setColor(Color.argb(240, 30, 30, 30))
                cornerRadius = 20f
            }
            background = shape
            setPadding(20, 20, 20, 20)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Заголовок + закрыть
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this).apply {
            text = "📝 Редактирование"
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(0, 0, 20, 10)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(TextView(this).apply {
            text = "✕"
            textSize = 22f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(255, 150, 50, 50))
            setTextColor(Color.WHITE)
            setPadding(15, 10, 15, 10)
            isClickable = true
            setOnClickListener { hideEditMenu() }
        })
        container.addView(header)

        // Ряд 1: Копировать / Вырезать
        val r1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        r1.addView(menuActionBtn("Копировать", Color.argb(255, 60, 60, 120)) {
            performEditAction(AccessibilityNodeInfo.ACTION_COPY, "Копировать")
        })
        r1.addView(menuActionBtn("Вырезать", Color.argb(255, 120, 60, 60)) {
            performEditAction(AccessibilityNodeInfo.ACTION_CUT, "Вырезать")
        })
        container.addView(r1)

        // Ряд 2: Вставить / Выделить всё
        val r2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        r2.addView(menuActionBtn("Вставить", Color.argb(255, 0, 100, 200)) {
            performEditAction(AccessibilityNodeInfo.ACTION_PASTE, "Вставить")
        })
        r2.addView(menuActionBtn("Выделить всё", Color.argb(255, 100, 100, 0)) {
            performLongPress(cursorX + 40f, cursorY + 40f)
            hideEditMenu()
            Toast.makeText(this, "📋 Системное меню — выбери 'Выделить всё'", Toast.LENGTH_LONG).show()
        })
        container.addView(r2)

        editMenu?.addView(container)

        val menuParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }
        editMenu?.visibility = View.GONE
        windowManager?.addView(editMenu, menuParams)
    }

    private fun menuActionBtn(text: String, bgColor: Int, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 15f
            gravity = Gravity.CENTER
            setBackgroundColor(bgColor)
            setTextColor(Color.WHITE)
            setPadding(20, 18, 20, 18)
            val shape = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = 12f
            }
            background = shape
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(6, 6, 6, 6)
            }
            isClickable = true
            setOnClickListener { onClick() }
        }
    }

    private fun showEditMenu() {
        val dm = resources.displayMetrics
        var x = cursorX.toInt() + 80
        var y = cursorY.toInt() + 80
        if (x + 520 > dm.widthPixels) x = dm.widthPixels - 540
        if (y + 360 > dm.heightPixels) y = dm.heightPixels - 380
        if (x < 10) x = 10
        if (y < 10) y = 10

        val newParams = editMenu?.layoutParams as? WindowManager.LayoutParams
        if (newParams != null) {
            newParams.x = x
            newParams.y = y
            windowManager?.updateViewLayout(editMenu, newParams)
        }
        editMenu?.visibility = View.VISIBLE
    }

    private fun hideEditMenu() {
        editMenu?.visibility = View.GONE
        savedFocusNode = null
    }

    private fun performEditAction(action: Int, label: String) {
        val node = savedFocusNode
        if (node != null && node.isEditable) {
            val ok = node.performAction(action)
            Toast.makeText(this, if (ok) "✅ $label" else "❌ $label не сработало", Toast.LENGTH_SHORT).show()
        } else {
            performLongPress(cursorX + 40f, cursorY + 40f)
            Toast.makeText(this, "⚠️ Поле потеряно — системное меню", Toast.LENGTH_SHORT).show()
        }
        hideEditMenu()
    }

    // ================= ЖЕСТЫ =================
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

    private fun teleportCursor(x: Float, y: Float) {
        cursorX = x; cursorY = y
        cursorParams.x = x.toInt(); cursorParams.y = y.toInt()
        windowManager?.updateViewLayout(cursorView, cursorParams)
        Toast.makeText(this, "📍 Курсор перемещён!", Toast.LENGTH_SHORT).show()
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        longPressHandler.removeCallbacksAndMessages(null)
        try { unregisterReceiver(rebuildReceiver) } catch (e: Exception) {}
        try {
            windowManager?.removeView(triggerButton)
            windowManager?.removeView(joystickRoot)
            windowManager?.removeView(cursorView)
            windowManager?.removeView(editMenu)
        } catch (e: Exception) {}
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
}
