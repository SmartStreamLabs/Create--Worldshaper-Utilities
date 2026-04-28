package com.smartstreamlabs.worldshaperutilities;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class WorldshaperUtilitiesItems {
	private static final DeferredRegister<Item> ITEMS =
		DeferredRegister.create(Registries.ITEM, WorldshaperUtilities.ID);

	public static final RegistryObject<SmartWorldshaperItem> SMART_WORLDSHAPER =
		ITEMS.register("smart_worldshaper",
			() -> new SmartWorldshaperItem(new Item.Properties().rarity(Rarity.EPIC)));

	public static void register(IEventBus modEventBus) {
		ITEMS.register(modEventBus);
	}
}
