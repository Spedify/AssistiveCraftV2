package com.assistivecraft;

import com.assistivecraft.assist.AutoEatManager;
import com.assistivecraft.assist.CombatAssistManager;
import com.assistivecraft.assist.FallMitigationManager;
import com.assistivecraft.assist.TotemSwapManager;
import com.assistivecraft.render.EntityOutlineRenderer;
import com.assistivecraft.render.OreOutlineRenderer;
import com.assistivecraft.render.ProjectilePathRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public class AssistiveCraftClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KeyBindings.register();

        // --- World render hooks (visual assist) ---
        WorldRenderEvents.LAST.register(EntityOutlineRenderer::onWorldRenderLast);
        WorldRenderEvents.LAST.register(ProjectilePathRenderer::onWorldRenderLast);
        WorldRenderEvents.LAST.register(OreOutlineRenderer::onWorldRenderLast);

        // --- Client tick hooks (motor / utility assist) ---
        ClientTickEvents.END_CLIENT_TICK.register(CombatAssistManager::onClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(TotemSwapManager::onClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(AutoEatManager::onClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(FallMitigationManager::onClientTick);
    }
}
