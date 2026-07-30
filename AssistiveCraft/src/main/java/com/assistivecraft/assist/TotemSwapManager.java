package com.assistivecraft.assist;

import com.assistivecraft.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

/**
 * If the offhand is empty (or not a Totem of Undying) and the player is
 * carrying one in their main inventory, moves it into the offhand slot
 * automatically. Intended for players who may not react fast enough to
 * manually swap a totem in an emergency.
 */
public class TotemSwapManager {

    private static final int OFFHAND_SLOT_ID = 45; // player screen slot index for offhand

    private static int cooldownTicks = 0;

    public static void onClientTick(MinecraftClient client) {
        ModuleManager mm = ModuleManager.INSTANCE;
        if (!mm.autoTotemEnabled) return;
        if (client.player == null || client.interactionManager == null) return;
        if (client.currentScreen != null) return; // don't fiddle with slots while a screen is open

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        ClientPlayerEntity player = client.player;
        ItemStack offhand = player.getOffHandStack();
        if (offhand.isOf(Items.TOTEM_OF_UNDYING)) return;

        int totemSlot = findTotemInMainInventory(player);
        if (totemSlot < 0) return;

        // Standard shift-less slot swap: pick up the totem, place in offhand,
        // put anything that was there back. Uses the same handled-screen
        // slot click logic the vanilla inventory GUI itself would issue.
        client.interactionManager.clickSlot(player.playerScreenHandler.syncId,
                totemSlot, 0, SlotActionType.PICKUP, player);
        client.interactionManager.clickSlot(player.playerScreenHandler.syncId,
                OFFHAND_SLOT_ID, 0, SlotActionType.PICKUP, player);
        if (!player.currentScreenHandler.getCursorStack().isEmpty()) {
            client.interactionManager.clickSlot(player.playerScreenHandler.syncId,
                    totemSlot, 0, SlotActionType.PICKUP, player);
        }

        cooldownTicks = 20; // avoid re-triggering every tick while state settles
    }

    private static int findTotemInMainInventory(ClientPlayerEntity player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.main.size(); i++) {
            if (inventory.main.get(i).isOf(Items.TOTEM_OF_UNDYING)) {
                // player.playerScreenHandler slot indices: 9-35 main, 36-39 armor,
                // hotbar is 0-8 mapped separately; getInventory index -> handler slot:
                return i < 9 ? i + 36 : i;
            }
        }
        return -1;
    }
}
