// WorkQueue.java
package com.shinapri.bulkfarming.Utils;

import com.shinapri.bulkfarming.Config.BulkFarmingConfigIO;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Inventory;

import java.util.*;

public final class HarvestQueue {
    // งานหนึ่งชิ้น = world ไหน + ตำแหน่งไหน
    public record Job(UUID playerId, ResourceKey<Level> dim, BlockPos pos) {}
    enum HarvestResult { OK, NO_CROP, INVENTORY_BLOCKED }

    private static final Deque<Job> Q = new ArrayDeque<>();
    private static final int BATCH_PER_TICK = 64;   // ปรับตามใจ

    private HarvestQueue() {}

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
        var cfg = BulkFarmingConfigIO.get();
        boolean collectToInv = cfg.collectInInventory;
        ServerPlayer player;

        BlockState currentState;
        for (int i = 0; i < BATCH_PER_TICK && !Q.isEmpty(); i++) {
            Job job = Q.pollFirst();
            ServerLevel level = server.getLevel(job.dim);
            if (level == null) continue;
            BlockPos p = job.pos;
            if (!level.isLoaded(p)) continue;
            currentState = level.getBlockState(p.above());
            // ตัวอย่างงาน: เก็บพืชบน FARMLAND เท่านั้น
            if (currentState.getBlock() instanceof CropBlock crop
                    && crop.isMaxAge(currentState)) {
                if(collectToInv){
                    player = server.getPlayerList().getPlayer(job.playerId);
                    if(player == null) continue;

                    HarvestResult r = tryHarvestOne(player, level, job.pos());

                    if(r == HarvestResult.INVENTORY_BLOCKED || r == HarvestResult.NO_CROP) {
                        continue;
                    }

                }else{
                    level.destroyBlock(p.above(), true);
                }


            }
        }
    }

    private static boolean canFitAll(Inventory inv, List<ItemStack> drops) {
        // ใช้เฉพาะ 36 ช่องหลัก
        final int n = Math.min(36, inv.getNonEquipmentItems().size());  // Inventory.items = main slots
        ItemStack[] sim = new ItemStack[n];
        for (int i = 0; i < n; i++) sim[i] = inv.getNonEquipmentItems().get(i).copy();

        for (ItemStack d0 : drops) {
            if (d0.isEmpty()) continue;
            ItemStack d = d0.copy();

            // รวมกับสแตกเดิม (ชนิด+components ตรงกัน)
            for (int i = 0; i < n && !d.isEmpty(); i++) {
                ItemStack s = sim[i];
                if (s.isEmpty()) continue;
                if (ItemStack.isSameItemSameComponents(s, d)) {
                    int can = Math.min(s.getMaxStackSize() - s.getCount(), d.getCount());
                    if (can > 0) { s.grow(can); d.shrink(can); }
                }
            }
            // ลงช่องว่าง
            for (int i = 0; i < n && !d.isEmpty(); i++) {
                if (sim[i].isEmpty()) { sim[i] = d.copy(); d.setCount(0); }
            }
            if (!d.isEmpty()) return false;   // บล็อกนี้ “เก็บไม่ครบ”
        }
        return true;
    }


    private static HarvestResult tryHarvestOne(ServerPlayer player, ServerLevel level, BlockPos basePos) {
        BlockPos cropPos = basePos.above();
        BlockState state = level.getBlockState(cropPos);
        if (state.isAir()) return HarvestResult.NO_CROP;

// ต้องอยู่ฝั่งเซิร์ฟเวอร์ และใช้ LootParams ให้ครบ
        LootParams.Builder lp = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(cropPos))
                .withParameter(LootContextParams.TOOL, player.getMainHandItem())
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, level.getBlockEntity(cropPos));

// ⬇️ นี่แหละที่หมายถึง `drops`
        java.util.List<ItemStack> drops = state.getDrops(lp);

        if (!canFitAll(player.getInventory(), drops)) {
            player.displayClientMessage(Component.literal("Inventory full for this crop"), true);
            return HarvestResult.INVENTORY_BLOCKED;  // ไม่ทำลายบล็อก
        }

// ใส่ของให้ครบก่อน แล้วค่อยลบโดยไม่ดรอปลงพื้น
        for (ItemStack st : drops) player.getInventory().add(st);
        level.destroyBlock(cropPos, false);
        return HarvestResult.OK;

    }
}
