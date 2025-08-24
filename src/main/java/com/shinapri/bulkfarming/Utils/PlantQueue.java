package com.shinapri.bulkfarming.Utils;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class PlantQueue {
    // งานหนึ่งชิ้น = world ไหน + ตำแหน่งไหน
    public record Job(UUID playerId, ResourceKey<Level> dim, BlockPos pos) {}
    enum HarvestResult { OK, NO_CROP, INVENTORY_BLOCKED }

    private static final Deque<Job> Q = new ArrayDeque<>();
    private static final int BATCH_PER_TICK = 64;   // ปรับตามใจ

    private PlantQueue() {}

    public static void enqueue(ServerPlayer player, Collection<BlockPos> positions) {
        var dim = player.level().dimension();
        UUID id = player.getUUID();
        for (BlockPos p : positions) Q.addLast(new Job(id, dim, p.immutable()));
    }
    public static void enqueue(ServerPlayer player, BlockPos p) {
        Q.addLast(new Job(player.getUUID(),
                player.level().dimension(), p.immutable()));
    }

    // เรียกทุก tick (END_SERVER_TICK)
    public static void tick(MinecraftServer server) {
        ServerPlayer player;
        BlockPos p;
        Job job;
        ServerLevel level;
        BlockState currentState;
        ItemStack stack;

        for (int i = 0; i < BATCH_PER_TICK && !Q.isEmpty(); i++) {

            job = Q.pollFirst();
            level = server.getLevel(job.dim);

            if (level == null) continue;

            p = job.pos;
            if (!level.isLoaded(p)) continue;

            player = server.getPlayerList().getPlayer(job.playerId);
            if(player == null) continue;
            /*player.displayClientMessage(
                    Component.literal("PLANT HAAAA!!"),
                    false
            );*/

            stack = player.getMainHandItem();
            currentState = level.getBlockState(p);
            BlockPos target = p.above();

            var hit = new net.minecraft.world.phys.BlockHitResult(
                    new net.minecraft.world.phys.Vec3(p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5),
                    net.minecraft.core.Direction.UP,
                    p,
                    false
            );

// ลองมือหลักก่อน
            var res = player.gameMode.useItemOn(
                    player, level, player.getMainHandItem(),
                    net.minecraft.world.InteractionHand.MAIN_HAND, hit
            );

            if (!res.consumesAction()) {
                player.gameMode.useItemOn(
                        player, level, player.getOffhandItem(),
                        net.minecraft.world.InteractionHand.OFF_HAND, hit
                );
            }

        }
    }

}
