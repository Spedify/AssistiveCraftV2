package com.assistivecraft.assist;

import com.assistivecraft.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;

/**
 * Softens dangerous falls by capping downward velocity shortly before ground
 * contact, similar in spirit to vanilla Slow Falling. This only ever reduces
 * an existing downward velocity (never adds upward motion or ignores terrain)
 * and only fires within a short distance of the ground.
 *
 * NOTE: this works reliably in singleplayer/integrated-server play, where the
 * client's own physics are authoritative. On a dedicated multiplayer server
 * the server recalculates fall damage independently, so this same technique
 * is what many servers' anti-cheat systems flag as "NoFall" and will reject
 * or punish. Keep this module singleplayer-only.
 */
public class FallMitigationManager {

    private static final double DANGEROUS_FALL_SPEED = -0.85; // roughly the point damage starts scaling up
    private static final double SCAN_DISTANCE = 3.0;
    private static final double MAX_SAFE_SPEED = -0.6;

    public static void onClientTick(MinecraftClient client) {
        ModuleManager mm = ModuleManager.INSTANCE;
        if (!mm.fallMitigationEnabled) return;

        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) return;
        if (player.isOnGround() || player.isFallFlying() || player.getAbilities().flying) return;

        Vec3d velocity = player.getVelocity();
        if (velocity.y >= DANGEROUS_FALL_SPEED) return; // not falling dangerously fast

        if (!nearingGroundWithin(client, player, SCAN_DISTANCE)) return;

        // Only ever clamp the existing downward speed upward toward a safer
        // value; never push the player up or sideways.
        double clampedY = Math.max(velocity.y, MAX_SAFE_SPEED);
        player.setVelocity(velocity.x, clampedY, velocity.z);
    }

    private static boolean nearingGroundWithin(MinecraftClient client, ClientPlayerEntity player, double distance) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        BlockPos base = player.getBlockPos();
        for (int dy = 1; dy <= distance; dy++) {
            pos.set(base.getX(), base.getY() - dy, base.getZ());
            VoxelShape shape = client.world.getBlockState(pos).getCollisionShape(client.world, pos);
            if (!shape.isEmpty()) return true;
        }
        return false;
    }
}
