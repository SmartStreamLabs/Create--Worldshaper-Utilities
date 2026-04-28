package com.smartstreamlabs.worldshaperutilities;

import net.minecraft.client.Minecraft;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = WorldshaperUtilities.ID, value = Dist.CLIENT)
public class SmartWorldshaperClientEvents {
	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END)
			return;

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.level == null)
			return;
		SmartWorldshaperRenderHandler.tick();
	}
}
