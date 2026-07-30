package com.assistivecraft.assist;

import com.assistivecraft.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;

/**
 * Watches hunger level and, once it drops below the configured threshold,
 * switches to a food item on the hotbar and holds the use-item key until
 * satiated. Intended for players who may lose track of hunger management
 * due to attention or motor difficulties.
 */
public class AutoEatManager {

    private static boolean eating = false;
    private static int previousSelectedSlot = -1;

    public static void onClientTick(MinecraftClient client) {
        ModuleManager mm = ModuleManager.INSTANCE;
        if (client.player == null) return;

        if (!mm.autoEatEnabled) {
            stopEatingIfActive(client);
            return;
        }
        if (client.currentScreen != null) return;

        ClientPlayerEntity player = client.player;

        if (eating) {
            if (player.getHungerManager().getFoodLevel() < 20 &&
                    player.isUsingItem() &&
                    player.getMainHandStack().getComponents().contains(DataComponentTypes.FOOD)) {
                return; // keep holding use key until done chewing / full
            }
            stopEatingIfActive(client);
            return;
        }

        if (player.getHungerManager().getFoodLevel() >= mm.autoEatHungerThreshold) return;

        int foodSlot = findFoodInHotbar(player);
        if (foodSlot < 0) return;

        previousSelectedSlot = player.getInventory().selectedSlot;
        player.getInventory().selectedSlot = foodSlot;
        client.options.useKey.setPressed(true);
        eating = true;
    }

    private static void stopEatingIfActive(MinecraftClient client) {
        if (!eating) return;
        client.options.useKey.setPressed(false);
        if (previousSelectedSlot >= 0 && client.player != null) {
            client.player.getInventory().selectedSlot = previousSelectedSlot;
        }
        eating = false;
        previousSelectedSlot = -1;
    }

    private static int findFoodInHotbar(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().main.get(i);
            if (!stack.isEmpty() && stack.getComponents().contains(DataComponentTypes.FOOD)) {
                return i;
            }
        }
        return -1;
    }
}
