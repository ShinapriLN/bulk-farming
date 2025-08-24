package com.shinapri.bulkfarming;

import com.shinapri.bulkfarming.Common.Action;

import com.shinapri.bulkfarming.Utils.PlantQueue;
import com.shinapri.bulkfarming.Utils.PosPayload;
import com.shinapri.bulkfarming.Utils.HarvestQueue;
import com.shinapri.bulkfarming.Config.BulkFarmingConfigIO;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class BulkFarming implements ModInitializer {
	public static final String MOD_ID = "bulk-farming";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> HarvestQueue.tick(server));
        ServerTickEvents.END_SERVER_TICK.register(server -> PlantQueue.tick(server));
        PayloadTypeRegistry.playC2S().register(PosPayload.TYPE, PosPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(
                PosPayload.TYPE,
                (payload, context) -> {
                    BlockPos startPos = payload.pos();     // ✅
                    Action action = payload.action();
                    var server = context.server();    // ✅
                    var player = context.player();    // ✅ ServerPlayer
                    server.execute(() -> {
                        var cfg = BulkFarmingConfigIO.get();
                        boolean collectToInv = cfg.collectInInventory;
                        ServerLevel world = player.level(); // ✅ ServerLevelช

                        Set<BlockPos> region = collectFarmland(world, startPos, 4096); // ใส่ limit กันหลุดโลก
                        List<BlockPos> path  = orderSouthWestSerpentine(region);

                        if(action == Action.HARVEST){
                            HarvestQueue.enqueue(player, path);
                        }else if(action == Action.PLANT){
                            PlantQueue.enqueue(player, path);
                            /*player.displayClientMessage(
                                    Component.literal("PLANT!!"),
                                    false
                            );*/
                        }




                    });
                }
        );

	}

    // เก็บ farmland ที่ติดกันทั้งหมดในระดับ Y เดียวกับ start
    static Set<BlockPos> collectFarmland(Level level, BlockPos start, int maxTiles) {
        int y = start.getY();
        if (!level.getBlockState(start).is(Blocks.FARMLAND)) return Set.of();

        var q    = new ArrayDeque<BlockPos>();
        var seen = new HashSet<BlockPos>();
        q.add(start); seen.add(start);

        while (!q.isEmpty() && seen.size() < maxTiles) {
            BlockPos p = q.removeFirst();
            for (BlockPos n : new BlockPos[]{ p.north(), p.south(), p.west(), p.east() }) {
                if (n.getY() != y) continue; // ล็อกให้อยู่ระนาบเดียว
                if (!seen.contains(n) && level.getBlockState(n).is(Blocks.FARMLAND)) {
                    seen.add(n);
                    q.addLast(n);
                }
            }
        }
        return seen;
    }

    static List<BlockPos> orderSouthWestSerpentine(Set<BlockPos> tiles) {
        // แยกเป็นแถวตาม Z (จากล่างไปบน: Z มาก -> น้อย)
        var rows = new TreeMap<Integer, List<BlockPos>>(Comparator.reverseOrder());
        for (BlockPos p : tiles) rows.computeIfAbsent(p.getZ(), k -> new ArrayList<>()).add(p);

        var out = new ArrayList<BlockPos>(tiles.size());
        boolean leftToRight = true; // แถวแรก (Z สูงสุด) ไปจาก X น้อย -> มาก
        for (var entry : rows.entrySet()) {
            var row = entry.getValue();
            row.sort(Comparator.comparingInt(BlockPos::getX)); // X น้อย -> มาก
            if (!leftToRight) Collections.reverse(row);        // สลับทิศทุกแถว (serpentine)
            out.addAll(row);
            leftToRight = !leftToRight;
        }
        return out;
    }



}