package com.shinapri.bulkfarming.Client;

import com.shinapri.bulkfarming.Common.Action;
import com.shinapri.bulkfarming.Utils.PosPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.api.ClientModInitializer;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
//import net.minecraft.client.render.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionResult;



public class BulkFarmingClient implements ClientModInitializer {
    private static KeyMapping KEY_ACTIVE;
    private boolean prevLmb = false, prevRmb = false;
    private long lastClickMs = 0;
    private static final long CLICK_COOLDOWN_MS = 120;
    private boolean selectionMode = false;

    @Override public void onInitializeClient() {
        KEY_ACTIVE = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "Activate Bulk Farming Key",
                GLFW.GLFW_KEY_LEFT_SHIFT,
                "Bulk Farming"
        ));
/*
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            return selectionMode ? InteractionResult.FAIL : InteractionResult.PASS;
        });*/

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) ->
                selectionMode ? InteractionResult.FAIL : InteractionResult.PASS
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client == null || client.level == null || client.isPaused() || client.screen != null) return;

            long window = client.getWindow().getWindow();

            boolean shiftHeld = KEY_ACTIVE.isDown() || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT);
            selectionMode = shiftHeld;

            boolean lmb = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            boolean rmb = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

            boolean leftMouseClick = lmb && !prevLmb;
            boolean rmbClicked = rmb && !prevRmb;
            prevLmb = lmb;
            prevRmb = rmb;

            if (!(shiftHeld && (leftMouseClick || rmbClicked))) return;
            long now = net.minecraft.Util.getMillis();
            if (now - lastClickMs < CLICK_COOLDOWN_MS) return;
            lastClickMs = now;

            if(client.player == null) return;
            ItemStack currentItem = client.player.getMainHandItem();

            boolean isSeedLike =
                    currentItem.is(Items.WHEAT_SEEDS)     ||
                            currentItem.is(Items.BEETROOT_SEEDS)  ||
                            currentItem.is(Items.CARROT)          ||
                            currentItem.is(Items.POTATO)          ||
                            //currentItem.is(Items.MELON_SEEDS)     ||
                            //currentItem.is(Items.PUMPKIN_SEEDS)   ||
                            currentItem.is(Items.TORCHFLOWER_SEEDS) ||
                            currentItem.is(Items.PITCHER_POD);

            Minecraft mc = Minecraft.getInstance();
            ClientLevel world = mc.level;
            if (world == null) return;
            BlockPos hit;

            if(currentItem.getItem() instanceof HoeItem){
                hit = getLookedBlock(client);
                if (hit == null) return;

                BlockState blockState   = world.getBlockState(hit);

                if(blockState.is(Blocks.FARMLAND)){
                    ClientPlayNetworking.send(new PosPayload(hit, Action.HARVEST));
                }else if( world.getBlockState(hit.below()).is(Blocks.FARMLAND)){
                    ClientPlayNetworking.send(new PosPayload(hit.below(), Action.HARVEST));
                }else{
                    return;
                }

            }else if(isSeedLike){
                if(rmbClicked){
                    hit = getLookedBlock(client);
                    if (hit == null) return;
                    ClientPlayNetworking.send(new PosPayload(hit, Action.PLANT));
                    /*client.gui.setOverlayMessage(
                            Component.literal("PLANT"), false
                    );*/
                }
            }



        });
    }
    private static BlockPos getLookedBlock(Minecraft client) {
        HitResult target = client.hitResult;
        if (!(target instanceof BlockHitResult bhr)) return null;
        return bhr.getBlockPos();
    }
}
