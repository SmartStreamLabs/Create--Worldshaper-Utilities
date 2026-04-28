package com.smartstreamlabs.worldshaperutilities;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public record SetWorldshaperDropsPacket(InteractionHand hand, boolean dropsEnabled) {
	public static void encode(SetWorldshaperDropsPacket packet, FriendlyByteBuf buffer) {
		buffer.writeBoolean(packet.hand == InteractionHand.OFF_HAND);
		buffer.writeBoolean(packet.dropsEnabled);
	}

	public static SetWorldshaperDropsPacket decode(FriendlyByteBuf buffer) {
		InteractionHand hand = buffer.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		return new SetWorldshaperDropsPacket(hand, buffer.readBoolean());
	}

	public static void handle(SetWorldshaperDropsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null)
				return;

			ItemStack stack = player.getItemInHand(packet.hand());
			if (stack.getItem() == WorldshaperUtilitiesItems.SMART_WORLDSHAPER.get())
				SmartWorldshaperItem.setDropsOnClear(stack, packet.dropsEnabled());
		});
		context.setPacketHandled(true);
	}
}
