package com.example

import android.content.Context
import android.content.SharedPreferences

class GameManager(val context: Context) {

    private val prefsName = "RopeHeroSwingPrefs"
    private val prefs: SharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    // Persistent values
    var highScore: Int
        get() = prefs.getInt("high_score", 0)
        set(value) = prefs.edit().putInt("high_score", value).apply()

    var coinBalance: Int
        get() = prefs.getInt("coin_balance", 0)
        set(value) = prefs.edit().putInt("coin_balance", value).apply()

    var selectedSkin: String
        get() = prefs.getString("selected_skin", "neon_ninja") ?: "neon_ninja"
        set(value) = prefs.edit().putString("selected_skin", value).apply()

    var selectedRopeStyle: String
        get() = prefs.getString("selected_rope", "laser_rope") ?: "laser_rope"
        set(value) = prefs.edit().putString("selected_rope", value).apply()

    var selectedBackground: String
        get() = prefs.getString("selected_bg", "cyber_night") ?: "cyber_night"
        set(value) = prefs.edit().putString("selected_bg", value).apply()

    // Temporary game session state
    var currentScore: Int = 0
    var coinsCollectedThisRun: Int = 0
    var isGameOver = false
    var isPaused = false
    var currentSpeedMultiplier = 1.0f

    // On-screen button input states
    var isLeftButtonPressed = false
    var isRightButtonPressed = false

    // Powerup screen scale indicator
    var shieldTimeRemainingMs: Long = 0L
    var slowMoTimeRemainingMs: Long = 0L
    var magnetTimeRemainingMs: Long = 0L
    var boostTimeRemainingMs: Long = 0L

    // Shop Item Declarations
    data class ShopSkin(
        val id: String, 
        val name: String, 
        val cost: Int, 
        val primaryColorHex: String, 
        val secondaryColorHex: String,
        val description: String
    )

    val skinsCatalog = listOf(
        ShopSkin("neon_ninja", "Neon Ninja", 0, "#39FF14", "#FF073A", "Standard cybernetic parkour runner with green plasma visor."),
        ShopSkin("cyber_cyberpunk", "Cyber Neon", 150, "#00FFFF", "#FF00FF", "Vibrant futuristic style with hot magenta and electric cyan highlights."),
        ShopSkin("pixel_retro", "Pixel 8-Bit", 300, "#FF7F00", "#00FF7F", "Retro arcade design utilizing clean vintage orange and mint colors."),
        ShopSkin("gold_hero", "Solid Gold", 500, "#FFE700", "#FFFFFF", "Elite champion armor forged with radiant solid gold filaments."),
        ShopSkin("shadow_specter", "Shadow Specter", 800, "#9400D3", "#500050", "Stealth infiltrator equipped with dark energy waves."),
        ShopSkin("crimson_phoenix", "Solar Phoenix", 1200, "#FF1493", "#FF8C00", "Forged in nuclear fusion, carrying beautiful crimson solar flares.")
    )

    data class ShopRope(
        val id: String,
        val name: String,
        val cost: Int,
        val colorHex: String,
        val description: String
    )

    val ropeCatalog = listOf(
        ShopRope("laser_rope", "Neon Cable", 0, "#39FF14", "Standard glowing high-tensile energy cable."),
        ShopRope("plasma_beam", "Plasma Beam", 100, "#00FFFF", "An electric plasma field bound into rope form."),
        ShopRope("lava_strand", "Lava Strand", 250, "#FF4500", "Molten thermite fiber that illuminates dark spaces."),
        ShopRope("chrono_wire", "Chrono Wire", 400, "#9D00FF", "Quantum tether crafted from chronostatic filaments.")
    )

    data class ShopBackground(
        val id: String,
        val name: String,
        val cost: Int,
        val skyColorHex: String,
        val description: String
    )

    val backgroundCatalog = listOf(
        ShopBackground("cyber_night", "Cyber Tokyo", 0, "#0B0E14", "Deep dark Tokyo cyberpunk night sky with purple clouds."),
        ShopBackground("solar_cloud", "Solar Sunset", 150, "#220D1A", "Beautiful neon violet with crimson and warm amber overlays."),
        ShopBackground("matrix_green", "Neon Grid", 300, "#051A05", "Virtual system grid themed on green simulation matrices.")
    )

    /**
     * Checks if a skin/rope/background is unlocked.
     * Free items are always unlocked. Unlocked purchases are stored in preferences.
     */
    fun isSkinUnlocked(skinId: String): Boolean {
        if (skinId == "neon_ninja") return true
        return prefs.getBoolean("unlocked_skin_$skinId", false)
    }

    fun unlockSkin(skinId: String): Boolean {
        val skin = skinsCatalog.firstOrNull { it.id == skinId } ?: return false
        if (coinBalance >= skin.cost) {
            coinBalance -= skin.cost
            prefs.edit().putBoolean("unlocked_skin_$skinId", true).apply()
            return true
        }
        return false
    }

    fun isRopeUnlocked(ropeId: String): Boolean {
        if (ropeId == "laser_rope") return true
        return prefs.getBoolean("unlocked_rope_$ropeId", false)
    }

    fun unlockRope(ropeId: String): Boolean {
        val rope = ropeCatalog.firstOrNull { it.id == ropeId } ?: return false
        if (coinBalance >= rope.cost) {
            coinBalance -= rope.cost
            prefs.edit().putBoolean("unlocked_rope_$ropeId", true).apply()
            return true
        }
        return false
    }

    fun isBackgroundUnlocked(bgId: String): Boolean {
        if (bgId == "cyber_night") return true
        return prefs.getBoolean("unlocked_bg_$bgId", false)
    }

    fun unlockBackground(bgId: String): Boolean {
        val bg = backgroundCatalog.firstOrNull { it.id == bgId } ?: return false
        if (coinBalance >= bg.cost) {
            coinBalance -= bg.cost
            prefs.edit().putBoolean("unlocked_bg_$bgId", true).apply()
            return true
        }
        return false
    }

    /**
     * Resets the run variables on a new game.
     */
    fun startNewRun() {
        currentScore = 0
        coinsCollectedThisRun = 0
        isGameOver = false
        isPaused = false
        currentSpeedMultiplier = 1.0f
        
        shieldTimeRemainingMs = 0L
        slowMoTimeRemainingMs = 0L
        magnetTimeRemainingMs = 0L
        boostTimeRemainingMs = 0L
    }

    /**
     * Saves the current run stats to persistent storage when the run ends.
     * Updates High Score and registers the acquired coins into the ledger.
     */
    fun endRunAndSave() {
        if (currentScore > highScore) {
            highScore = currentScore
        }
        coinBalance += coinsCollectedThisRun
        isGameOver = true
    }
}
