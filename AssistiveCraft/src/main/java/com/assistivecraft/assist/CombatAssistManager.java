package com.assistivecraft.assist;

import com.assistivecraft.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;

/**
 * Two related motor-accessibility aids, both opt-in and both mirroring the
 * player's own inputs rather than acting on their own:
 *
 *  - Crosshair Assist: gently pulls the camera toward the nearest hostile
 *    mob when it's already within a small cone in front of the player, for
 *    players with tremor or reduced fine motor control.
 *  - Attack Sync: only fires the existing attack key once the vanilla
 *    attack-cooldown bar is full AND a mob is directly targeted, so a
 *    player with delayed reflexes doesn't waste swings on a weak hit.
 */
public class CombatAssistManager {

    private static final double AIM_ASSIST_SCAN_RANGE = 32.0;
    private static boolean attackKeyHeldByAssist = false;

    public static void onClientTick(MinecraftClient client) {
        ModuleManager mm = ModuleManager.INSTANCE;
        if (client.player == null || client.world == null) return;
        if (client.currentScreen != null) return; // never act while a menu/inventory is open

        LivingEntity target = null;

        if (mm.aimAssistEnabled) {
            target = findNearestHostileInCone(client, mm.aimAssistFovDegrees, AIM_ASSIST_SCAN_RANGE);
            if (target != null) {
                applyAimAssist(client, target, mm.aimAssistSpeed);
            }
        }

        if (mm.autoAttackEnabled) {
            applyAutoAttack(client, mm.autoAttackRange);
        } else if (attackKeyHeldByAssist) {
            client.options.attackKey.setPressed(false);
            attackKeyHeldByAssist = false;
        }
    }

    private static LivingEntity findNearestHostileInCone(MinecraftClient client, float fovDegrees, double range) {
        ClientPlayerEntity player = client.player;
        Vec3d eyePos = player.getCameraPosVec(1.0f);
        Vec3d look = player.getRotationVec(1.0f);
        double cosThreshold = Math.cos(Math.toRadians(fovDegrees));

        return client.world.getEntitiesByClass(HostileEntity.class,
                        player.getBoundingBox().expand(range),
                        e -> e.isAlive())
                .stream()
                .filter(e -> {
                    Vec3d toEntity = e.getBoundingBox().getCenter().subtract(eyePos).normalize();
                    return toEntity.dotProduct(look) >= cosThreshold;
                })
                .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(player)))
                .orElse(null);
    }

    private static void applyAimAssist(MinecraftClient client, LivingEntity target, float speed) {
        ClientPlayerEntity player = client.player;
        Vec3d eyePos = player.getCameraPosVec(1.0f);
        Vec3d toTarget = target.getBoundingBox().getCenter().subtract(eyePos);

        double dx = toTarget.x, dy = toTarget.y, dz = toTarget.z;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        float desiredYaw = (float) (MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        float desiredPitch = (float) -(MathHelper.atan2(dy, horizontalDist) * (180.0 / Math.PI));

        float newYaw = MathHelper.lerpAngleDegrees(speed, player.getYaw(), desiredYaw);
        float newPitch = MathHelper.clamp(
                MathHelper.lerp(speed, player.getPitch(), desiredPitch), -90.0f, 90.0f);

        player.setYaw(newYaw);
        player.setPitch(newPitch);
    }

    private static void applyAutoAttack(MinecraftClient client, double interactionDistance) {
        ClientPlayerEntity player = client.player;

        if (!(client.crosshairTarget instanceof EntityHitResult entityHit)) return;
        if (client.crosshairTarget.getType() != HitResult.Type.ENTITY) return;
        Entity targetEntity = entityHit.getEntity();
        if (!(targetEntity instanceof LivingEntity)) return;
        if (player.squaredDistanceTo(targetEntity) > interactionDistance * interactionDistance) return;

        float cooldown = player.getAttackCooldownProgress(0.5f);
        if (cooldown >= 1.0f) {
            // Mirrors the player's own bound attack key for a single tick
            // (press then release) rather than sending a raw packet directly,
            // so all normal client-side checks and the vanilla cooldown still apply.
            client.options.attackKey.setPressed(true);
            attackKeyHeldByAssist = true;
        } else if (attackKeyHeldByAssist) {
            client.options.attackKey.setPressed(false);
            attackKeyHeldByAssist = false;
        }
    }
}
