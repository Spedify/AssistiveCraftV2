package com.assistivecraft.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class HandledScreenMixin {

    @Inject(method = "keyPressed", at = @At("HEAD"))
    private void assistivecraft$forwardMovementKeyPress(int keyCode, int scanCode, int modifiers, CallbackInfo ci) {
        if ((Object) this instanceof HandledScreen<?>) forwardToMovementOptions(keyCode, true);
    }

    @Inject(method = "keyReleased", at = @At("HEAD"))
    private void assistivecraft$forwardMovementKeyRelease(int keyCode, int scanCode, int modifiers, CallbackInfo ci) {
        if ((Object) this instanceof HandledScreen<?>) forwardToMovementOptions(keyCode, false);
    }

    private void forwardToMovementOptions(int keyCode, boolean pressed) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options == null) return;

        if (client.options.forwardKey.matchesKey(keyCode, -1)) client.options.forwardKey.setPressed(pressed);
        else if (client.options.backKey.matchesKey(keyCode, -1)) client.options.backKey.setPressed(pressed);
        else if (client.options.leftKey.matchesKey(keyCode, -1)) client.options.leftKey.setPressed(pressed);
        else if (client.options.rightKey.matchesKey(keyCode, -1)) client.options.rightKey.setPressed(pressed);
        else if (client.options.jumpKey.matchesKey(keyCode, -1)) client.options.jumpKey.setPressed(pressed);
    }
}
