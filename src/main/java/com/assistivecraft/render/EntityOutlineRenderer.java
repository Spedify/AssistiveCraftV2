package com.assistivecraft.render;

import com.assistivecraft.ModuleManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
/**
 * Draws high-contrast bounding-box outlines around living entities so
 * low-vision players can locate mobs through foliage, fog, or dense terrain.
 */
public class EntityOutlineRenderer {

    // range now pulled from ModuleManager.mobOutlineRange

    public static void onWorldRenderLast(WorldRenderContext context) {
        ModuleManager mm = ModuleManager.INSTANCE;
        if (!mm.mobOutlineEnabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        Vec3d cameraPos = context.camera().getPos();
        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;

        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Box searchBox = client.player.getBoundingBox().expand(mm.mobOutlineRange);
        for (LivingEntity entity : client.world.getEntitiesByClass(
                LivingEntity.class, searchBox, e -> e != client.player && e.isAlive())) {

            Box box = entity.getBoundingBox().offset(
                    -cameraPos.x, -cameraPos.y, -cameraPos.z);

            float[] color = colorFor(entity);
            VertexConsumer buffer = immediate.getBuffer(RenderLayer.getLines());
            drawBoxOutline(matrices, buffer, box, color[0], color[1], color[2], 1.0f);
        }

        immediate.draw();
        RenderSystem.enableDepthTest();
    }

    private static float[] colorFor(LivingEntity entity) {
        // Hostile mobs render red, everything else (passive/neutral) renders cyan,
        // chosen for high contrast against most biomes.
        if (entity instanceof HostileEntity) {
            return new float[]{1.0f, 0.15f, 0.15f};
        }
        return new float[]{0.1f, 0.9f, 1.0f};
    }

    private static void drawBoxOutline(MatrixStack matrices, VertexConsumer buffer, Box box,
                                        float r, float g, float b, float a) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        WorldRenderer.drawBox(matrices, buffer,
                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                r, g, b, a);
    }
}
