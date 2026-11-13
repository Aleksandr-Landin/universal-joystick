// --- ТАЧПАД (ИСПРАВЛЕНО) ---
val trackpad = FrameLayout(this).apply {
    layoutParams = LinearLayout.LayoutParams(250, 250).apply { 
        bottomMargin = 20 
    }
    val border = GradientDrawable()
    border.setColor(Color.argb(255, 50, 50, 50))
    border.setStroke(4, Color.argb(255, 100, 100, 100))
    background = border
    
    // ВАЖНО: делаем тачпад интерактивным
    isClickable = true
    isFocusable = true
    isFocusableInTouchMode = true
    
    val hint = TextView(this@MyAccessibilityService).apply {
        text = "ВОДИ ЗДЕСЬ\nдля курсора"
        gravity = Gravity.CENTER
        setTextColor(Color.argb(150, 255, 255, 255))
        textSize = 14f
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        // ВАЖНО: отключаем перехват касаний у текста
        isClickable = false
        isFocusable = false
        isFocusableInTouchMode = false
        isEnabled = false  // полностью пассивный
    }
    addView(hint)
}

var lastTouchX = 0f
var lastTouchY = 0f
val sensitivity = 2.0f

trackpad.setOnTouchListener { v, event ->
    when (event.action) {
        MotionEvent.ACTION_DOWN -> {
            lastTouchX = event.x
            lastTouchY = event.y
            // Визуальная обратная связь: меняем цвет при касании
            v.setBackgroundColor(Color.argb(255, 80, 80, 80))
            Toast.makeText(this@MyAccessibilityService, 
                "✋ Тачпад активен!", Toast.LENGTH_SHORT).show()
            true
        }
        MotionEvent.ACTION_MOVE -> {
            val dx = event.x - lastTouchX
            val dy = event.y - lastTouchY
            
            cursorX += dx * sensitivity
            cursorY += dy * sensitivity
            
            // Ограничиваем курсор размерами экрана
            cursorX = cursorX.coerceIn(0f, 1080f)
            cursorY = cursorY.coerceIn(0f, 2400f)

            cursorParams.x = cursorX.toInt()
            cursorParams.y = cursorY.toInt()
            windowManager?.updateViewLayout(cursorView, cursorParams)
            
            lastTouchX = event.x
            lastTouchY = event.y
            true
        }
        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
            // Возвращаем обычный цвет
            v.setBackgroundColor(Color.argb(255, 50, 50, 50))
            true
        }
    }
    false
}
controlPanel?.addView(trackpad)
