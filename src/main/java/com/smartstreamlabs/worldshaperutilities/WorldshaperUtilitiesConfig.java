package com.smartstreamlabs.worldshaperutilities;

import java.util.List;

import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.common.ForgeConfigSpec;

public class WorldshaperUtilitiesConfig {
	public static final ForgeConfigSpec COMMON_SPEC;

	public static final ForgeConfigSpec.IntValue MAX_RANGE;
	public static final ForgeConfigSpec.IntValue MAX_CUBOID_BRUSH_SIZE;
	public static final ForgeConfigSpec.IntValue MAX_BLOCKS_PER_ACTION;
	public static final ForgeConfigSpec.BooleanValue ENABLE_DROPS_ON_CLEAR;
	public static final ForgeConfigSpec.IntValue DROP_LIMIT_PER_ACTION;
	public static final ForgeConfigSpec.BooleanValue SILK_TOUCH_CLEAR_MODE;
	public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLIST_BLOCKS;

	static {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

		builder.push("worldshaper");
		MAX_RANGE = builder
			.comment("Maximum raycast range for the enhanced Worldshaper.")
			.defineInRange("maxRange", 64, 1, 256);
		MAX_CUBOID_BRUSH_SIZE = builder
			.comment("Maximum width, height, and length for the Cuboid brush in the enhanced Worldshaper UI.")
			.defineInRange("maxCuboidBrushSize", 120, 1, 120);
		MAX_BLOCKS_PER_ACTION = builder
			.comment("Maximum block positions that the enhanced Worldshaper may process in one action.")
			.defineInRange("maxBlocksPerAction", 1728000, 1, 1728000);
		ENABLE_DROPS_ON_CLEAR = builder
			.comment("When true, Clear mode drops blocks using normal Minecraft loot tables.")
			.define("enableDropsOnClear", true);
		DROP_LIMIT_PER_ACTION = builder
			.comment("Maximum item count that Clear mode may drop in one action. Further blocks are still cleared, but their drops are skipped.")
			.defineInRange("dropLimitPerAction", 4096, 1, 65536);
		SILK_TOUCH_CLEAR_MODE = builder
			.comment("When true, Clear mode evaluates loot tables with Silk Touch.")
			.define("silkTouchClearMode", false);
		BLACKLIST_BLOCKS = builder
			.comment("Blocks that the enhanced Worldshaper must not clear.")
			.defineList("blacklistBlocks",
				List.of("minecraft:bedrock", "minecraft:barrier", "minecraft:command_block"),
				value -> value instanceof String id && ResourceLocation.tryParse(id) != null);
		builder.pop();

		COMMON_SPEC = builder.build();
	}
}
