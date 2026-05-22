package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var gameManager: GameManager
    private lateinit var soundManager: SoundManager
    private var gameViewInstance: GameView? = null

    enum class Screen {
        SPLASH,
        MAIN_MENU,
        PLAYING,
        SHOP,
        SETTINGS
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        gameManager = GameManager(this)
        soundManager = SoundManager(this)

        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf(Screen.SPLASH) }

                // State holders synchronised to GameView callbacks
                var currentLiveScore by remember { mutableStateOf(0) }
                var currentLiveCoins by remember { mutableStateOf(0) }
                var isGameOverOverlayVisible by remember { mutableStateOf(false) }
                var isGamePausedOverlayVisible by remember { mutableStateOf(false) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0) // Edge-to-edge
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(Color(0xFF070B11))
                    ) {
                        when (currentScreen) {
                            Screen.SPLASH -> {
                                SplashScreen(
                                    onTimeout = {
                                        currentScreen = Screen.MAIN_MENU
                                    }
                                )
                            }
                            Screen.MAIN_MENU -> {
                                MainMenuScreen(
                                    gameManager = gameManager,
                                    soundManager = soundManager,
                                    onPlayClicked = {
                                        currentLiveScore = 0
                                        currentLiveCoins = 0
                                        isGameOverOverlayVisible = false
                                        isGamePausedOverlayVisible = false
                                        currentScreen = Screen.PLAYING
                                    },
                                    onShopClicked = {
                                        currentScreen = Screen.SHOP
                                    },
                                    onSettingsClicked = {
                                        currentScreen = Screen.SETTINGS
                                    }
                                )
                            }
                            Screen.PLAYING -> {
                                GamePlayingScreen(
                                    gameManager = gameManager,
                                    soundManager = soundManager,
                                    currentLiveScore = currentLiveScore,
                                    currentLiveCoins = currentLiveCoins,
                                    isGameOver = isGameOverOverlayVisible,
                                    isPaused = isGamePausedOverlayVisible,
                                    onScoreUpdated = { score, coins ->
                                        currentLiveScore = score
                                        currentLiveCoins = coins
                                    },
                                    onGameOver = {
                                        isGameOverOverlayVisible = true
                                    },
                                    onPauseStateChanged = { paused ->
                                        isGamePausedOverlayVisible = paused
                                    },
                                    onBackToMenu = {
                                        currentScreen = Screen.MAIN_MENU
                                    },
                                    onBindGameView = { gView ->
                                        gameViewInstance = gView
                                    }
                                )
                            }
                            Screen.SHOP -> {
                                ShopScreen(
                                    gameManager = gameManager,
                                    soundManager = soundManager,
                                    onBackClicked = {
                                        currentScreen = Screen.MAIN_MENU
                                    }
                                )
                            }
                            Screen.SETTINGS -> {
                                SettingsScreen(
                                    gameManager = gameManager,
                                    soundManager = soundManager,
                                    onBackClicked = {
                                        currentScreen = Screen.MAIN_MENU
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        gameViewInstance?.resumeGameLoop()
        soundManager.startBgm()
    }

    override fun onPause() {
        super.onPause()
        gameViewInstance?.stopGameLoop()
        soundManager.stopBgm()
    }
}

/**
 * 1. SPLASH SCREEN
 * Animates beautiful glowing titles.
 */
@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1.05f else 0.85f,
        animationSpec = repeatable(
            iterations = 1,
            animation = tween(durationMillis = 2000, easing = LinearEasing)
        ),
        label = "SplashScale"
    )

    val listWaves = listOf(Color(0xFF00FFCC), Color(0xFF9900FF), Color(0xFFFF007F))

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2200) // 2.2 seconds duration
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF030508), Color(0xFF0B0E14))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            // Animated Cyber Hook Icon
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        3.dp,
                        Brush.linearGradient(colors = listWaves),
                        RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Swinger Icon",
                    tint = Color(0xFF39FF14),
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Game Name
            Text(
                text = "ROPE HERO",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF39FF14),
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "SWING",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFFF007F),
                letterSpacing = 6.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(60.dp))

            // Loading bar
            LinearProgressIndicator(
                color = Color(0xFF00FFFF),
                trackColor = Color(0xFF1E283A),
                modifier = Modifier
                    .width(180.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Procedurally Assembling City Skyline...",
                fontSize = 12.sp,
                color = Color.Gray,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * 2. MAIN MENU SCREEN
 * Clean futuristic control hub.
 */
@Composable
fun MainMenuScreen(
    gameManager: GameManager,
    soundManager: SoundManager,
    onPlayClicked: () -> Unit,
    onShopClicked: () -> Unit,
    onSettingsClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF090B10), Color(0xFF141A29))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Cash Purse
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFF1F293D), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Coins balance",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${gameManager.coinBalance}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Top High-Score
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFFFF007F).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFFF007F), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Trophy icon",
                        tint = Color(0xFFFF007F),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BEST: ${gameManager.highScore}m",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Big Title Center Block
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(y = (-20).dp)
            ) {
                Text(
                    text = "ROPE HERO",
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                    letterSpacing = 4.sp
                )
                Text(
                    text = "SWING",
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF39FF14),
                    letterSpacing = 6.sp,
                    modifier = Modifier.offset(y = (-4).dp)
                )
                Text(
                    text = "Endless Cyber-Physics Grappling",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Central Hero Skin preview card
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(130.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF151B26))
                    .border(2.dp, Color(0xFFFF007F).copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CURRENT NINJA",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = gameManager.skinsCatalog.firstOrNull { it.id == gameManager.selectedSkin }?.name ?: "Neon Hero",
                        fontSize = 20.sp,
                        color = Color(0xFF00FFFF),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row {
                        Icon(Icons.Default.Star, "Equipped", tint = Color(0xFF39FF14), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Active & Standing By", fontSize = 12.sp, color = Color.LightGray, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // Actions Buttons footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Central START Action
                Button(
                    onClick = {
                        soundManager.playPowerupCollect()
                        onPlayClicked()
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF39FF14),
                        contentColor = Color.Black
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Text(
                        text = "START SWINGING",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom grid sub-actions
                Row(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Button(
                        onClick = {
                            soundManager.playSwingShoot()
                            onShopClicked()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E283A),
                            contentColor = Color.White
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ShoppingCart, "Shop", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("UPGRADES", fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            soundManager.playSwingShoot()
                            onSettingsClicked()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E283A),
                            contentColor = Color.White
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, "Settings", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SETTINGS", fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * A beautiful, futuristic, transparent cyber button that tracks presses with multi-touch precision.
 */
@Composable
fun CyberButton(
    label: String,
    subLabel: String,
    icon: ImageVector,
    colorAccent: Color,
    onPressStateChanged: (isPressed: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    iconRotation: Float = 0f,
    testTag: String = ""
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.90f else 1.0f, label = "cyber_btn_scale")
    val alphaAnim by animateFloatAsState(if (isPressed) 0.82f else 0.44f, label = "cyber_btn_alpha")

    Box(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = alphaAnim))
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = if (isPressed) listOf(colorAccent, colorAccent.copy(alpha = 0.5f))
                             else listOf(colorAccent.copy(alpha = 0.4f), Color.Transparent)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val isDown = event.changes.any { it.pressed }
                        if (isDown != isPressed) {
                            isPressed = isDown
                            onPressStateChanged(isDown)
                        }
                    }
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = colorAccent,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer(rotationZ = iconRotation)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = subLabel,
                    color = colorAccent.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * 3. EXCEL-GRADE PLAYING HOOK OVERLAY SCREEN
 * Displays procedural Canvas view with interactive M3 HUD elements.
 */
@Composable
fun GamePlayingScreen(
    gameManager: GameManager,
    soundManager: SoundManager,
    currentLiveScore: Int,
    currentLiveCoins: Int,
    isGameOver: Boolean,
    isPaused: Boolean,
    onScoreUpdated: (Int, Int) -> Unit,
    onGameOver: () -> Unit,
    onPauseStateChanged: (Boolean) -> Unit,
    onBackToMenu: () -> Unit,
    onBindGameView: (GameView) -> Unit
) {
    val context = LocalContext.current
    var localGameView by remember { mutableStateOf<GameView?>(null) }

    // Trigger timer indicators on layout loop
    var shieldPct by remember { mutableStateOf(0f) }
    var slowMoPct by remember { mutableStateOf(0f) }
    var magnetPct by remember { mutableStateOf(0f) }
    var boostPct by remember { mutableStateOf(0f) }

    LaunchedEffect(isPaused, isGameOver) {
        while (!isPaused && !isGameOver) {
            shieldPct = if (gameManager.shieldTimeRemainingMs > 0L) (gameManager.shieldTimeRemainingMs / 8000f).coerceIn(0f, 1f) else 0f
            slowMoPct = if (gameManager.slowMoTimeRemainingMs > 0L) (gameManager.slowMoTimeRemainingMs / 6000f).coerceIn(0f, 1f) else 0f
            magnetPct = if (gameManager.magnetTimeRemainingMs > 0L) (gameManager.magnetTimeRemainingMs / 10000f).coerceIn(0f, 1f) else 0f
            boostPct = if (gameManager.boostTimeRemainingMs > 0L) (gameManager.boostTimeRemainingMs / 4000f).coerceIn(0f, 1f) else 0f
            delay(100)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Embedded Procedural Game Canvas View
        AndroidView(
            factory = { ctx ->
                GameView(ctx).apply {
                    localGameView = this
                    setupGame(gameManager, soundManager)
                    onStateChangedListener = object : GameView.OnStateChangedListener {
                        override fun onGameOver(score: Int, coins: Int) {
                            onGameOver()
                        }

                        override fun onPauseStateChanged(isPaused: Boolean) {
                            onPauseStateChanged(isPaused)
                        }

                        override fun onScoreUpdated(score: Int, coins: Int) {
                            onScoreUpdated(score, coins)
                        }
                    }
                    onBindGameView(this)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { gView ->
                // Ensure live values remain clean
                localGameView = gView
            }
        )

        // Fixed Controls at Bottom Screen (Cyberpunk Dual Buttons)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CyberButton(
                label = "UPWARDS GLIDE",
                subLabel = "HOLD TO GLIDE",
                icon = Icons.Default.PlayArrow,
                iconRotation = -90f,
                colorAccent = Color(0xFF00FFFF), // Cyan
                onPressStateChanged = { pressed ->
                    gameManager.isLeftButtonPressed = pressed
                },
                modifier = Modifier.weight(1f),
                testTag = "left_glide_button"
            )

            CyberButton(
                label = "DRAGON DASH",
                subLabel = "HOLD TO DASH",
                icon = Icons.Default.PlayArrow,
                colorAccent = Color(0xFFFF007F), // Neon Pink/Rose
                onPressStateChanged = { pressed ->
                    gameManager.isRightButtonPressed = pressed
                },
                modifier = Modifier.weight(1f),
                testTag = "right_dash_button"
            )
        }

        // HUD Dashboard Overlay (Top Left Screen)
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(18.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            // Distance indicator
            Text(
                text = "${currentLiveScore}m",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF39FF14) // Hot Green
            )
            // Coin increment indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Coin icon",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "+$currentLiveCoins",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFFFD700)
                )
            }
        }

        // Active Powerup visual progress dials
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (shieldPct > 0f) {
                PowerupIndicator(label = "SHIELD", progress = shieldPct, barColor = Color(0xFF00FFFF))
            }
            if (slowMoPct > 0f) {
                PowerupIndicator(label = "SLOW-MO", progress = slowMoPct, barColor = Color(0xFF4DBC8F))
            }
            if (magnetPct > 0f) {
                PowerupIndicator(label = "MAGNET", progress = magnetPct, barColor = Color(0xFFFFA500))
            }
            if (boostPct > 0f) {
                PowerupIndicator(label = "TURBO BOOST", progress = boostPct, barColor = Color(0xFFFF1493))
            }
        }

        // Top-Right Control Actions (Pause/Mute toggle)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Pause action trigger
            FloatingActionButton(
                onClick = {
                    soundManager.playSwingRelease()
                    gameManager.isPaused = !gameManager.isPaused
                    onPauseStateChanged(gameManager.isPaused)
                },
                containerColor = Color.Black.copy(alpha = 0.6f),
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(46.dp)
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Home, // Visual alternate
                    contentDescription = "Pause Toggle",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // SEMI-TRANSPARENT PAUSE MENU CARD
        if (isPaused && !isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141A29)),
                    border = BorderStroke(2.dp, Color(0xFF00FFFF))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "GAME PAUSED",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF00FFFF),
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Current distance covered: ${currentLiveScore}m",
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.LightGray
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Actions
                        Button(
                            onClick = {
                                soundManager.playPowerupCollect()
                                gameManager.isPaused = false
                                onPauseStateChanged(false)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFFF), contentColor = Color.Black)
                        ) {
                            Text("RESUME GAME", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                soundManager.playPowerupCollect()
                                // Re-trigger initial setups on gameViewInstance
                                gameManager.isPaused = false
                                onPauseStateChanged(false)
                                localGameView?.let { gv ->
                                    gv.setupGame(gameManager, soundManager)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E283A), contentColor = Color.White)
                        ) {
                            Text("RESTART RUN", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        TextButton(
                            onClick = {
                                soundManager.playSwingRelease()
                                localGameView?.stopGameLoop()
                                onBackToMenu()
                            }
                        ) {
                            Text(
                                "QUIT TO MAIN MENU",
                                color = Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // SEMI-TRANSPARENT GAME OVER OVERLAY
        if (isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1215)),
                    border = BorderStroke(2.dp, Color(0xFFFF073A)) // Blood neon red
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "NINJA CRASHED",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFFF1493),
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // High score check indicator
                        if (currentLiveScore >= gameManager.highScore && currentLiveScore > 0) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF39FF14).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF39FF14), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "👑 NEW RECORD HOOKED!",
                                    color = Color(0xFF39FF14),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Run Statistics Card
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("DISTANCE COVERED:", fontSize = 11.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                Text("${currentLiveScore}m", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("COINS GRABBED:", fontSize = 11.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                Text("+$currentLiveCoins", fontSize = 15.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("LIFETIME HIGH SCORE:", fontSize = 11.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                Text("${gameManager.highScore}m", fontSize = 15.sp, color = Color.LightGray, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Button(
                            onClick = {
                                soundManager.playPowerupCollect()
                                localGameView?.let { gv ->
                                    gv.setupGame(gameManager, soundManager)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39FF14), contentColor = Color.Black)
                        ) {
                            Text("PLAY AGAIN", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                soundManager.playSwingShoot()
                                localGameView?.stopGameLoop()
                                onBackToMenu()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E283A), contentColor = Color.White)
                        ) {
                            Text("BACK TO CENTRAL HUB", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PowerupIndicator(label: String, progress: Float, barColor: Color) {
    Row(
        modifier = Modifier
            .width(200.dp)
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                val secLeft = (progress * (if (label == "MAGNET") 10 else if (label == "SHIELD") 8 else if (label == "SLOW-MO") 6 else 4)).toInt()
                Text(text = "${secLeft}s", color = barColor, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.height(2.dp))
            LinearProgressIndicator(
                progress = { progress },
                color = barColor,
                trackColor = Color.Gray.copy(alpha = 0.3f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
        }
    }
}

/**
 * 4. UPGRADES & SKIN SHOP
 * Multi-category custom content terminal to buy characters / rope skins / templates using gathered virtual coin wallets.
 */
@Composable
fun ShopScreen(
    gameManager: GameManager,
    soundManager: SoundManager,
    onBackClicked: () -> Unit
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Skins, 1: Cables, 2: City Themes
    var coinBalanceState by remember { mutableStateOf(gameManager.coinBalance) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E17))
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClicked) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Return home", tint = Color.White)
                }

                Text(
                    text = "CYBERNETIC DEPOT",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                    letterSpacing = 1.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFF1E283A), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Wallet balance",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$coinBalanceState",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Depots Segment Tab Toggles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF151C2C))
                    .padding(4.dp)
            ) {
                val tabs = listOf("CHARACTERS", "ROPE CABLES", "BACKDROPS")
                tabs.forEachIndexed { index, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeTab == index) Color(0xFFFF007F) else Color.Transparent)
                            .clickable {
                                soundManager.playSwingRelease()
                                activeTab = index
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (activeTab == index) Color.White else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Body Lists
            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    0 -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(gameManager.skinsCatalog) { skin ->
                                val isUnlocked = gameManager.isSkinUnlocked(skin.id)
                                val isEquipped = gameManager.selectedSkin == skin.id

                                ShopItemRow(
                                    name = skin.name,
                                    desc = skin.description,
                                    cost = skin.cost,
                                    colorAccent = Color(android.graphics.Color.parseColor(skin.primaryColorHex)),
                                    isUnlocked = isUnlocked,
                                    isEquipped = isEquipped,
                                    onAction = {
                                        if (isUnlocked) {
                                            gameManager.selectedSkin = skin.id
                                            soundManager.playPowerupCollect()
                                        } else {
                                            val success = gameManager.unlockSkin(skin.id)
                                            if (success) {
                                                coinBalanceState = gameManager.coinBalance
                                                gameManager.selectedSkin = skin.id
                                                soundManager.playPowerupCollect()
                                            } else {
                                                soundManager.playHitExplosion() // Failed buzz
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                    1 -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(gameManager.ropeCatalog) { rope ->
                                val isUnlocked = gameManager.isRopeUnlocked(rope.id)
                                val isEquipped = gameManager.selectedRopeStyle == rope.id

                                ShopItemRow(
                                    name = rope.name,
                                    desc = rope.description,
                                    cost = rope.cost,
                                    colorAccent = Color(android.graphics.Color.parseColor(rope.colorHex)),
                                    isUnlocked = isUnlocked,
                                    isEquipped = isEquipped,
                                    onAction = {
                                        if (isUnlocked) {
                                            gameManager.selectedRopeStyle = rope.id
                                            soundManager.playPowerupCollect()
                                        } else {
                                            val success = gameManager.unlockRope(rope.id)
                                            if (success) {
                                                coinBalanceState = gameManager.coinBalance
                                                gameManager.selectedRopeStyle = rope.id
                                                soundManager.playPowerupCollect()
                                            } else {
                                                soundManager.playHitExplosion()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                    2 -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(gameManager.backgroundCatalog) { bg ->
                                val isUnlocked = gameManager.isBackgroundUnlocked(bg.id)
                                val isEquipped = gameManager.selectedBackground == bg.id

                                ShopItemRow(
                                    name = bg.name,
                                    desc = bg.description,
                                    cost = bg.cost,
                                    colorAccent = Color(android.graphics.Color.parseColor(bg.skyColorHex)),
                                    isUnlocked = isUnlocked,
                                    isEquipped = isEquipped,
                                    onAction = {
                                        if (isUnlocked) {
                                            gameManager.selectedBackground = bg.id
                                            soundManager.playPowerupCollect()
                                        } else {
                                            val success = gameManager.unlockBackground(bg.id)
                                            if (success) {
                                                coinBalanceState = gameManager.coinBalance
                                                gameManager.selectedBackground = bg.id
                                                soundManager.playPowerupCollect()
                                            } else {
                                                soundManager.playHitExplosion()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShopItemRow(
    name: String,
    desc: String,
    cost: Int,
    colorAccent: Color,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131A27)),
        border = BorderStroke(1.dp, if (isEquipped) Color(0xFF39FF14) else Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visual swatch + Details
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                // Color swatch
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorAccent)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = desc,
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Action section
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEquipped) Color(0xFF39FF14).copy(alpha = 0.2f)
                                     else if (isUnlocked) Color(0xFF1E283A)
                                     else Color(0xFFFFD700),
                    contentColor = if (isEquipped) Color(0xFF39FF14)
                                   else if (isUnlocked) Color.White
                                   else Color.Black
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.height(38.dp)
            ) {
                if (isEquipped) {
                    Text("EQUIPPED", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                } else if (isUnlocked) {
                    Text("EQUIP", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, "Coins", modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("$cost", fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

/**
 * 5. SETTINGS CONTROL SCREEN
 * Clean diagnostic sliders and sound toggles.
 */
@Composable
fun SettingsScreen(
    gameManager: GameManager,
    soundManager: SoundManager,
    onBackClicked: () -> Unit
) {
    var bgmState by remember { mutableStateOf(soundManager.bgmEnabled) }
    var sfxState by remember { mutableStateOf(soundManager.sfxEnabled) }
    var vibrateState by remember { mutableStateOf(soundManager.vibrateEnabled) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090C12))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClicked) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Exit to Menu", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "SYSTEM SETTINGS",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            }

            // Body selectors
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Background Music Toggle
                SettingsCustomRow(
                    title = "BACKGROUND MUSIC (BGM)",
                    desc = "Loop high-speed cyberpunk dynamic synth arrays.",
                    isActive = bgmState,
                    onToggle = {
                        bgmState = !bgmState
                        soundManager.bgmEnabled = bgmState
                        soundManager.saveSettings()
                        if (bgmState) {
                            soundManager.startBgm()
                        } else {
                            soundManager.stopBgm()
                        }
                    }
                )

                // Sound FX Toggle
                SettingsCustomRow(
                    title = "SOUND EFFECTS (SFX)",
                    desc = "Enable synthesized audio responses for grapple hooks and coins.",
                    isActive = sfxState,
                    onToggle = {
                        sfxState = !sfxState
                        soundManager.sfxEnabled = sfxState
                        soundManager.saveSettings()
                        if (sfxState) soundManager.playSwingShoot()
                    }
                )

                // Vibration Haptic Toggle
                SettingsCustomRow(
                    title = "HAPTIC ENGINES (VIBRATION)",
                    desc = "Trigger kinetic feedback upon landing, crashing, or hooking.",
                    isActive = vibrateState,
                    onToggle = {
                        vibrateState = !vibrateState
                        soundManager.vibrateEnabled = vibrateState
                        soundManager.saveSettings()
                        if (vibrateState) soundManager.vibrate(80)
                    }
                )

                // Reset data safeguard
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF24151B)),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "DIAGNOSTIC SAFEGUARD",
                            color = Color.Red,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Erase all high scores, unlock logs, and clear standard character bank configurations. This cannot be undone.",
                            color = Color.LightGray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val prefs = gameManager.context.getSharedPreferences("RopeHeroSwingPrefs", android.content.Context.MODE_PRIVATE)
                                prefs.edit().clear().apply()
                                soundManager.playHitExplosion()
                                // Sync local state
                                gameManager.currentScore = 0
                                gameManager.coinsCollectedThisRun = 0
                                bgmState = true
                                sfxState = true
                                vibrateState = true
                                soundManager.bgmEnabled = true
                                soundManager.sfxEnabled = true
                                soundManager.vibrateEnabled = true
                                soundManager.saveSettings()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Text("ERASE MEMORY LOG", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            // Footer Central Button
            Button(
                onClick = onBackClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                Text("SAVE & GO BACK", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun SettingsCustomRow(
    title: String,
    desc: String,
    isActive: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131824)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 13.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Switch(
                checked = isActive,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF39FF14),
                    checkedTrackColor = Color(0xFF1E3D23)
                )
            )
        }
    }
}
