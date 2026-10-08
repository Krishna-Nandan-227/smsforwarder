package com.smsrelay.app

import android.content.Context
import android.graphics.*
import android.view.View

internal class PetFace(context: Context) : View(context) {
    var badge = 0
    var species = "Cat"
    var coat = Color.rgb(247, 193, 140)
    var petName = "Milo"
    var animate = true
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        c.save(); c.scale(width / 76f, height / 82f)
        p.style = Paint.Style.FILL; p.color = coat
        when (species) {
            "Dog" -> {
                c.drawOval(4f, 20f, 24f, 62f, p); c.drawOval(52f, 20f, 72f, 62f, p)
                p.color = darken(coat); c.drawOval(4f, 22f, 20f, 61f, p); c.drawOval(56f, 22f, 72f, 61f, p)
            }
            "Rabbit" -> {
                c.drawRoundRect(17f, 2f, 32f, 39f, 9f, 9f, p)
                c.drawRoundRect(44f, 2f, 59f, 39f, 9f, 9f, p)
                p.color = Color.rgb(239, 174, 169)
                c.drawRoundRect(22f, 7f, 27f, 29f, 4f, 4f, p)
                c.drawRoundRect(49f, 7f, 54f, 29f, 4f, 4f, p)
            }
            "Elephant" -> {
                c.drawOval(1f, 23f, 31f, 69f, p); c.drawOval(45f, 23f, 75f, 69f, p)
                p.color = darken(coat); c.drawOval(5f, 28f, 26f, 61f, p); c.drawOval(50f, 28f, 71f, 61f, p)
            }
            else -> {
                val ears = Path().apply {
                    moveTo(12f, 33f); lineTo(12f, 10f); lineTo(30f, 24f); close()
                    moveTo(46f, 24f); lineTo(64f, 10f); lineTo(64f, 33f); close()
                }
                c.drawPath(ears, p)
            }
        }
        p.color = coat; c.drawRoundRect(12f, 23f, 64f, 70f, 22f, 22f, p)
        if (species == "Cat") {
            p.color = darken(coat)
            c.drawRoundRect(28f, 23f, 31f, 33f, 2f, 2f, p)
            c.drawRoundRect(36f, 23f, 39f, 36f, 2f, 2f, p)
            c.drawRoundRect(44f, 23f, 47f, 33f, 2f, 2f, p)
        }
        p.color = if (Color.red(coat)*0.299 + Color.green(coat)*0.587 + Color.blue(coat)*0.114 < 125) Color.rgb(250, 250, 247) else Color.rgb(37, 55, 69)
        if (animate && android.os.SystemClock.uptimeMillis() % 4000 < 220) {
            p.strokeWidth = 3f; c.drawLine(23f, 42f, 31f, 42f, p); c.drawLine(45f, 42f, 53f, 42f, p)
        } else { c.drawCircle(27f, 42f, 3.5f, p); c.drawCircle(49f, 42f, 3.5f, p) }
        if (species == "Elephant") {
            p.color = coat; c.drawRoundRect(32f, 46f, 44f, 78f, 7f, 7f, p)
            p.color = darken(coat); p.strokeWidth = 2f
            c.drawLine(34f, 59f, 41f, 59f, p); c.drawLine(34f, 65f, 41f, 65f, p)
        } else {
            c.drawOval(34f, 49f, 42f, 54f, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 1.7f
            c.drawArc(30f, 49f, 46f, 61f, 0f, 180f, false, p)
            if (species == "Cat" || species == "Rabbit") {
                c.drawLine(16f, 51f, 27f, 53f, p); c.drawLine(49f, 53f, 60f, 51f, p)
            }
            p.style = Paint.Style.FILL
        }
        if (badge > 0) {
            p.color = Color.rgb(37, 55, 69); c.drawCircle(62f, 16f, 13f, p)
            p.color = Color.WHITE; p.textSize = 12f; p.textAlign = Paint.Align.CENTER
            c.drawText(if (badge > 99) "99+" else badge.toString(), 62f, 20f, p)
        }
        c.restore()
        contentDescription = "$petName the $species. $badge new collected messages."
        if (animate && isAttachedToWindow) postInvalidateDelayed(180)
    }
    private fun darken(color: Int) = Color.rgb((Color.red(color)*0.78f).toInt(),
        (Color.green(color)*0.78f).toInt(), (Color.blue(color)*0.78f).toInt())
}
