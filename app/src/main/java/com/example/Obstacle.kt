package com.example

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.cos
import kotlin.math.sin

class Obstacle(
    var x: Float,
    var y: Float,
    val type: Type,
    var width: Float = 60f,
    var height: Float = 60f
) {
    enum class Type {
        LASER,          // Neon vertical laser beam
        DRONE,          // Oscillating electrical hover drone
        SPIKES,         // Spiky roof hazard
        ROTATING_FAN,   // Rotating laser hazard
        COIN,           // Collectible score coin
        POWERUP_SHIELD, // Invulnerability bubble powerup
        POWERUP_SLOW,   // Chrono slow-mo powerup
        POWERUP_MAGNET, // Coin attractor magnet
        POWERUP_BOOST   // High-speed jetpack speedup
    }

    var isCollected: Boolean = false
    var isDestroyed: Boolean = false

    // Animation variables
    private var angle: Float = 0f
    private var baseHeightY: Float = y
    private var hoverOffset: Float = 0f
    private var animeTimer: Float = 0f

    init {
        // Customize collider size parameters per type
        when (type) {
            Type.LASER -> {
                width = 30f
                height = 140f
            }
            Type.DRONE -> {
                width = 65f
                height = 65f
            }
            Type.SPIKES -> {
                width = 80f
                height = 40f
            }
            Type.ROTATING_FAN -> {
                width = 110f
                height = 110f
            }
            Type.COIN -> {
                width = 36f
                height = 36f
            }
            Type.POWERUP_SHIELD, Type.POWERUP_SLOW, Type.POWERUP_MAGNET, Type.POWERUP_BOOST -> {
                width = 44f
                height = 44f
            }
        }
        animeTimer = (Math.random() * 100).toFloat() // Random start phase
    }

    /**
     * Updates obstacle animation states, drone movement oscillations, and coin hover patterns.
     */
    fun update(gameSpeed: Float, isSlowMotion: Boolean) {
        val speedFactor = if (isSlowMotion) 0.4f else 1.0f
        animeTimer += 0.05f * speedFactor
        angle += 4f * speedFactor

        when (type) {
            Type.DRONE -> {
                // Drone oscillates nicely up and down
                hoverOffset = sin(animeTimer * 2f) * 35f
                y = baseHeightY + hoverOffset
            }
            Type.ROTATING_FAN -> {
                // Keep rotating the collider fan blades
                if (angle >= 360f) angle -= 360f
            }
            Type.COIN -> {
                // Micro-hover for coins
                hoverOffset = sin(animeTimer * 3f) * 8f
                y = baseHeightY + hoverOffset
            }
            Type.POWERUP_SHIELD, Type.POWERUP_SLOW, Type.POWERUP_MAGNET, Type.POWERUP_BOOST -> {
                // Slower hover float for items
                hoverOffset = sin(animeTimer * 2f) * 12f
                y = baseHeightY + hoverOffset
            }
            else -> {}
        }
    }

    /**
     * Handles collision check using bounding circles or bounding box logic.
     */
    fun collidesWith(px: Float, py: Float, pRadius: Float): Boolean {
        if (isCollected || isDestroyed) return false

        when (type) {
            Type.COIN, Type.POWERUP_SHIELD, Type.POWERUP_SLOW, Type.POWERUP_MAGNET, Type.POWERUP_BOOST -> {
                // Pure circle to circle fast bounds
                val dx = px - x
                val dy = py - y
                val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                return dist < (pRadius + width / 2f)
            }
            Type.SPIKES -> {
                // AABB logic for Spikes (which rest flat on rooftops)
                val left = x - width / 2f
                val right = x + width / 2f
                val top = y - height / 2f
                val bottom = y + height / 2f
                return px + pRadius > left && px - pRadius < right && py + pRadius > top && py - pRadius < bottom
            }
            Type.LASER -> {
                // Vertical strip AABB
                val left = x - width / 2f
                val right = x + width / 2f
                val top = y - height / 2f
                val bottom = y + height / 2f
                return px + pRadius > left && px - pRadius < right && py + pRadius > top && py - pRadius < bottom
            }
            Type.DRONE -> {
                // Fast radius collision based on drone main core
                val dx = px - x
                val dy = py - y
                val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                return dist < (pRadius + width / 1.8f)
            }
            Type.ROTATING_FAN -> {
                // Circle collider around rotating blades
                val dx = px - x
                val dy = py - y
                val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                return dist < (pRadius + width / 2.2f)
            }
        }
    }

    /**
     * Draws vector art for each item with high visual cyberpunk fidelity using primitive operations.
     */
    fun draw(canvas: Canvas, paint: Paint, scrollX: Float, scrollY: Float) {
        if (isCollected || isDestroyed) return
        val screenX = x - scrollX
        val screenY = y - scrollY

        paint.reset()
        paint.isAntiAlias = true

        when (type) {
            Type.LASER -> {
                // Laser emitters (top and bottom boxes) and a beautiful thick glowing red/pink center laser line
                val halfW = width / 2f
                val halfH = height / 2f

                // Draw Top emitter
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#4B0082") // Dark Indigo
                canvas.drawRect(screenX - halfW, screenY - halfH, screenX + halfW, screenY - halfH + 15f, paint)
                paint.color = Color.parseColor("#00FFFF") // Cyan top cap
                canvas.drawRect(screenX - halfW + 4f, screenY - halfH + 15f, screenX + halfW - 4f, screenY - halfH + 20f, paint)

                // Draw Bottom emitter
                paint.color = Color.parseColor("#4B0082")
                canvas.drawRect(screenX - halfW, screenY + halfH - 15f, screenX + halfW, screenY + halfH, paint)
                paint.color = Color.parseColor("#00FFFF")
                canvas.drawRect(screenX - halfW + 4f, screenY + halfH - 20f, screenX + halfW - 4f, screenY + halfH - 15f, paint)

                // Neon active pulse glow
                val pulseWidth = 5f + sin(animeTimer * 12f) * 2f
                paint.style = Paint.Style.STROKE
                
                // Outer glow magenta
                paint.color = Color.parseColor("#80FF007F") // Semi-trans magenta
                paint.strokeWidth = pulseWidth * 2.8f
                canvas.drawLine(screenX, screenY - halfH + 20f, screenX, screenY + halfH - 20f, paint)

                // Inner electric core white
                paint.color = Color.WHITE
                paint.strokeWidth = pulseWidth * 0.9f
                canvas.drawLine(screenX, screenY - halfH + 20f, screenX, screenY + halfH - 20f, paint)
            }

            Type.DRONE -> {
                // Circular metallic central dome, moving scanning neon visor, side jet thrusters with tiny particle flame lines
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#2F4F4F") // Slate grey
                
                // Outer body
                canvas.drawCircle(screenX, screenY, 20f, paint)
                
                // Drone side shields / wings
                paint.color = Color.parseColor("#1D2530")
                canvas.drawRect(screenX - 32f, screenY - 5f, screenX - 18f, screenY + 5f, paint)
                canvas.drawRect(screenX + 18f, screenY - 5f, screenX + 32f, screenY + 5f, paint)

                // Drone Red Scanning Visor Eye
                paint.color = Color.parseColor("#FF003F")
                val eyeMove = sin(animeTimer * 4f) * 8f
                canvas.drawCircle(screenX + eyeMove, screenY - 1f, 4f, paint)

                // Fire thruster trails from bottom of engines
                val flameLen = 10f + sin(animeTimer * 10f) * 8f
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                paint.color = Color.parseColor("#FF6600") // Thruster amber
                canvas.drawLine(screenX - 25f, screenY + 5f, screenX - 25f, screenY + 5f + flameLen, paint)
                canvas.drawLine(screenX + 25f, screenY + 5f, screenX + 25f, screenY + 5f + flameLen, paint)
            }

            Type.SPIKES -> {
                // Ground triangular neon spikes
                val halfW = width / 2f
                val halfH = height / 2f
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#D2691E") // Industrial spike back

                val path = Path()
                path.moveTo(screenX - halfW, screenY + halfH)
                
                // Draw 4 separate spike teeth
                val spikeCount = 4
                val step = width / spikeCount
                for (i in 0 until spikeCount) {
                    val baseLeft = screenX - halfW + (i * step)
                    val baseRight = baseLeft + step
                    val peakX = baseLeft + step / 2f
                    path.lineTo(peakX, screenY - halfH)
                    path.lineTo(baseRight, screenY + halfH)
                }
                path.close()
                canvas.drawPath(path, paint)

                // Draw neon edge to make them glow menacingly
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                paint.color = Color.parseColor("#FF0055") // Toxic neon red
                canvas.drawPath(path, paint)
            }

            Type.ROTATING_FAN -> {
                // Rotates clockwise: has center hub and 3 glowing fan line spikes
                paint.style = Paint.Style.FILL
                paint.color = Color.DKGRAY
                canvas.drawCircle(screenX, screenY, 15f, paint)

                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 6f
                paint.color = Color.parseColor("#9D00FF") // Cosmic violet
                
                // 3 blades spaced 120 deg apart
                for (i in 0..2) {
                    val mAngle = angle + (i * 120)
                    val rad = Math.toRadians(mAngle.toDouble())
                    val endX = screenX + (width / 2f) * cos(rad).toFloat()
                    val endY = screenY + (width / 2f) * sin(rad).toFloat()
                    
                    canvas.drawLine(screenX, screenY, endX, endY, paint)
                    // Draw a dangerous red ball of energy at each blade end
                    paint.style = Paint.Style.FILL
                    paint.color = Color.parseColor("#FF007F")
                    canvas.drawCircle(endX, endY, 10f, paint)
                    paint.style = Paint.Style.STROKE
                    paint.color = Color.parseColor("#9D00FF")
                }
            }

            Type.COIN -> {
                // Cyber gold coin: drawing coin face with double neon yellow/gold circles and internal 'C' insignia
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#E6B800")
                canvas.drawCircle(screenX, screenY, 16f, paint)

                // Shimmer gold line
                val shimmer = (cos(animeTimer * 2f) * 6f)
                paint.color = Color.parseColor("#FFF3A8")
                canvas.drawCircle(screenX + shimmer / 2.5f, screenY - shimmer / 2.5f, 13f, paint)

                // Golden neon outline
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3.5f
                paint.color = Color.parseColor("#FFD700") // Gold
                canvas.drawCircle(screenX, screenY, 16f, paint)

                // Draw simple "C' in the center
                paint.style = Paint.Style.FILL
                paint.textSize = 17f
                paint.strokeWidth = 1f
                paint.color = Color.parseColor("#8B6508")
                paint.textAlign = Paint.Align.CENTER
                val textY = screenY - ((paint.descent() + paint.ascent()) / 2f)
                canvas.drawText("$", screenX, textY, paint)
            }

            Type.POWERUP_SHIELD -> {
                // Blue neon shield icon inside a glowing ring
                drawPowerupContainer(canvas, paint, screenX, screenY, Color.parseColor("#00FFFF"))
                // Shield insignia
                paint.color = Color.parseColor("#00FFFF")
                paint.style = Paint.Style.FILL
                val sPath = Path().apply {
                    moveTo(screenX, screenY - 12f)
                    lineTo(screenX + 10f, screenY - 8f)
                    lineTo(screenX + 8f, screenY + 4f)
                    lineTo(screenX, screenY + 12f)
                    lineTo(screenX - 8f, screenY + 4f)
                    lineTo(screenX - 10f, screenY - 8f)
                    close()
                }
                canvas.drawPath(sPath, paint)
            }

            Type.POWERUP_SLOW -> {
                // Chrono slow-mo clock icon in cyan/green
                drawPowerupContainer(canvas, paint, screenX, screenY, Color.parseColor("#4DBC8F"))
                // Draw dynamic clock ticking hands
                paint.color = Color.parseColor("#4DBC8F")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                canvas.drawLine(screenX, screenY, screenX, screenY - 10f, paint)
                val handX = screenX + 8f * cos(animeTimer).toFloat()
                val handY = screenY + 8f * sin(animeTimer).toFloat()
                canvas.drawLine(screenX, screenY, handX, handY, paint)
            }

            Type.POWERUP_MAGNET -> {
                // Orange magnet shape
                drawPowerupContainer(canvas, paint, screenX, screenY, Color.parseColor("#FFA500"))
                paint.color = Color.parseColor("#FFA500")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 5f
                // Draw tiny horseshoe magnet
                canvas.drawArc(screenX - 10f, screenY - 8f, screenX + 10f, screenY + 10f, 180f, 180f, false, paint)
                paint.strokeWidth = 3.5f
                paint.color = Color.WHITE
                // White tips
                canvas.drawPoint(screenX - 10f, screenY + 10f, paint)
                canvas.drawPoint(screenX + 10f, screenY + 10f, paint)
            }

            Type.POWERUP_BOOST -> {
                // Turbo Jet boost arrow rocket
                drawPowerupContainer(canvas, paint, screenX, screenY, Color.parseColor("#FF1493"))
                paint.color = Color.parseColor("#FF1493")
                paint.style = Paint.Style.FILL
                // Arrow triangle pointing right/up
                val path = Path().apply {
                    moveTo(screenX - 10f, screenY + 10f)
                    lineTo(screenX - 4f, screenY + 10f)
                    lineTo(screenX + 10f, screenY - 4f)
                    lineTo(screenX + 10f, screenY + 4f)
                    lineTo(screenX + 12f, screenY - 12f)
                    lineTo(screenX - 4f, screenY - 10f)
                    lineTo(screenX + 4f, screenY - 10f)
                    lineTo(screenX - 10f, screenY + 10f)
                    close()
                }
                canvas.drawPath(path, paint)
            }
        }
    }

    private fun drawPowerupContainer(canvas: Canvas, paint: Paint, sx: Float, sy: Float, colorCode: Int) {
        // Double pulsing round capsule container
        paint.color = Color.parseColor("#1A2330")
        paint.style = Paint.Style.FILL
        canvas.drawCircle(sx, sy, width / 2.2f, paint)

        // Pulsing glow border
        val pulse = sin(animeTimer * 2f) * 3f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = colorCode
        canvas.drawCircle(sx, sy, width / 2.2f + pulse, paint)
    }
}
