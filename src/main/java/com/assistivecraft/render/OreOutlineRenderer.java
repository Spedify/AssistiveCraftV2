package com.assistivecraft.render;

import com.assistivecraft.ModuleManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Highlights nearby diamond ore (and deepslate variant) so low-vision players
 * can spot valuable blocks in dim or low-contrast cave lighting.
 */
public class OreOutlineRenderer {

    public static void onWorldRenderLast(WorldRenderContext context) {
        ModuleManager mm = ModuleManager.INSTANCE;
        if (!mm.diamondOutlineEnabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;

        Vec3d cameraPos = context.camera().getPos();
        int radius = mm.diamondOutlineRadius;
        BlockPos center = client.player.getBlockPos();

        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
        RenderSystem.disableDepthTest();
        VertexConsumer buffer = immediate.getBuffer(RenderLayer.getLines());

        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    var state = client.world.getBlockState(pos);
                    if (state.isOf(Blocks.DIAMOND_ORE) || state.isOf(Blocks.DEEPSLATE_DIAMOND_ORE)) {
                        Box box = new Box(pos).offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
                        WorldRenderer.drawBox(matrices, buffer,
                                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                                0.2f, 0.8f, 1.0f, 1.0f);
                    }
                }
            }
        }

        immediate.draw();
        RenderSystem.enableDepthTest();
    }
}
