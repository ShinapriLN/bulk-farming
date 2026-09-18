package com.shinapri.bulkfarming.Utils;

import com.shinapri.bulkfarming.Common.Action;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.BlockPos;
//? if <=1.21.10 {
import net.minecraft.resources.ResourceLocation;
//?} else {
/*import net.minecraft.resources.Identifier;
*///?}

public record PosPayload(BlockPos pos, Action action) implements CustomPacketPayload {
    //? if <=1.21.10 {
    public static final Type<PosPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("bulk-farming", "pos"));
    //?} else {
    /*public static final Type<PosPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("bulk-farming", "pos"));
    *///?}
    public static final StreamCodec<FriendlyByteBuf, PosPayload> CODEC =
            CustomPacketPayload.codec(PosPayload::write, PosPayload::new);

    public PosPayload(FriendlyByteBuf buf) {
        this(
                buf.readBlockPos(),
                buf.readEnum(Action.class)
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeEnum(action);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
