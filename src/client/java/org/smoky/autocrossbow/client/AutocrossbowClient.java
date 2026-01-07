package org.smoky.autocrossbow.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public class AutocrossbowClient implements ClientModInitializer {

    private static KeyBinding toggleKey;
    private static boolean enabled = true;

    private static int swapCooldown = 0;

    @Override
    public void onInitializeClient() {

        toggleKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.autocrossbow.toggle",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_R,
                        Category.MISC
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            if (swapCooldown > 0) {
                swapCooldown--;
            }

            while (toggleKey.wasPressed()) {
                enabled = !enabled;
                if (client.player != null) {
                    client.player.sendMessage(
                            Text.literal("AutoCrossbow: " + (enabled ? "ON" : "OFF")),
                            true
                    );
                }
            }

            if (enabled && swapCooldown == 0) {
                tryAutoSwap(client);
            }
        });
    }

    private void tryAutoSwap(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) return;

        ItemStack mainHand = client.player.getStackInHand(Hand.MAIN_HAND);

        if (!(mainHand.getItem() instanceof CrossbowItem)) return;
        if (CrossbowItem.isCharged(mainHand)) return;

        int chargedSlot = findChargedCrossbow(client);
        if (chargedSlot == -1) return;

        swapToCharged(client, chargedSlot);
    }

    private int findChargedCrossbow(MinecraftClient client) {
        int selected = client.player.getInventory().getSelectedSlot();

        for (int i = 0; i < 9; i++) {
            if (i == selected) continue;
            ItemStack stack = client.player.getInventory().getStack(i);
            if (isChargedCrossbow(stack)) {
                return i;
            }
        }

        for (int i = 9; i < client.player.getInventory().size(); i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (isChargedCrossbow(stack)) {
                return i;
            }
        }

        return -1;
    }

    private boolean isChargedCrossbow(ItemStack stack) {
        return stack.getItem() instanceof CrossbowItem
                && CrossbowItem.isCharged(stack);
    }

    private void swapToCharged(MinecraftClient client, int inventorySlot) {
        int selectedHotbar = client.player.getInventory().getSelectedSlot();

        client.interactionManager.clickSlot(
                client.player.currentScreenHandler.syncId,
                inventorySlot,
                selectedHotbar,
                SlotActionType.SWAP,
                client.player
        );

        swapCooldown = 6;
    }
}
