package com.example

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Dependencies
    lateinit var gameManager: GameManager
    lateinit var soundManager: SoundManager

    // Interface Callbacks to bridge to Jetpack Compose UI
    var onStateChangedListener: OnStateChangedListener? = null

    interface OnStateChangedListener {
        fun onGameOver(score: Int, coins: Int)
        fun onPauseStateChanged(isPaused: Boolean)
        fun onScoreUpdated(score: Int, coins: Int)
    }

    // Entities
    val player = Player()
    val ropePhysics = RopePhysics()
    
    // Lists of environment pieces
    private val buildings = mutableListOf<Building>()
    private val anchors = mutableListOf<AnchorPoint>()
    private val obstacles = mutableListOf<Obstacle>()
    private val particles = mutableListOf<Particle>()

    // Core camera viewport and physics config
    private var scrollX = 0f
    private var scrollY = 0f

    // Diagonal gravity vectors (pulling diagonally left-downwards)
    private val gravityX = -0.18f
    private val gravityY = 0.28f

    private val terminalVelocityX = -13f
    private val terminalVelocityY = 16f

    private val playerStartX = 0f
    private val playerStartY = 0f

    // Procedural generation trackers
    private var lastGeneratedDistance = 0f
    private val random = Random()

    // Neon & visual effect paints
    private val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 30f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }

    // Interactive targeting
    private var highlightedAnchor: AnchorPoint? = null

    // Game loop state runner
    private var isRunning = false
    private val gameLoopRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                if (!gameManager.isPaused && !gameManager.isGameOver) {
                    updateGame()
                }
                invalidate() // Force redrawing the custom canvas
                postOnAnimation(this) // Trigger synchronized call on next frame
            }
        }
    }

    // Particle pool struct
    private data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var color: Int,
        var size: Float,
        var alpha: Int,
        var life: Int,
        val maxLife: Int
    )

    // Building layout struct
    private data class Building(
        val left: Float,
        val right: Float,
        val top: Float,
        val bottom: Float,
        val colorCode: Int,
        val pulseOffset: Float
    )

    // Target grapple anchor struct
    private data class AnchorPoint(
        val x: Float,
        val y: Float,
        val radius: Float = 22f,
        val pulseOffset: Float = (Math.random() * 5.0).toFloat()
    )

    /**
     * Initializes structural game components.
     * Starts background looping sounds and populates the initial environment layer.
     */
    fun setupGame(gm: GameManager, sm: SoundManager) {
        this.gameManager = gm
        this.soundManager = sm
        this.player.skinId = gm.selectedSkin

        resetWorld()

        // Start Background Music
        soundManager.startBgm()

        // Start Loop
        if (!isRunning) {
            isRunning = true
            postOnAnimation(gameLoopRunnable)
        }
    }

    /**
     * Halts/Resumes rendering threads safely in response to Activity states.
     */
    fun resumeGameLoop() {
        if (!isRunning) {
            isRunning = true
            postOnAnimation(gameLoopRunnable)
            soundManager.startBgm()
        }
    }

    fun stopGameLoop() {
        isRunning = false
        removeCallbacks(gameLoopRunnable)
        soundManager.stopBgm()
    }

    private fun resetWorld() {
        // Clear all persistent variables
        gameManager.startNewRun()
        player.reset(playerStartX, playerStartY)
        ropePhysics.detach()
        buildings.clear()
        anchors.clear()
        obstacles.clear()
        particles.clear()
        scrollX = 0f
        scrollY = 0f
        lastGeneratedDistance = 0f
        highlightedAnchor = null

        // Spawn a sturdy starter platform under the player
        val starterPlatform = Building(
            left = -400f,
            right = 400f,
            top = 100f,
            bottom = 600f,
            colorCode = Color.parseColor("#12161F"),
            pulseOffset = 0f
        )
        buildings.add(starterPlatform)

        // Initial procedural load ahead
        generateProceduralFeatures(1500f)
        onStateChangedListener?.onScoreUpdated(gameManager.currentScore, gameManager.coinsCollectedThisRun)
    }

    /**
     * Primary physical computation tick.
     */
    private fun updateGame() {
        // Slow Motion math scaler
        val isSlowMo = gameManager.slowMoTimeRemainingMs > 0
        val dt = if (isSlowMo) 0.45f else 1.0f

        // 1. Progress timers
        val actualDeltaMs = (16.67f * dt).toLong()
        if (gameManager.shieldTimeRemainingMs > 0) {
            gameManager.shieldTimeRemainingMs -= actualDeltaMs
            if (gameManager.shieldTimeRemainingMs <= 0) player.isShieldActive = false
        }
        if (gameManager.slowMoTimeRemainingMs > 0) {
            gameManager.slowMoTimeRemainingMs -= actualDeltaMs
            if (gameManager.slowMoTimeRemainingMs <= 0) player.isSlowMotionActive = false
        }
        if (gameManager.magnetTimeRemainingMs > 0) {
            gameManager.magnetTimeRemainingMs -= actualDeltaMs
            if (gameManager.magnetTimeRemainingMs <= 0) player.isMagnetActive = false
        }
        if (gameManager.boostTimeRemainingMs > 0) {
            gameManager.boostTimeRemainingMs -= actualDeltaMs
            if (gameManager.boostTimeRemainingMs <= 0) player.isSpeedBoostActive = false
        }

        // Speed increases gradually over time
        val boostMultiplier = if (player.isSpeedBoostActive) 1.6f else 1.0f
        gameManager.currentSpeedMultiplier = (1.0f + (-player.x / 14000f)).coerceAtMost(2.5f) * boostMultiplier

        // 2. Continuous Input and Physics Updates
        // Read on-screen button states
        if (gameManager.isLeftButtonPressed) {
            // A. LEFT BUTTON (UPWARDS GLIDE)
            // Move player upward slightly, slow falling speed, smooth movement
            player.vy -= 0.65f * dt
            player.vx += 0.38f * dt // Oppose the leftward drift, slowing descent
            player.rotationSpeed = 12f
            
            // Spawn tiny upward blue thruster sparks
            if (random.nextInt(3) == 0) {
                addSparkParticle(player.x, player.y + 20f, (random.nextFloat() - 0.5f) * 3f, 4f + random.nextFloat() * 4f, Color.parseColor("#00FFFF"), 4f)
            }
        } else if (gameManager.isRightButtonPressed) {
            // B. RIGHT BUTTON (DRAGON DASH)
            // Fast dash forward, increase rotation speed, quick escape movement
            player.vx -= 0.82f * dt // Fast leftward thrust
            player.vy += 0.55f * dt // Fast downward thrust
            player.rotationSpeed = 26f
            
            // Spawn rapid glowing pink jet sparks
            if (random.nextInt(2) == 0) {
                addSparkParticle(player.x + 20f, player.y - 10f, 4f + random.nextFloat() * 6f, (random.nextFloat() - 0.5f) * 4f, Color.parseColor("#FF007F"), 5f)
            }
        } else {
            // C. NORMAL STATE
            player.rotationSpeed = 7f
        }

        // Apply diagonal gravity vector to player
        player.vx += gravityX * dt
        player.vy += gravityY * dt

        // Dynamic speed limit coercion based on active dash boosters
        val limitVx = if (gameManager.isRightButtonPressed) terminalVelocityX * 1.6f else terminalVelocityX
        val limitVy = if (gameManager.isRightButtonPressed) terminalVelocityY * 1.4f else terminalVelocityY

        player.vx = player.vx.coerceIn(limitVx, 2f)
        player.vy = player.vy.coerceIn(-9f, limitVy)

        // Integrate movement updates
        player.x += player.vx * dt
        player.y += player.vy * dt

        // Update trail points and shield angle calculations via native Player frame updater
        player.update(0f, 0f)

        // 3. Smooth Camera Tracking on BOTH X and Y
        val targetScrollX = player.x - width * 0.40f
        val targetScrollY = player.y - height * 0.40f
        scrollX = scrollX + (targetScrollX - scrollX) * 0.08f * dt
        scrollY = scrollY + (targetScrollY - scrollY) * 0.08f * dt

        // 4. Update and animate obstacles (coins, lasers, traps)
        val iterator = obstacles.iterator()
        while (iterator.hasNext()) {
            val obs = iterator.next()
            obs.update(gameManager.currentSpeedMultiplier, isSlowMo)

            // Remove out-of-screen backward obstacles to save memory
            // Distance check in 2D space
            val dx = obs.x - player.x
            val dy = obs.y - player.y
            if (dx > 1800f || dx < -1850f) {
                iterator.remove()
                continue
            }

            // Attract coins if Magnet is active
            if (player.isMagnetActive && obs.type == Obstacle.Type.COIN) {
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < 320f) {
                    val pullSpeed = 16f * dt
                    obs.x += ((-dx) / dist) * pullSpeed
                    obs.y += ((-dy) / dist) * pullSpeed
                }
            }

            // Check Collision bounds
            if (obs.collidesWith(player.x, player.y, player.radius)) {
                when (obs.type) {
                    Obstacle.Type.COIN -> {
                        obs.isCollected = true
                        gameManager.coinsCollectedThisRun++
                        soundManager.playCoinCollect()
                        spawnExplosionParticles(obs.x, obs.y, Color.parseColor("#FFD700"), 8)
                        onStateChangedListener?.onScoreUpdated(gameManager.currentScore, gameManager.coinsCollectedThisRun)
                    }
                    Obstacle.Type.POWERUP_SHIELD -> {
                        obs.isCollected = true
                        player.isShieldActive = true
                        gameManager.shieldTimeRemainingMs = 8000L // 8 seconds
                        soundManager.playPowerupCollect()
                        spawnExplosionParticles(obs.x, obs.y, Color.parseColor("#00FFFF"), 15)
                    }
                    Obstacle.Type.POWERUP_SLOW -> {
                        obs.isCollected = true
                        player.isSlowMotionActive = true
                        gameManager.slowMoTimeRemainingMs = 6000L // 6 seconds
                        soundManager.playPowerupCollect()
                        spawnExplosionParticles(obs.x, obs.y, Color.parseColor("#4DBC8F"), 15)
                    }
                    Obstacle.Type.POWERUP_MAGNET -> {
                        obs.isCollected = true
                        player.isMagnetActive = true
                        gameManager.magnetTimeRemainingMs = 10000L // 10 seconds
                        soundManager.playPowerupCollect()
                        spawnExplosionParticles(obs.x, obs.y, Color.parseColor("#FFA500"), 15)
                    }
                    Obstacle.Type.POWERUP_BOOST -> {
                        obs.isCollected = true
                        player.isSpeedBoostActive = true
                        gameManager.boostTimeRemainingMs = 4000L // 4 seconds
                        soundManager.playPowerupCollect()
                        spawnExplosionParticles(obs.x, obs.y, Color.parseColor("#FF1493"), 20)
                    }
                    else -> {
                        // Standard obstacle: hit check
                        if (player.isShieldActive) {
                            player.isShieldActive = false
                            gameManager.shieldTimeRemainingMs = 0
                            obs.isDestroyed = true
                            soundManager.playHitExplosion()
                            spawnExplosionParticles(obs.x, obs.y, Color.parseColor("#00FFFF"), 25)
                        } else {
                            // Game Over! Crash
                            triggerGameOver()
                        }
                    }
                }
            }
        }

        // 5. Check structural building/platform landings
        var placedOnBuilding = false
        for (b in buildings) {
            if (player.x >= b.left && player.x <= b.right) {
                // If player is falling down and hits building deck
                if (player.y + player.height / 2.5f >= b.top && player.y - player.height / 2f <= b.top + 35f && player.vy >= 0f) {
                    player.y = b.top - player.height / 2.5f
                    player.vy = 0f
                    if (player.currentState == Player.State.FALLING || player.currentState == Player.State.JUMPING) {
                        player.currentState = Player.State.FALLING // Sliding/running state
                    }
                    placedOnBuilding = true
                    // Add tiny neon dust sparks running below feet
                    if (random.nextInt(3) == 0) {
                        addSparkParticle(player.x, b.top, -player.vx * 0.4f, -random.nextFloat() * 2f, Color.parseColor("#39FF14"), 3f)
                    }
                    break
                }
            }
        }

        // 6. Perform Game Over check (fell off of the center diagonal corridor too far)
        // Average diagonal Y position at player's current absolute distance:
        val dxFactor = -0.643f
        val dyFactor = 0.766f
        val relativeDist = (-player.x / -dxFactor) // Approximate diagonal distance traveled
        val centerCorridorY = relativeDist * dyFactor

        // If player drops 750 pixels too deep below the corridor center without recovering, trigger Game Over!
        if (player.y > centerCorridorY + 750f && !gameManager.isGameOver) {
            triggerGameOver()
        }

        // 7. Progress Distance/Scores (X-axis movement determines score)
        if (!gameManager.isGameOver) {
            val calculatedScore = (-player.x / 12f).toInt()
            if (calculatedScore > gameManager.currentScore) {
                gameManager.currentScore = calculatedScore
                onStateChangedListener?.onScoreUpdated(gameManager.currentScore, gameManager.coinsCollectedThisRun)
            }
        }

        // 8. Generate upcoming procedural objects dynamically further down the slide pipeline
        val currentTraveledDiagonal = sqrt(player.x * player.x + player.y * player.y)
        generateProceduralFeatures(currentTraveledDiagonal + width * 2.5f)

        // 9. Update particles (decaying life parameters)
        updateParticles(dt)
    }

    private fun triggerGameOver() {
        if (gameManager.isGameOver) return
        soundManager.playHitExplosion()
        spawnExplosionParticles(player.x, player.y, Color.parseColor("#FF073A"), 40)
        ropePhysics.detach()
        gameManager.endRunAndSave()
        stopGameLoop()
        onStateChangedListener?.onGameOver(gameManager.currentScore, gameManager.coinsCollectedThisRun)
    }

    /**
     * Selects the most optimal spatial anchor point ahead of the player to snap the swing grapple.
     */
    private fun findBestGrappleAnchor() {
        if (ropePhysics.isAttached) {
            highlightedAnchor = null
            return
        }

        var idealAnchor: AnchorPoint? = null
        var minScore = Float.MAX_VALUE

        val maxAttachDistance = 580f
        val minAttachDistance = 50f

        for (a in anchors) {
            val dx = a.x - player.x
            val dy = a.y - player.y
            val dist = sqrt(dx * dx + dy * dy)

            // Anchor MUST be in front of the player (or slightly behind but high)
            if (dx > -50f && dist in minAttachDistance..maxAttachDistance) {
                // Angle math to ensure grappling angle is pleasant (between 10deg to 110deg down)
                // Score is weighted so anchors slightly ahead-upward (45deg to 60deg ahead) get high preference
                val anglePreference = Math.abs(dx - dy * 0.7f) // Closer to a 45 degree angle
                val totalWeight = dist + anglePreference * 0.8f
                if (totalWeight < minScore) {
                    minScore = totalWeight
                    idealAnchor = a
                }
            }
        }
        highlightedAnchor = idealAnchor
    }

    /**
     * Appends procedurally generated obstacles, gaps, roof platforms, cranes, and coin arcs.
     * Generates everything along a 50-degree diagonal corridor running down-left!
     */
    private fun generateProceduralFeatures(targetDistance: Float) {
        while (lastGeneratedDistance < targetDistance) {
            // Group-scale segment variables
            // Average diagonal angle: 50 degrees
            // dx = -cos(50) = -0.643
            // dy = sin(50) = 0.766
            val dxFactor = -0.643f
            val dyFactor = 0.766f

            // Spacing step along the diagonal direction
            val spacingStep = (250 + random.nextInt(180)).toFloat()
            lastGeneratedDistance += spacingStep

            // Compute center of this new layout segment
            val centerX = lastGeneratedDistance * dxFactor
            val centerY = lastGeneratedDistance * dyFactor

            // 1. Procedural Building Block platform (a beautiful neon building that acts as a landing platform or a hazard)
            if (random.nextFloat() < 0.75f) {
                val bWidth = (220 + random.nextInt(280)).toFloat()
                // Center the platform around the diagonal centerline with some minor vertical offset
                val bHeight = 80f + random.nextInt(140)
                
                val pLeft = centerX - bWidth / 2f
                val pRight = centerX + bWidth / 2f
                val pTop = centerY + (random.nextFloat() - 0.4f) * 150f
                val pBottom = pTop + bHeight

                val buildingColor = when (gameManager.selectedBackground) {
                    "solar_cloud" -> Color.parseColor("#1B121C")
                    "matrix_green" -> Color.parseColor("#050F05")
                    else -> Color.parseColor("#121820") // Tokyo theme
                }

                buildings.add(Building(
                    left = pLeft,
                    right = pRight,
                    top = pTop,
                    bottom = pBottom + 400f, // Extend down
                    colorCode = buildingColor,
                    pulseOffset = random.nextFloat() * 10f
                ))

                // Spawn some Spikes directly on top of this platform!
                if (random.nextFloat() < 0.4f) {
                    val spikeX = pLeft + 30f + random.nextInt((bWidth - 60f).toInt().coerceAtLeast(1))
                    obstacles.add(Obstacle(spikeX, pTop - 20f, Obstacle.Type.SPIKES))
                }
            }

            // 2. Deco Neon Stars / Anchors floating in the cyber sky
            val anchorX = centerX + (random.nextFloat() - 0.5f) * 500f
            val anchorY = centerY - 250f - random.nextInt(200)
            anchors.add(AnchorPoint(anchorX, anchorY))

            // 3. Spawning Dynamic Hazards: Saws, Lasers, and Drones
            val levelProgressFactor = lastGeneratedDistance / 3000f
            val obstacleSpawnChance = (0.28f + (levelProgressFactor * 0.05f)).coerceAtMost(0.75f)

            if (random.nextFloat() < obstacleSpawnChance) {
                val selection = random.nextFloat()
                val obsX = centerX + (random.nextFloat() - 0.5f) * 350f
                val obsY = centerY + (random.nextFloat() - 0.5f) * 300f

                if (selection < 0.28f) {
                    // Toxic spin fan blade
                    obstacles.add(Obstacle(obsX, obsY, Obstacle.Type.ROTATING_FAN))
                } else if (selection < 0.60f) {
                    // Patrol hovering drone
                    obstacles.add(Obstacle(obsX, obsY, Obstacle.Type.DRONE))
                } else {
                    // Laser barrier
                    obstacles.add(Obstacle(obsX, obsY, Obstacle.Type.LASER))
                }
            }

            // 4. Sparkly Hovering Coins
            if (random.nextFloat() < 0.70f) {
                // Spawn sequential gold coins following the diagonal trajectory
                val coinCount = 3 + random.nextInt(3)
                for (c in 0 until coinCount) {
                    val cDist = lastGeneratedDistance - spacingStep * 0.4f + (c * 40f)
                    val coinX = cDist * dxFactor + (random.nextFloat() - 0.5f) * 60f
                    val coinY = cDist * dyFactor + (random.nextFloat() - 0.5f) * 60f
                    obstacles.add(Obstacle(coinX, coinY, Obstacle.Type.COIN))
                }
            }

            // 5. Spawn rare floating power-up bubbles
            if (random.nextFloat() < 0.12f) {
                val pX = centerX + (random.nextFloat() - 0.5f) * 200f
                val pY = centerY + (random.nextFloat() - 0.5f) * 200f
                val pTypes = arrayOf(
                    Obstacle.Type.POWERUP_SHIELD,
                    Obstacle.Type.POWERUP_SLOW,
                    Obstacle.Type.POWERUP_MAGNET,
                    Obstacle.Type.POWERUP_BOOST
                )
                val selectedType = pTypes[random.nextInt(pTypes.size)]
                obstacles.add(Obstacle(pX, pY, selectedType))
            }

            // Memory trim for performance
            if (buildings.size > 22) {
                buildings.removeAt(0)
            }
            if (anchors.size > 30) {
                anchors.removeAt(0)
            }
        }
    }

    /**
     * Touch input interceptor - touch state is controlled via composite overlays.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        performClick()
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    /**
     * Visual Spark and particle spawners
     */
    private fun spawnExplosionParticles(px: Float, py: Float, colorCode: Int, count: Int) {
        for (i in 0 until count) {
            val angle = random.nextFloat() * Math.PI * 2
            val velocity = 2f + random.nextFloat() * 7f
            val pvx = (cos(angle) * velocity).toFloat()
            val pvy = (sin(angle) * velocity).toFloat()
            addSparkParticle(px, py, pvx, pvy, colorCode, 4f + random.nextFloat() * 4f, 25)
        }
    }

    private fun spawnGrappleSparks(ax: Float, ay: Float) {
        for (i in 0 until 12) {
            val pvx = (random.nextFloat() - 0.5f) * 8f
            val pvy = random.nextFloat() * 6f + 2f
            addSparkParticle(ax, ay, pvx, pvy, Color.parseColor("#FFFF00"), 4f)
        }
    }

    private fun addSparkParticle(px: Float, py: Float, pvx: Float, pvy: Float, col: Int, size: Float, maxLife: Int = 18) {
        if (particles.size > 140) {
            particles.removeAt(0)
        }
        particles.add(
            Particle(
                x = px,
                y = py,
                vx = pvx,
                vy = pvy,
                color = col,
                size = size,
                alpha = 255,
                life = maxLife,
                maxLife = maxLife
            )
        )
    }

    private fun updateParticles(dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.life = (p.life - (1 * dt).toInt()).coerceAtLeast(0)
            if (p.life <= 0) {
                iter.remove()
                continue
            }
            p.x += p.vx * dt
            p.y += p.vy * dt
            // Add gravity to decay path
            p.y += 0.08f * dt
            p.alpha = ((p.life.toFloat() / p.maxLife) * 255).toInt()
        }
    }

    /**
     * Primary graphics rendering.
     * Draws beautiful layered background parallax and neon cyberpunk vectors.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Reset canvas colors using selected settings background style
        canvas.drawColor(getSkyColor())

        // 1. Draw multi-layered parallax backdrop
        drawParallaxCity(canvas)

        // 2. Draw ground building rooftops with vibrant neon borders
        drawCityBuildings(canvas)

        // 3. Draw active grapple anchor nodes
        drawAnchors(canvas)

        // 4. Draw Obstacles, powerups, coins
        drawObstacles(canvas)

        // 5. Draw active particle sparkles
        drawParticles(canvas)

        // 6. Draw rope cable connecting player and anchor
        drawGrappleRope(canvas)

        // 7. Draw our stylish ninja player
        player.draw(
            canvas = canvas,
            paint = mainPaint,
            scrollX = scrollX,
            scrollY = scrollY,
            screenHeight = height.toFloat(),
            ropeActive = ropePhysics.isAttached,
            ropeAnchorX = ropePhysics.anchorX,
            ropeAnchorY = ropePhysics.anchorY
        )
    }

    private fun getSkyColor(): Int {
        return when (gameManager.selectedBackground) {
            "solar_cloud" -> Color.parseColor("#150A21") // Deep purple sunset
            "matrix_green" -> Color.parseColor("#020C02") // Dark digital matrix green
            else -> Color.parseColor("#090B10") // Neon Midnight Tokyo
        }
    }

    private fun drawParallaxCity(canvas: Canvas) {
        val skyColor = getSkyColor()
        
        // Background Layer 1: Sky Grid Net (very slow parallax)
        mainPaint.strokeWidth = 1f
        mainPaint.color = Color.parseColor("#1Fffffff")
        val gridSpacing = 80f
        val startOffsetGridX = -(scrollX * 0.05f) % gridSpacing
        val startOffsetGridY = -(scrollY * 0.05f) % gridSpacing
        for (gx in 0..width / gridSpacing.toInt() + 1) {
            val xPos = startOffsetGridX + gx * gridSpacing
            canvas.drawLine(xPos, 0f, xPos, height.toFloat(), mainPaint)
        }
        for (gy in 0..height / gridSpacing.toInt() + 1) {
            val yPos = startOffsetGridY + gy * gridSpacing
            canvas.drawLine(0f, yPos, width.toFloat(), yPos, mainPaint)
        }

        // Background Layer 2: Distant silhouette skyscrapers (Parallax factor 0.1)
        val p1Factor = 0.08f
        mainPaint.style = Paint.Style.FILL
        mainPaint.color = when (gameManager.selectedBackground) {
            "solar_cloud" -> Color.parseColor("#26152F")
            "matrix_green" -> Color.parseColor("#091809")
            else -> Color.parseColor("#11151F")
        }

        val dBuildingW = 180f
        val startVal1 = -(scrollX * p1Factor) % dBuildingW
        for (i in -1..(width / dBuildingW).toInt() + 2) {
            val x = startVal1 + i * dBuildingW
            // Deterministic taller skyscraper heights
            val bHeight = 310f + sin((i + 3).toDouble()).toFloat() * 80f
            val topY = height - bHeight - (scrollY * p1Factor)
            canvas.drawRect(x, topY, x + dBuildingW - 10f, height.toFloat() + 500f, mainPaint)

            // Faint windows in background
            mainPaint.color = when (gameManager.selectedBackground) {
                "solar_cloud" -> Color.parseColor("#3FA34D9F")
                "matrix_green" -> Color.parseColor("#2F00FF00")
                else -> Color.parseColor("#1F00FFFF")
            }
            canvas.drawRect(x + 20f, topY + 40f, x + dBuildingW - 30f, topY + 70f, mainPaint)
            mainPaint.color = when (gameManager.selectedBackground) {
                "solar_cloud" -> Color.parseColor("#26152F")
                "matrix_green" -> Color.parseColor("#091809")
                else -> Color.parseColor("#11151F")
            }
        }

        // Background Layer 3: Mid-ground neon towers (Parallax factor 0.22)
        val p2Factor = 0.22f
        mainPaint.color = when (gameManager.selectedBackground) {
            "solar_cloud" -> Color.parseColor("#301A38")
            "matrix_green" -> Color.parseColor("#0F280F")
            else -> Color.parseColor("#191D2B")
        }

        val mBuildingW = 280f
        val startVal2 = -(scrollX * p2Factor) % mBuildingW
        for (j in -1..(width / mBuildingW).toInt() + 2) {
            val mx = startVal2 + j * mBuildingW
            val mbHeight = 220f + cos((j * 2).toDouble()).toFloat() * 100f
            val topY = height - mbHeight - (scrollY * p2Factor)
            canvas.drawRect(mx, topY, mx + mBuildingW - 20f, height.toFloat() + 500f, mainPaint)

            // Glowing vertical stripe on tower
            mainPaint.style = Paint.Style.STROKE
            mainPaint.strokeWidth = 2f
            mainPaint.color = when (gameManager.selectedBackground) {
                "solar_cloud" -> Color.parseColor("#44FF4500")
                "matrix_green" -> Color.parseColor("#3300FF7F")
                else -> Color.parseColor("#334DEEEA")
            }
            canvas.drawLine(mx + mBuildingW / 2f, topY + 10f, mx + mBuildingW / 2f, height.toFloat() + 500f, mainPaint)

            mainPaint.style = Paint.Style.FILL
            mainPaint.color = when (gameManager.selectedBackground) {
                "solar_cloud" -> Color.parseColor("#301A38")
                "matrix_green" -> Color.parseColor("#0F280F")
                else -> Color.parseColor("#191D2B")
            }
        }
    }

    private fun drawCityBuildings(canvas: Canvas) {
        val accentColor = getPrimaryNeonColor()

        for (b in buildings) {
            val left = b.left - scrollX
            val right = b.right - scrollX
            val top = b.top - scrollY
            val bottom = b.bottom - scrollY

            // A. Draw main shaded building container
            mainPaint.style = Paint.Style.FILL
            mainPaint.color = b.colorCode
            canvas.drawRect(left, top, right, bottom, mainPaint)

            // B. Draw neon highlighted top platform edge
            mainPaint.style = Paint.Style.STROKE
            mainPaint.strokeCap = Paint.Cap.ROUND
            mainPaint.strokeWidth = 6.5f
            mainPaint.color = accentColor
            canvas.drawLine(left, top, right, top, mainPaint)

            // C. Draw aesthetic neon structural side trusses/girders
            mainPaint.style = Paint.Style.STROKE
            mainPaint.strokeWidth = 1.5f
            mainPaint.color = Color.parseColor("#2Affffff")
            val step = 80f
            var trussX = left + 30f
            while (trussX < right - 30f) {
                canvas.drawLine(trussX, top + 10f, trussX + 40f, top + 60f, mainPaint)
                canvas.drawLine(trussX + 40f, top + 10f, trussX, top + 60f, mainPaint)
                trussX += step
            }
        }
    }

    private fun drawAnchors(canvas: Canvas) {
        for (a in anchors) {
            val screenX = a.x - scrollX
            val screenY = a.y - scrollY

            // Anchor pulse animations
            val pulse = sin(a.pulseOffset + player.x * 0.05f) * 5f
            
            // Draw outer neon halo circle
            mainPaint.style = Paint.Style.STROKE
            
            // Highlight closest anchor point in cyan/gold, rest in standard neon violet
            if (a == highlightedAnchor) {
                mainPaint.strokeWidth = 4f
                mainPaint.color = Color.parseColor("#FFD700") // Gold tracker lock!
                canvas.drawCircle(screenX, screenY, a.radius + 6f + pulse, mainPaint)

                // Draw aiming crosshairs
                paintCrosshair(canvas, screenX, screenY, a.radius + 14f)
            } else {
                mainPaint.strokeWidth = 2.5f
                mainPaint.color = Color.parseColor("#FF00FF") // Magenta grapple halo
                canvas.drawCircle(screenX, screenY, a.radius + pulse, mainPaint)
            }

            // Draw center node core
            mainPaint.style = Paint.Style.FILL
            mainPaint.color = if (a == highlightedAnchor) Color.WHITE else Color.parseColor("#9900FF")
            canvas.drawCircle(screenX, screenY, a.radius * 0.55f, mainPaint)
            
            // Core accent white dot
            mainPaint.color = Color.WHITE
            canvas.drawCircle(screenX, screenY, 4f, mainPaint)
        }
    }

    private fun paintCrosshair(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        mainPaint.style = Paint.Style.STROKE
        mainPaint.strokeWidth = 2f
        mainPaint.color = Color.parseColor("#FFD700")
        // Draw 4 corner hair stems
        canvas.drawLine(cx - radius, cy, cx - radius + 8f, cy, mainPaint)
        canvas.drawLine(cx + radius, cy, cx + radius - 8f, cy, mainPaint)
        canvas.drawLine(cx, cy - radius, cx, cy - radius + 8f, mainPaint)
        canvas.drawLine(cx, cy + radius, cx, cy + radius - 8f, mainPaint)
    }

    private fun drawObstacles(canvas: Canvas) {
        for (obs in obstacles) {
            obs.draw(canvas, mainPaint, scrollX, scrollY)
        }
    }

    private fun drawParticles(canvas: Canvas) {
        mainPaint.style = Paint.Style.FILL
        for (p in particles) {
            mainPaint.color = p.color
            mainPaint.alpha = p.alpha
            canvas.drawCircle(p.x - scrollX, p.y - scrollY, p.size, mainPaint)
        }
        mainPaint.alpha = 255 // Reset alpha
    }

    private fun drawGrappleRope(canvas: Canvas) {
        if (!ropePhysics.isAttached) return

        val screenPX = player.x - scrollX
        val screenPY = player.y - player.height / 3.5f - scrollY // Rope attached near chest/hands
        val screenAX = ropePhysics.anchorX - scrollX
        val screenAY = ropePhysics.anchorY - scrollY

        // Draw multiple glowing layers to make rope look like light-plasma beam
        val ropeStyle = gameManager.selectedRopeStyle
        val coreColor = getRopeColor(ropeStyle)

        mainPaint.style = Paint.Style.STROKE
        mainPaint.strokeCap = Paint.Cap.ROUND

        // 1. Draw outer wide glowing beam
        mainPaint.color = coreColor
        mainPaint.alpha = 110
        mainPaint.strokeWidth = 7.5f
        canvas.drawLine(screenPX, screenPY, screenAX, screenAY, mainPaint)

        // 2. Draw inner bright central core filament
        mainPaint.color = Color.WHITE
        mainPaint.alpha = 255
        mainPaint.strokeWidth = 2.5f
        canvas.drawLine(screenPX, screenPY, screenAX, screenAY, mainPaint)
    }

    private fun getRopeColor(ropeStyle: String): Int {
        return when (ropeStyle) {
            "plasma_beam" -> Color.parseColor("#00FFFF") // Electric Cyan
            "lava_strand" -> Color.parseColor("#FF4500") // Lava Red
            "chrono_wire" -> Color.parseColor("#9D00FF") // Chrono Spatial Purple
            else -> Color.parseColor("#39FF14") // Standard Neon Green
        }
    }

    private fun getPrimaryNeonColor(): Int {
        return when (gameManager.selectedBackground) {
            "solar_cloud" -> Color.parseColor("#FF00FF") // Hot Magenta
            "matrix_green" -> Color.parseColor("#00FF55") // Matrix Lime Green
            else -> Color.parseColor("#00E5FF") // Midnight Cyan Glow
        }
    }
}
