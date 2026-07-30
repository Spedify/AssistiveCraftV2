package com.assistivecraft.render;

import com.assistivecraft.ModuleManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * Draws a segmented predicted trajectory for the item currently held by the
 * player (bow, ender pearl, snowball, egg), to help players who struggle with
 * spatial/depth tracking judge where a throw or shot will land.
 */
public class ProjectilePathRenderer {

    private static final int MAX_STEPS = 120;
    private static final float STEP_TIME = 1.0f; // ticks per simulation step

    public static void onWorldRenderLast(WorldRenderContext context) {
        ModuleManager mm = ModuleManager.INSTANCE;
        if (!mm.projectilePathEnabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        var stack = client.player.getMainHandStack();
        ProjectileProfile profile = profileFor(stack);
        if (profile == null) return;

        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;
        Vec3d cameraPos = context.camera().getPos();

        Vec3d pos = client.player.getCameraPosVec(1.0f);
        Vec3d look = client.player.getRotationVec(1.0f);
        Vec3d velocity = look.multiply(profile.initialSpeed);

        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
        RenderSystem.disableDepthTest();
        VertexConsumer buffer = immediate.getBuffer(RenderLayer.getLines());

        Vec3d prev = pos;
        for (int i = 0; i < MAX_STEPS; i++) {
            Vec3d next = prev.add(velocity);
            velocity = velocity.multiply(profile.drag).subtract(0, profile.gravity, 0);

            HitResult hit = client.world.raycast(new RaycastContext(
                    prev, next, RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE, client.player));

            Vec3d segmentEnd = (hit.getType() != HitResult.Type.MISS) ? hit.getPos() : next;

            drawSegment(matrices, buffer,
                    prev.subtract(cameraPos), segmentEnd.subtract(cameraPos),
                    0.95f, 0.85f, 0.1f);

            if (hit.getType() != HitResult.Type.MISS) break;
            prev = next;
        }

        immediate.draw();
        RenderSystem.enableDepthTest();
    }

    private static void drawSegment(MatrixStack matrices, VertexConsumer buffer,
                                     Vec3d from, Vec3d to, float r, float g, float b) {
        var entry = matrices.peek();
        Vec3d dir = to.subtract(from).normalize();
        buffer.vertex(entry.getPositionMatrix(), (float) from.x, (float) from.y, (float) from.z)
                .color(r, g, b, 1.0f)
                .normal(entry.getNormalMatrix(), (float) dir.x, (float) dir.y, (float) dir.z)
                .next();
        buffer.vertex(entry.getPositionMatrix(), (float) to.x, (float) to.y, (float) to.z)
                .color(r, g, b, 1.0f)
                .normal(entry.getNormalMatrix(), (float) dir.x, (float) dir.y, (float) dir.z)
                .next();
    }

    private static ProjectileProfile profileFor(net.minecraft.item.ItemStack stack) {
        if (stack.getItem() instanceof RangedWeaponItem) {
            // Fully-drawn bow approximation; real draw-time scaling can be added later.
            return new ProjectileProfile(3.0, 0.99, 0.05);
        }
        if (stack.isOf(Items.ENDER_PEARL) || stack.isOf(Items.SNOWBALL) || stack.isOf(Items.EGG)) {
            return new ProjectileProfile(1.5, 0.99, 0.03);
        }
        return null;
    }

    private record ProjectileProfile(double initialSpeed, double drag, double gravity) {}
}
