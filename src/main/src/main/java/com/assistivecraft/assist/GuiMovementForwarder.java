package com.assistivecraft.assist;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Keeps WASD/jump responsive while an inventory, chest, or other HandledScreen
 * is open, by reading the real physical key state each tick and forwarding it
 * to the movement keybinds. No bytecode injection required.
 */
public class GuiMovementForwarder {

    public static void onClientTick(MinecraftClient client) {
        if (client.player == null) return;
        if (!(client.currentScreen instanceof HandledScreen<?>)) return;

        long handle = client.getWindow().getHandle();

        setFromRawKey(client, handle, client.options.forwardKey);
        setFromRawKey(client, handle, client.options.backKey);
        setFromRawKey(client, handle, client.options.leftKey);
        setFromRawKey(client, handle, client.options.rightKey);
        setFromRawKey(client, handle, client.options.jumpKey);
    }

    private static void setFromRawKey(MinecraftClient client, long handle, net.minecraft.client.option.KeyBinding binding) {
        InputUtil.Key boundKey = InputUtil.fromTranslationKey(binding.getBoundKeyTranslationKey());
        if (boundKey.getCategory() != InputUtil.Type.KEYSYM) return; // skip mouse-bound keys

        boolean pressed = GLFW.glfwGetKey(handle, boundKey.getCode()) == GLFW.GLFW_PRESS;
        binding.setPressed(pressed);
    }
}
