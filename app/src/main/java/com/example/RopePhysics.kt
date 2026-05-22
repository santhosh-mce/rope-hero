package com.example

import kotlin.math.sqrt

class RopePhysics {

    var isAttached: Boolean = false
        private set

    var anchorX: Float = 0f
        private set
    var anchorY: Float = 0f
        private set
    var ropeLength: Float = 0f
        private set

    /**
     * Attaches the rope to a specific coordinate point in the screen space.
     * Calculates the resting length of the rope based on the player's current position.
     */
    fun attach(playerX: Float, playerY: Float, targetAnchorX: Float, targetAnchorY: Float) {
        anchorX = targetAnchorX
        anchorY = targetAnchorY
        val dx = playerX - anchorX
        val dy = playerY - anchorY
        ropeLength = sqrt(dx * dx + dy * dy)
        isAttached = true
    }

    /**
     * Detaches the rope, returning the player to free fall mode.
     */
    fun detach() {
        isAttached = false
    }

    /**
     * Solves the pendulum tension constraint.
     * Computes the new position and velocity of the player to maintain the fixed rope length constraint.
     * Returns true if active, or false if not attached.
     */
    fun updatePhysics(
        playerX: Float,
        playerY: Float,
        vx: Float,
        vy: Float,
        gravity: Float,
        windResistance: Float = 0.995f
    ): PhysicsResult {
        if (!isAttached) {
            return PhysicsResult(playerX, playerY, vx, vy)
        }

        // 1. Calculate relative displacement vector from anchor to player
        var dx = playerX - anchorX
        var dy = playerY - anchorY
        val currentDist = sqrt(dx * dx + dy * dy)

        if (currentDist <= 1.0f) {
            return PhysicsResult(playerX, playerY, vx, vy)
        }

        // Normalize direction vector from anchor to player
        val ux = dx / currentDist
        val uy = dy / currentDist

        // 2. Apply gravity and wind resistance to current velocities
        var currentVx = vx
        var currentVy = vy + gravity
        currentVx *= windResistance
        currentVy *= windResistance

        // 3. Project current position onto the rope constraint circle (distance = ropeLength)
        val constraintX = anchorX + ux * ropeLength
        val constraintY = anchorY + uy * ropeLength

        // 4. Resolve the velocity component:
        // When attached to a rope, the player cannot move radially (along the direction of the rope).
        // We project the velocity onto the tangent vector (orthogonal to the rope) to simulate circular motion.
        // Tangent vector is (-uy, ux) or (uy, -ux)
        val tx = -uy
        val ty = ux

        // Dot product of velocity and tangent vector to find tangential speed
        val dotTangent = currentVx * tx + currentVy * ty

        // The new velocity is entirely tangential
        val resolvedVx = tx * dotTangent
        val resolvedVy = ty * dotTangent

        // Add a tiny bit of gravity-based swing pump (accelerates downward swings to make search fun)
        val swingPump = if (uy > 0) 0.15f else 0.0f
        val enhancedVx = resolvedVx + (if (resolvedVx >= 0) swingPump else -swingPump)

        return PhysicsResult(
            newX = constraintX,
            newY = constraintY,
            newVx = enhancedVx,
            newVy = resolvedVy
        )
    }
}

data class PhysicsResult(
    val newX: Float,
    val newY: Float,
    val newVx: Float,
    val newVy: Float
)
