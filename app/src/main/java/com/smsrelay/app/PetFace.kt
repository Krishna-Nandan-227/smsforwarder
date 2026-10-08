package com.smsrelay.app

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.sin

/** Sculpted illustration: layered light and shadow, drawn locally for any coat colour. */
internal class PetFace(context: Context) : View(context) {
    var badge = 0
    var species = "Cat"
    var coat = Color.rgb(247, 193, 140)
    var petName = "Milo"
    var animate = true
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private fun blend(a: Int, b: Int, t: Float) = Color.rgb(
        (Color.red(a)*(1-t)+Color.red(b)*t).toInt(),
        (Color.green(a)*(1-t)+Color.green(b)*t).toInt(),
        (Color.blue(a)*(1-t)+Color.blue(b)*t).toInt())
    private fun sculpt(lightX: Float = 27f, lightY: Float = 27f, radius: Float = 48f) {
        p.style = Paint.Style.FILL; p.alpha = 255
        p.shader = RadialGradient(lightX, lightY, radius,
            intArrayOf(blend(coat, Color.WHITE, .55f), coat, blend(coat, Color.rgb(70,43,40), .4f)),
            floatArrayOf(0f, .5f, 1f), Shader.TileMode.CLAMP)
    }
    private fun solid(colour: Int) { p.shader = null; p.color = colour; p.style = Paint.Style.FILL }
    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        c.save(); c.scale(width/76f, height/82f)
        p.shader = RadialGradient(38f, 75f, 29f, Color.argb(42,37,55,69), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawOval(9f, 69f, 67f, 81f, p)
        val bob = if (animate) sin(android.os.SystemClock.uptimeMillis()/900.0).toFloat()*1.1f else 0f
        c.save(); c.translate(0f,bob)
        sculpt(28f,52f,35f); c.drawRoundRect(22f,52f,54f,74f,15f,15f,p)
        sculpt(22f,65f,18f); c.drawOval(18f,64f,34f,76f,p)
        sculpt(43f,65f,18f); c.drawOval(42f,64f,58f,76f,p)
        sculpt()
        when(species) {
            "Dog" -> {
                c.drawOval(4f,19f,25f,61f,p); c.drawOval(51f,19f,72f,61f,p)
                solid(blend(coat,Color.rgb(94,55,35),.3f)); c.drawOval(6f,28f,17f,55f,p); c.drawOval(59f,28f,70f,55f,p)
            }
            "Rabbit" -> {
                c.drawRoundRect(17f,1f,32f,39f,9f,9f,p); c.drawRoundRect(44f,1f,59f,39f,9f,9f,p)
                solid(Color.rgb(235,171,159)); c.drawRoundRect(22f,6f,27f,29f,4f,4f,p); c.drawRoundRect(49f,6f,54f,29f,4f,4f,p)
            }
            "Elephant" -> {
                c.drawOval(1f,20f,31f,62f,p); c.drawOval(45f,20f,75f,62f,p)
                solid(blend(coat,Color.rgb(190,129,129),.3f)); c.drawOval(5f,27f,26f,55f,p); c.drawOval(50f,27f,71f,55f,p)
            }
            else -> {
                val ears=Path().apply {
                    moveTo(10f,34f); quadTo(7f,3f,16f,9f); lineTo(31f,24f); close()
                    moveTo(45f,24f); quadTo(69f,1f,66f,34f); close()
                }
                c.drawPath(ears,p)
                solid(blend(coat,Color.rgb(220,134,123),.5f))
                c.drawPath(Path().apply { moveTo(14f,26f); lineTo(14f,14f); lineTo(25f,26f); close()
                    moveTo(51f,26f); lineTo(62f,14f); lineTo(62f,26f); close() },p)
            }
        }
        sculpt(26f,27f,49f); c.drawRoundRect(10f,21f,66f,66f,23f,23f,p)
        // A broad soft highlight gives the forehead a rounded, toy-like finish.
        p.shader = RadialGradient(27f,27f,22f,Color.argb(66,255,255,255),Color.TRANSPARENT,Shader.TileMode.CLAMP)
        c.drawOval(15f,23f,56f,45f,p)
        if(species=="Cat") {
            solid(blend(coat,Color.rgb(149,87,51),.4f))
            c.drawRoundRect(28f,23f,31f,30f,2f,2f,p); c.drawRoundRect(36f,22f,39f,32f,2f,2f,p)
            c.drawRoundRect(44f,23f,47f,30f,2f,2f,p)
        }
        val blink = animate && android.os.SystemClock.uptimeMillis()%4200<180
        for(x in listOf(26f,50f)) {
            solid(Color.rgb(39,44,48))
            if(blink) { p.strokeWidth=2.5f; p.strokeCap=Paint.Cap.ROUND; c.drawLine(x-4,42f,x+4,42f,p) }
            else {
                c.drawOval(x-4.5f,36f,x+4.5f,47f,p)
                solid(Color.rgb(90,102,110)); c.drawOval(x-2.8f,42f,x+2.8f,46f,p)
                solid(Color.WHITE); c.drawCircle(x-1.2f,38.5f,1.6f,p); c.drawCircle(x+1.5f,43f,.7f,p)
            }
        }
        if(species=="Elephant") {
            sculpt(34f,46f,27f); c.drawRoundRect(31f,43f,45f,73f,8f,8f,p)
            solid(blend(coat,Color.BLACK,.23f)); p.strokeWidth=1.2f
            c.drawLine(34f,58f,41f,58f,p); c.drawLine(34f,64f,41f,64f,p)
        } else {
            solid(blend(coat,Color.WHITE,.5f)); c.drawOval(25f,46f,51f,61f,p)
            solid(Color.rgb(112,70,64)); c.drawOval(34f,48f,42f,53f,p)
            p.style=Paint.Style.STROKE; p.strokeWidth=1.3f
            c.drawArc(30f,49f,46f,59f,0f,180f,false,p)
            if(species=="Cat" || species=="Rabbit") {
                c.drawLine(16f,51f,27f,53f,p); c.drawLine(49f,53f,60f,51f,p)
            }
            solid(Color.argb(50,231,121,123)); c.drawOval(17f,46f,25f,50f,p); c.drawOval(51f,46f,59f,50f,p)
        }
        c.restore()
        if(badge>0) {
            solid(Color.rgb(37,55,69)); c.drawCircle(62f,16f,12f,p)
            solid(Color.WHITE); p.textSize=11f; p.textAlign=Paint.Align.CENTER
            c.drawText(if(badge>99) "99+" else badge.toString(),62f,20f,p)
        }
        p.shader=null; c.restore()
        contentDescription="$petName the $species. $badge new collected messages."
        if(animate && isAttachedToWindow) postInvalidateDelayed(80)
    }
}
