package com.mykeyboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("kb", Context.MODE_PRIVATE)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(48, 96, 48, 48)
        root.layoutDirection = View.LAYOUT_DIRECTION_RTL

        val title = TextView(this)
        title.text = "إعدادات كيبورد بحر"
        title.textSize = 24f
        root.addView(title)

        val step1 = Button(this)
        step1.text = "1) تفعيل الكيبورد"
        step1.setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        root.addView(step1)

        val step2 = Button(this)
        step2.text = "2) اختيار الكيبورد"
        step2.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }
        root.addView(step2)

        val sizeLabel = TextView(this)
        sizeLabel.textSize = 18f
        root.addView(sizeLabel)

        val seek = SeekBar(this)
        seek.max = 100
        val saved = prefs.getInt("scale", 100)
        seek.progress = saved - 60
        sizeLabel.text = "حجم الكيبورد: $saved%"

        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                val scale = 60 + progress
                sizeLabel.text = "حجم الكيبورد: $scale%"
                prefs.edit().putInt("scale", scale).apply()
            }
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {}
        })
        root.addView(seek)

        val delayLabel = TextView(this)
        delayLabel.textSize = 18f
        delayLabel.setPadding(0, 48, 0, 0)
        root.addView(delayLabel)

        val delaySeek = SeekBar(this)
        delaySeek.max = 470
        val savedDelay = prefs.getInt("delay", 100)
        delaySeek.progress = savedDelay - 30
        delayLabel.text = "وقت الإرسال التلقائي (زر تلقائي): $savedDelay ميلي ثانية"

        delaySeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                val d = 30 + progress
                delayLabel.text = "وقت الإرسال التلقائي (زر تلقائي): $d ميلي ثانية"
                prefs.edit().putInt("delay", d).apply()
            }
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {}
        })
        root.addView(delaySeek)

        setContentView(root)
    }
}
