package com.smartstreamlabs.worldshaperutilities;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.simibubi.create.AllCreativeModeTabs;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.TooltipModifier;

import net.createmod.catnip.lang.FontHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab.TabVisibility;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(WorldshaperUtilities.ID)
public class WorldshaperUtilities {
	public static final String ID = "smart_worldshaper";
	public static final String NAME = "Worldshaper Utilities";
	public static final Logger LOGGER = LogUtils.getLogger();

	public WorldshaperUtilities() {
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		WorldshaperUtilitiesItems.register(modEventBus);
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, WorldshaperUtilitiesConfig.COMMON_SPEC,
			"worldshaperutilities-common.toml");

		modEventBus.addListener(this::addCreativeTabItems);
		modEventBus.addListener(this::setup);
		LOGGER.info("{} initializing", NAME);
	}

	public static ResourceLocation asResource(String path) {
		return ResourceLocation.fromNamespaceAndPath(ID, path);
	}

	private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES
			|| event.getTabKey() == AllCreativeModeTabs.BASE_CREATIVE_TAB.getKey()) {
			addWorldshaperNextToCreateWorldshaper(event);
		}
	}

	private void setup(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> {
			WorldshaperUtilitiesNetwork.register();
			TooltipModifier.REGISTRY.register(WorldshaperUtilitiesItems.SMART_WORLDSHAPER.get(),
				new ItemDescription.Modifier(WorldshaperUtilitiesItems.SMART_WORLDSHAPER.get(),
					FontHelper.Palette.STANDARD_CREATE));
		});
	}

	private static void addWorldshaperNextToCreateWorldshaper(BuildCreativeModeTabContentsEvent event) {
		ItemStack createWorldshaper = AllItems.WORLDSHAPER.asStack();
		ItemStack smartWorldshaper = new ItemStack(WorldshaperUtilitiesItems.SMART_WORLDSHAPER.get());
		boolean inserted = false;

		for (java.util.Map.Entry<ItemStack, TabVisibility> entry : event.getEntries()) {
			if (!ItemStack.isSameItem(entry.getKey(), createWorldshaper))
				continue;

			event.getEntries().putAfter(entry.getKey(), smartWorldshaper.copy(), entry.getValue());
			inserted = true;
			break;
		}
		if (!inserted)
			event.accept(smartWorldshaper, TabVisibility.PARENT_AND_SEARCH_TABS);
	}
}
