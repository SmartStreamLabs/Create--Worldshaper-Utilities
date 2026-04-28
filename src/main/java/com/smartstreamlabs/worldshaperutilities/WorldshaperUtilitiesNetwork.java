package com.smartstreamlabs.worldshaperutilities;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class WorldshaperUtilitiesNetwork {
	private static final String PROTOCOL_VERSION = "1";

	private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
		new ResourceLocation(WorldshaperUtilities.ID, "main"),
		() -> PROTOCOL_VERSION,
		PROTOCOL_VERSION::equals,
		PROTOCOL_VERSION::equals
	);

	public static void register() {
		CHANNEL.messageBuilder(SetWorldshaperDropsPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
			.encoder(SetWorldshaperDropsPacket::encode)
			.decoder(SetWorldshaperDropsPacket::decode)
			.consumerMainThread(SetWorldshaperDropsPacket::handle)
			.add();
	}

	public static void sendToServer(SetWorldshaperDropsPacket packet) {
		CHANNEL.sendToServer(packet);
	}
}
