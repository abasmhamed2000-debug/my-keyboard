package com.mykeyboard

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView

class MyKeyboardService : InputMethodService() {

    private val bg = Color.parseColor("#1B1B1D")
    private val keyColor = Color.parseColor("#2B2B2E")
    private val accent = Color.parseColor("#1EC3E0")
    private val accentOn = Color.parseColor("#14546A")

    private var scale = 1f
    private var showNumbers = false
    private var autoDigits = 0
    private var typed = 0
    private var tafkeek = false
    private var arabicDigits = false
    private var wordMode = 0 // 0 مطفي، 1 المسافة ترسل، 2 إرسال بالمؤقت

    private val handler = Handler(Looper.getMainLooper())
    private val sendRunnable = Runnable { sendIfNotEmpty() }

    override fun onCreateInputView(): View = build()

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        typed = 0
        setInputView(build())
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        cancelTimer()
    }

    private fun refresh() {
        setInputView(build())
    }

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density * scale).toInt()

    private fun build(): View {
        val prefs = getSharedPreferences("kb", MODE_PRIVATE)
        scale = prefs.getInt("scale", 100) / 100f
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.layoutDirection = View.LAYOUT_DIRECTION_LTR
        root.setBackgroundColor(bg)
        root.setPadding(dp(4), dp(6), dp(4), dp(6))
        root.addView(topRow())
        if (showNumbers) addNumbers(root) else addLetters(root)
        return root
    }

    private fun key(
        label: String,
        weight: Float = 1f,
        color: Int = keyColor,
        textColor: Int = Color.WHITE,
        size: Float = 18f,
        onClick: () -> Unit
    ): TextView {
        val t = TextView(this)
        t.text = label
        t.gravity = Gravity.CENTER
        t.textSize = size * scale
        t.setTextColor(textColor)
        val shape = GradientDrawable()
        shape.cornerRadius = dp(8).toFloat()
        shape.setColor(color)
        t.background = shape
        val lp = LinearLayout.LayoutParams(0, dp(44), weight)
        lp.setMargins(dp(2), dp(3), dp(2), dp(3))
        t.layoutParams = lp
        t.setOnClickListener { onClick() }
        return t
    }

    private fun row(vararg keys: View): LinearLayout {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        for (k in keys) r.addView(k)
        return r
    }

    private fun modeKey(label: String, n: Int): TextView =
        key(label, 1f, if (autoDigits == n) accentOn else keyColor, accent, 13f) {
            autoDigits = n
            typed = 0
            showNumbers = true
            refresh()
        }

    private fun toggleWord(mode: Int) {
        wordMode = if (wordMode == mode) 0 else mode
        cancelTimer()
        autoDigits = 0
        typed = 0
        showNumbers = false
        refresh()
    }

    private fun topRow(): LinearLayout = row(
        modeKey("1 خانه", 1),
        modeKey("2 خانه", 2),
        modeKey("3 خانه", 3),
        key("تفكيك", 1f, if (tafkeek) accentOn else keyColor, accent, 13f) {
            tafkeek = !tafkeek
            refresh()
        },
        key("كلمة", 1f, if (wordMode == 1) accentOn else keyColor, accent, 13f) {
            toggleWord(1)
        },
        key("تلقائي", 1f, if (wordMode == 2) accentOn else keyColor, accent, 13f) {
            toggleWord(2)
        }
    )

    private fun letterRow(chars: String): LinearLayout {
        val r = row()
        for (c in chars) {
            r.addView(key(c.toString()) { typeLetter(c.toString()) })
        }
        return r
    }

    private fun addLetters(root: LinearLayout) {
        root.addView(letterRow("ضصثقفغعهخحج"))
        root.addView(letterRow("شسيبلاتنمكط"))
        val r3 = letterRow("ذءؤرىةوزظد")
        r3.addView(key("⌫", 1.5f, keyColor, accent) { backspace() })
        root.addView(r3)
        root.addView(
            row(
                key("123", 1.3f, keyColor, accent) {
                    showNumbers = true
                    refresh()
                },
                key("،") { typeText("،") },
                key("مسافة", 4f) { spaceTap() },
                key(".") { typeText(".") },
                key("⏎", 1.3f, keyColor, accent) { sendEnter() }
            )
        )
    }

    private fun addNumbers(root: LinearLayout) {
        val digits = if (arabicDigits) "٠١٢٣٤٥٦٧٨٩" else "0123456789"
        fun d(i: Int): TextView = key(digits[i].toString()) { typeDigit(digits[i]) }

        root.addView(row(
            key(".") { typeText(".") }, d(1), d(2), d(3),
            key("⌫", 1f, keyColor, accent) { backspace() }
        ))
        root.addView(row(
            key("+") { typeText("+") }, d(4), d(5), d(6),
            key(if (arabicDigits) "1234" else "١٢٣٤", 1f, keyColor, accent) {
                arabicDigits = !arabicDigits
                refresh()
            }
        ))
        root.addView(row(
            key("×") { typeText("×") }, d(7), d(8), d(9),
            key("مسافة", 1f, keyColor, accent) { typeText(" ") }
        ))
        root.addView(row(
            key("/") { typeText("/") },
            key("↩", 1f, keyColor, accent) {
                autoDigits = 0
                typed = 0
                showNumbers = false
                refresh()
            },
            d(0),
            key("@") { typeText("@") },
            key("⏎", 1f, keyColor, accent) { sendEnter() }
        ))
    }

    private fun cancelTimer() {
        handler.removeCallbacks(sendRunnable)
    }

    private fun scheduleSend() {
        cancelTimer()
        val delay = getSharedPreferences("kb", MODE_PRIVATE).getInt("delay", 100)
        handler.postDelayed(sendRunnable, delay.toLong())
    }

    private fun sendIfNotEmpty() {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(1, 0)?.toString() ?: ""
        if (before.isNotEmpty()) sendEnter()
    }

    private fun spaceTap() {
        cancelTimer()
        if (wordMode != 0) sendIfNotEmpty() else typeText(" ")
    }

    private fun typeText(s: String) {
        cancelTimer()
        currentInputConnection?.commitText(s, 1)
    }

    private fun typeLetter(ch: String) {
        val ic = currentInputConnection ?: return
        var text = ch
        if (tafkeek) {
            val prev = ic.getTextBeforeCursor(1, 0)?.toString() ?: ""
            if (prev.isNotEmpty() && !prev[0].isWhitespace()) text = " $ch"
        }
        ic.commitText(text, 1)
        if (wordMode == 2) scheduleSend()
    }

    private fun typeDigit(c: Char) {
        val isZero = (c == '0' || c == '٠')
        if (autoDigits > 0 && typed == 0 && isZero) return
        currentInputConnection?.commitText(c.toString(), 1)
        if (autoDigits > 0) {
            typed++
            if (typed >= autoDigits) {
                typed = 0
                sendEnter()
            }
        }
    }

    private fun backspace() {
        cancelTimer()
        currentInputConnection?.deleteSurroundingText(1, 0)
        if (typed > 0) typed--
    }

    private fun sendEnter() {
        cancelTimer()
        val ic = currentInputConnection ?: return
        val ei = currentInputEditorInfo
        val options = ei?.imeOptions ?: 0
        val action = options and EditorInfo.IME_MASK_ACTION
        val noEnter = (options and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (!noEnter &&
            action != EditorInfo.IME_ACTION_NONE &&
            action != EditorInfo.IME_ACTION_UNSPECIFIED
        ) {
            ic.performEditorAction(action)
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        }
    }
}
