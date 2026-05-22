package com.example

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Player {
    var x: Float = 150f
    var y: Float = 400f
    var vx: Float = 10f
    var vy: Float = 0f

    val radius: Float = 24f
    val width: Float = 48f
    val height: Float = 64f

    enum class State {
        SWINGING,
        JUMPING,
        FALLING
    }

    var currentState: State = State.FALLING

    // Trail for style / speed feedback
    val trailPoints = mutableListOf<Pair<Float, Float>>()
    private val maxTrailSize = 10

    // Power-up states
    var isShieldActive: Boolean = false
    var isMagnetActive: Boolean = false
    var isSpeedBoostActive: Boolean = false
    var isSlowMotionActive: Boolean = false

    // Shield effect animation angle
    private var shieldAngle = 0f

    // Selected Skin ID
    var skinId: String = "neon_ninja"

    // Animation frames / timers
    private var animTime = 0f

    // Spin properties
    var rotationAngle: Float = 0f
    var rotationSpeed: Float = 6f

    fun reset(startX: Float, startY: Float) {
        x = startX
        y = startY
        vx = -5f
        vy = 0f
        rotationAngle = 0f
        rotationSpeed = 6f
        currentState = State.FALLING
        trailPoints.clear()
        isShieldActive = false
        isMagnetActive = false
        isSpeedBoostActive = false
        isSlowMotionActive = false
    }

    fun update(gravity: Float, terminalVelocity: Float) {
        // 1. Update Position based on velocity (if not swinging, swinging position is updated by RopePhysics)
        if (currentState != State.SWINGING) {
            vy += gravity
            if (vy > terminalVelocity) vy = terminalVelocity
            x += vx
            y += vy
        }

        // Determine State if not swinging
        if (currentState != State.SWINGING) {
            currentState = if (vy < -2f) State.JUMPING else State.FALLING
        }

        // Update spin angle
        rotationAngle += rotationSpeed
        if (rotationAngle >= 360f) rotationAngle -= 360f

        // Update trail points
        trailPoints.add(Pair(x, y))
        if (trailPoints.size > maxTrailSize) {
            trailPoints.removeAt(0)
        }

        // Animate variables
        animTime += 0.2f
        shieldAngle += 5f
        if (shieldAngle >= 360f) shieldAngle -= 360f
    }

    /**
     * Draws the parkour hero with dynamic kinetic body positioning.
     * Cyberpunk neon glow lines and body joints are adapted to state (Swinging, Jumping, Falling).
     */
    fun draw(canvas: Canvas, paint: Paint, scrollX: Float, scrollY: Float, screenHeight: Float, ropeActive: Boolean, ropeAnchorX: Float, ropeAnchorY: Float) {
        val screenX = x - scrollX
        val screenY = y - scrollY

        // Create secondary neon colors based on active skin
        val skinColors = getSkinColors(skinId)
        val primaryColor = skinColors.first
        val accentColor = skinColors.second

        // 1. Draw stylish neon motion trail
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        for (i in 0 until trailPoints.size - 1) {
            val p1 = trailPoints[i]
            val p2 = trailPoints[i + 1]
            val alpha = ((i.toFloat() / maxTrailSize) * 120).toInt()
            
            paint.color = primaryColor
            paint.alpha = alpha
            paint.strokeWidth = 6f + i * 0.8f
            canvas.drawLine(p1.first - scrollX, p1.second - scrollY, p2.first - scrollX, p2.second - scrollY, paint)
        }
        paint.alpha = 255

        // 1.1 Draw Custom Neon Cyberpunk Glow Effect backing the spinning hero
        paint.style = Paint.Style.FILL
        paint.color = primaryColor
        paint.alpha = 50
        canvas.drawCircle(screenX, screenY, radius * 2.5f, paint)
        paint.alpha = 25
        canvas.drawCircle(screenX, screenY, radius * 4.0f, paint)
        paint.alpha = 255

        // Save canvas for localized rotation or scaling relative to velocity
        canvas.save()
        // Spin the player continuously
        canvas.rotate(rotationAngle, screenX, screenY)

        // Draw Player components with crisp neon vector joints
        // 2. Draw stylish limbs based on State
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f

        val legCycle = sin(animTime) * 15f
        val armCycle = cos(animTime) * 15f

        when (currentState) {
            State.SWINGING -> {
                // Swinging: Limbs tucked, hand pointed towards rope anchor
                // We draw torso, tucked limbs, and an arm holding the rope
                paint.color = accentColor
                // Left leg (Tucked in)
                canvas.drawLine(screenX, screenY, screenX - 10f, screenY + 15f, paint)
                canvas.drawLine(screenX - 10f, screenY + 15f, screenX - 5f, screenY + 28f, paint)
                // Right leg (Tucked in)
                canvas.drawLine(screenX, screenY, screenX + 5f, screenY + 15f, paint)
                canvas.drawLine(screenX + 5f, screenY + 15f, screenX + 12f, screenY + 25f, paint)

                paint.color = primaryColor
                // Torso
                canvas.drawLine(screenX, screenY - 20f, screenX, screenY, paint)

                // Arm holding rope (points towards anchor point)
                if (ropeActive) {
                    val rdaX = ropeAnchorX - x
                    val rdaY = ropeAnchorY - y
                    val len = sqrt(rdaX * rdaX + rdaY * rdaY).coerceAtLeast(1f)
                    val armX = (rdaX / len) * 25f
                    val armY = (rdaY / len) * 25f
                    paint.color = accentColor
                    canvas.drawLine(screenX, screenY - 15f, screenX + armX, screenY - 15f + armY, paint)
                }
            }
            State.JUMPING -> {
                // Jumping: Dynamic pose, legs pointing straight down-back, arms up
                paint.color = accentColor
                // Left leg
                canvas.drawLine(screenX - 4f, screenY, screenX - 12f, screenY + 18f, paint)
                canvas.drawLine(screenX - 12f, screenY + 18f, screenX - 6f, screenY + 30f, paint)
                // Right leg (lagging behind)
                canvas.drawLine(screenX + 4f, screenY, screenX - 4f, screenY + 16f, paint)
                canvas.drawLine(screenX - 4f, screenY + 16f, screenX + 2f, screenY + 28f, paint)

                paint.color = primaryColor
                // Torso
                canvas.drawLine(screenX, screenY - 22f, screenX, screenY, paint)

                // Arms reaching up/forward
                paint.color = accentColor
                canvas.drawLine(screenX, screenY - 18f, screenX + 16f, screenY - 30f, paint)
                canvas.drawLine(screenX, screenY - 18f, screenX - 10f, screenY - 26f, paint)
            }
            State.FALLING -> {
                // Falling: Limbs flailing or relaxed
                paint.color = accentColor
                // Left leg (moving with legCycle)
                canvas.drawLine(screenX - 4f, screenY, screenX - 10f + legCycle * 0.3f, screenY + 18f, paint)
                canvas.drawLine(screenX - 10f + legCycle * 0.3f, screenY + 18f, screenX - 5f + legCycle * 0.4f, screenY + 32f, paint)
                // Right leg (inverse moving)
                canvas.drawLine(screenX + 4f, screenY, screenX + 8f - legCycle * 0.3f, screenY + 18f, paint)
                canvas.drawLine(screenX + 8f - legCycle * 0.3f, screenY + 18f, screenX + 12f - legCycle * 0.4f, screenY + 32f, paint)

                paint.color = primaryColor
                // Torso (slight forward bend)
                canvas.drawLine(screenX - 2f, screenY - 20f, screenX, screenY, paint)

                // Arms flailing
                paint.color = accentColor
                canvas.drawLine(screenX - 2f, screenY - 16f, screenX - 15f, screenY - 10f + armCycle * 0.5f, paint)
                canvas.drawLine(screenX - 2f, screenY - 16f, screenX + 15f, screenY - 12f - armCycle * 0.5f, paint)
            }
        }

        // 3. Draw Body & Glowing Cyber Visor
        paint.style = Paint.Style.FILL
        paint.color = primaryColor
        // Cyber chest armor/body shape
        canvas.drawCircle(screenX, screenY - 10f, 10f, paint)

        // Head (Helmed modern helmet)
        paint.color = primaryColor
        canvas.drawCircle(screenX, screenY - 26f, 9f, paint)

        // Cyber Visor glow (horizontal neon line across the head)
        paint.color = accentColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawLine(screenX - 6f, screenY - 27f, screenX + 6f, screenY - 27f, paint)

        canvas.restore()

        // 4. Draw Active Powerup Visual Effects
        // Shield Bubble Effect (Spinning Neon Circle)
        if (isShieldActive) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = Color.parseColor("#4DEEEA") // Cyan neon
            
            // Draw outer neon shield circle
            canvas.drawCircle(screenX, screenY - 10f, 42f, paint)
            
            // Draw orbital shields dots
            paint.style = Paint.Style.FILL
            val orb1X = screenX + 42f * cos(Math.toRadians(shieldAngle.toDouble())).toFloat()
            val orb1Y = screenY - 10f + 42f * sin(Math.toRadians(shieldAngle.toDouble())).toFloat()
            val orb2X = screenX + 42f * cos(Math.toRadians((shieldAngle + 180f).toDouble())).toFloat()
            val orb2Y = screenY - 10f + 42f * sin(Math.toRadians((shieldAngle + 180f).toDouble())).toFloat()
            
            canvas.drawCircle(orb1X, orb1Y, 6f, paint)
            canvas.drawCircle(orb2X, orb2Y, 6f, paint)
        }

        // Speed Boost visual: fast lines below back
        if (isSpeedBoostActive) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = Color.parseColor("#E0115F") // Ruby red
            val rippleRadius = 25f + (shieldAngle % 30)
            canvas.drawCircle(screenX, screenY - 10f, rippleRadius, paint)
        }

        // Magnet active: drawing magnetic fields around the head
        if (isMagnetActive) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.5f
            paint.color = Color.parseColor("#FFD700") // Neon Gold
            val wave = sin(animTime * 2) * 6f
            canvas.drawCircle(screenX, screenY - 10f, 34f + wave, paint)
        }
    }

    private fun getSkinColors(skinId: String): Pair<Int, Int> {
        return when (skinId) {
            "neon_ninja" -> Pair(Color.parseColor("#39FF14"), Color.parseColor("#FF073A")) // Neon Green & Neon Red
            "cyber_cyberpunk" -> Pair(Color.parseColor("#00FFFF"), Color.parseColor("#FF00FF")) // Cyan & Magenta
            "pixel_retro" -> Pair(Color.parseColor("#FF7F00"), Color.parseColor("#00FF7F")) // Orange & Spring Green
            "gold_hero" -> Pair(Color.parseColor("#FFE700"), Color.parseColor("#FFFFFF")) // Golden & White
            "shadow_specter" -> Pair(Color.parseColor("#9400D3"), Color.parseColor("#500050")) // Dark Violet & Indigo
            "crimson_phoenix" -> Pair(Color.parseColor("#FF1493"), Color.parseColor("#FF8C00")) // Deep pink & Dark orange
            else -> Pair(Color.parseColor("#39FF14"), Color.parseColor("#FF073A"))
        }
    }
}
