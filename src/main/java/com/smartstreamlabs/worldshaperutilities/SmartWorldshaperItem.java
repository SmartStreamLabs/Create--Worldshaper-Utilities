package com.smartstreamlabs.worldshaperutilities;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.equipment.zapper.PlacementPatterns;
import com.simibubi.create.content.equipment.zapper.ZapperItem;
import com.simibubi.create.content.equipment.zapper.terrainzapper.Brush;
import com.simibubi.create.content.equipment.zapper.terrainzapper.FlattenTool;
import com.simibubi.create.content.equipment.zapper.terrainzapper.PlacementOptions;
import com.simibubi.create.content.equipment.zapper.terrainzapper.TerrainBrushes;
import com.simibubi.create.content.equipment.zapper.terrainzapper.TerrainTools;
import com.simibubi.create.content.equipment.zapper.terrainzapper.WorldshaperItem;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import com.simibubi.create.foundation.utility.BlockHelper;

import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.gui.ScreenOpener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;

public class SmartWorldshaperItem extends WorldshaperItem {
	private static final String DROPS_ON_CLEAR_KEY = "SmartWorldshaperDropsOnClear";

	private static final List<IntegerProperty> COUNT_STATES = List.of(
		BlockStateProperties.EGGS,
		BlockStateProperties.PICKLES,
		BlockStateProperties.CANDLES
	);

	private static final List<Block> VINELIKE_BLOCKS = List.of(Blocks.VINE, Blocks.GLOW_LICHEN);
	private static final List<BooleanProperty> VINELIKE_STATES = List.of(
		BlockStateProperties.UP,
		BlockStateProperties.NORTH,
		BlockStateProperties.EAST,
		BlockStateProperties.SOUTH,
		BlockStateProperties.WEST,
		BlockStateProperties.DOWN
	);

	public SmartWorldshaperItem(Properties properties) {
		super(properties);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(SimpleCustomRenderer.create(this, new SmartWorldshaperItemRenderer()));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	protected void openHandgunGUI(ItemStack item, net.minecraft.world.InteractionHand hand) {
		ScreenOpener.open(new SmartWorldshaperScreen(item, hand));
	}

	@Override
	protected int getZappingRange(ItemStack stack) {
		return WorldshaperUtilitiesConfig.MAX_RANGE.get();
	}

	public static boolean dropsOnClear(ItemStack stack) {
		CompoundTag tag = stack.getOrCreateTag();
		return !tag.contains(DROPS_ON_CLEAR_KEY) || tag.getBoolean(DROPS_ON_CLEAR_KEY);
	}

	public static void setDropsOnClear(ItemStack stack, boolean enabled) {
		stack.getOrCreateTag()
			.putBoolean(DROPS_ON_CLEAR_KEY, enabled);
	}

	@Override
	protected boolean activate(Level level, Player player, ItemStack stack, BlockState stateToUse,
		BlockHitResult raytrace, CompoundTag data) {
		BlockPos targetPos = raytrace.getBlockPos();
		List<BlockPos> affectedPositions = new ArrayList<>();

		CompoundTag settings = stack.getOrCreateTag();
		if (!settings.contains("BrushParams"))
			return false;

		TerrainBrushes brushType = NBTHelper.readEnum(settings, "Brush", TerrainBrushes.class);
		Brush brush = brushType.get();
		BlockPos params = NbtUtils.readBlockPos(settings.getCompound("BrushParams"));
		PlacementOptions option = NBTHelper.readEnum(settings, "Placement", PlacementOptions.class);
		TerrainTools tool = NBTHelper.readEnum(settings, "Tool", TerrainTools.class);

		brush.set(params.getX(), params.getY(), params.getZ());
		targetPos = targetPos.offset(brush.getOffset(player.getLookAngle(), raytrace.getDirection(), option));
		if (brushType == TerrainBrushes.Cuboid)
			addLimitedCuboidPositions(targetPos, params, affectedPositions, WorldshaperUtilitiesConfig.MAX_BLOCKS_PER_ACTION.get());
		else
			brush.addToGlobalPositions(level, targetPos, raytrace.getDirection(), affectedPositions, tool);
		PlacementPatterns.applyPattern(affectedPositions, stack);

		TargetSelection targets = limitTargets(affectedPositions);
		if (targets.capped())
			player.displayClientMessage(Component.translatable("message.smart_worldshaper.action_limit",
				targets.positions().size()), true);

		TerrainTools effectiveTool = brush.redirectTool(tool);
		if (effectiveTool == TerrainTools.Clear)
			return clearBlocks(level, player, stack, targets.positions());
		if (effectiveTool == TerrainTools.Flatten) {
			FlattenTool.apply(level, targets.positions(), raytrace.getDirection());
			return true;
		}

		return placeBlocks(level, player, stateToUse, data, raytrace.getDirection(), effectiveTool, targets.positions(),
			targetPos);
	}

	static void addLimitedCuboidPositions(BlockPos targetPos, BlockPos params, List<BlockPos> affectedPositions, int limit) {
		BlockPos from = BlockPos.ZERO.offset((params.getX() - 1) / -2, (params.getY() - 1) / -2, (params.getZ() - 1) / -2);
		BlockPos to = BlockPos.ZERO.offset(params.getX() / 2, params.getY() / 2, params.getZ() / 2);
		int added = 0;
		for (BlockPos localPos : BlockPos.betweenClosed(from, to)) {
			if (added++ >= limit)
				break;
			affectedPositions.add(targetPos.offset(localPos));
		}
	}

	private static TargetSelection limitTargets(List<BlockPos> affectedPositions) {
		int limit = WorldshaperUtilitiesConfig.MAX_BLOCKS_PER_ACTION.get();
		Set<BlockPos> uniquePositions = new LinkedHashSet<>(affectedPositions);
		List<BlockPos> positions = new ArrayList<>(Math.min(uniquePositions.size(), limit));
		int index = 0;
		for (BlockPos pos : uniquePositions) {
			if (index++ >= limit)
				break;
			positions.add(pos);
		}
		return new TargetSelection(positions, uniquePositions.size() > positions.size());
	}

	private boolean placeBlocks(Level level, Player player, BlockState stateToUse, @Nullable CompoundTag data,
		Direction facing, TerrainTools tool, List<BlockPos> affectedPositions, BlockPos center) {
		List<BlockPos> placementTargets = collectPlacementTargets(level, tool, stateToUse, affectedPositions);
		if (placementTargets.isEmpty())
			return true;

		int placementBudget = placementTargets.size();
		if (!player.isCreative()) {
			placementBudget = countAvailablePlacements(player, stateToUse, placementTargets.size());
			if (placementBudget < placementTargets.size()) {
				player.displayClientMessage(Component.translatable("message.smart_worldshaper.not_enough_blocks"), true);
				AllSoundEvents.DENY.play(level, player, player.blockPosition());
			}
			if (placementBudget <= 0)
				return false;
		}
		if (placementBudget < placementTargets.size())
			placementTargets.sort(Comparator.comparingDouble(pos -> distanceSquared(pos, center)));

		int placed = 0;
		for (BlockPos pos : placementTargets) {
			if (placed >= placementBudget)
				break;

			if (!placeSingleBlock(level, pos, stateToUse, data, player, facing, !player.isCreative()))
				continue;
			placed++;
		}

		return placed > 0;
	}

	private static double distanceSquared(BlockPos pos, BlockPos center) {
		double dx = pos.getX() - center.getX();
		double dy = pos.getY() - center.getY();
		double dz = pos.getZ() - center.getZ();
		return dx * dx + dy * dy + dz * dz;
	}

	private static List<BlockPos> collectPlacementTargets(Level level, TerrainTools tool, BlockState stateToUse,
		List<BlockPos> affectedPositions) {
		List<BlockPos> placementTargets = new ArrayList<>();
		for (BlockPos pos : affectedPositions) {
			switch (tool) {
			case Fill -> {
				BlockState toReplace = level.getBlockState(pos);
				if (TerrainTools.isReplaceable(toReplace))
					placementTargets.add(pos);
			}
			case Overlay -> {
				BlockState toOverlay = level.getBlockState(pos);
				if (TerrainTools.isReplaceable(toOverlay))
					continue;
				if (toOverlay == stateToUse)
					continue;

				BlockPos above = pos.above();
				BlockState toReplace = level.getBlockState(above);
				if (TerrainTools.isReplaceable(toReplace))
					placementTargets.add(above);
			}
			case Place -> placementTargets.add(pos);
			case Replace -> {
				BlockState toReplace = level.getBlockState(pos);
				if (!TerrainTools.isReplaceable(toReplace))
					placementTargets.add(pos);
			}
			default -> {
			}
			}
		}
		return placementTargets;
	}

	private static boolean placeSingleBlock(Level level, BlockPos pos, BlockState stateToUse, @Nullable CompoundTag data,
		Player player, Direction facing, boolean consumeFromInventory) {
		BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
		level.setBlockAndUpdate(pos, stateToUse);
		ZapperItem.setBlockEntityData(level, pos, stateToUse, data, player);

		if (ForgeEventFactory.onBlockPlace(player, snapshot, facing)) {
			snapshot.restore(true, true);
			return false;
		}
		if (consumeFromInventory && BlockHelper.findAndRemoveInInventory(stateToUse, player, 1) <= 0) {
			snapshot.restore(true, true);
			return false;
		}
		return true;
	}

	private static int countAvailablePlacements(Player player, BlockState state, int maxNeeded) {
		int requiredPerPlacement = getRequiredItemCount(state);
		if (requiredPerPlacement <= 0)
			return 0;

		Item required = BlockHelper.getRequiredItem(state).getItem();
		if (required == Items.AIR)
			return 0;

		int availableItems = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.getItem() == required)
				availableItems += stack.getCount();
		}
		return Math.min(maxNeeded, availableItems / requiredPerPlacement);
	}

	private static int getRequiredItemCount(BlockState state) {
		int amount = 1;
		if (state.hasProperty(BlockStateProperties.SLAB_TYPE)
			&& state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE)
			amount *= 2;

		for (IntegerProperty property : COUNT_STATES)
			if (state.hasProperty(property))
				amount *= state.getValue(property);

		if (VINELIKE_BLOCKS.contains(state.getBlock())) {
			int vineCount = 0;
			for (BooleanProperty vineState : VINELIKE_STATES)
				if (state.hasProperty(vineState) && state.getValue(vineState))
					vineCount++;
			amount += vineCount - 1;
		}
		return amount;
	}

	private boolean clearBlocks(Level level, Player player, ItemStack stack, List<BlockPos> affectedPositions) {
		boolean dropsEnabled = WorldshaperUtilitiesConfig.ENABLE_DROPS_ON_CLEAR.get() && dropsOnClear(stack);
		ClearResult result = dropsEnabled
			? clearBlocksWithDrops(level, player, stack, affectedPositions)
			: clearBlocksWithoutDrops(level, affectedPositions);

		if (dropsEnabled) {
			player.displayClientMessage(Component.translatable("message.smart_worldshaper.cleared_with_drops",
				result.clearedBlocks(), result.droppedItems()), true);
			if (result.dropLimitReached())
				player.displayClientMessage(Component.translatable("message.smart_worldshaper.drop_limit"), true);
		} else {
			player.displayClientMessage(Component.translatable("message.smart_worldshaper.cleared_without_drops",
				result.clearedBlocks()), true);
		}
		return result.clearedBlocks() > 0;
	}

	private static ClearResult clearBlocksWithoutDrops(Level level, List<BlockPos> affectedPositions) {
		int cleared = 0;
		for (BlockPos pos : affectedPositions) {
			BlockState state = level.getBlockState(pos);
			if (!canClear(state))
				continue;
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			cleared++;
		}
		return new ClearResult(cleared, 0, false);
	}

	private static ClearResult clearBlocksWithDrops(Level level, Player player, ItemStack stack,
		List<BlockPos> affectedPositions) {
		if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel))
			return new ClearResult(0, 0, false);

		ItemStack toolForDrops = createClearTool(level, stack);
		int droppedItems = 0;
		int clearedBlocks = 0;
		int dropLimit = WorldshaperUtilitiesConfig.DROP_LIMIT_PER_ACTION.get();
		boolean limitReached = false;
		List<ItemStack> collectedDrops = new ArrayList<>();

		for (BlockPos pos : affectedPositions) {
			BlockState state = level.getBlockState(pos);
			if (!canClear(state))
				continue;

			if (state.getBlock() instanceof LiquidBlock) {
				level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
				clearedBlocks++;
				continue;
			}

			BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
			List<ItemStack> drops = Block.getDrops(state, serverLevel, pos, blockEntity, player, toolForDrops);
			int dropCount = drops.stream()
				.mapToInt(ItemStack::getCount)
				.sum();
			boolean mayDrop = !limitReached && droppedItems + dropCount <= dropLimit;

			if (!mayDrop)
				limitReached = true;

			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			if (mayDrop) {
				for (ItemStack drop : drops)
					if (!drop.isEmpty())
						addCollectedDrop(collectedDrops, drop);
				droppedItems += dropCount;
			}
			state.spawnAfterBreak(serverLevel, pos, toolForDrops, false);
			clearedBlocks++;
		}

		spawnCollectedDropsAtPlayer(level, player, collectedDrops);
		return new ClearResult(clearedBlocks, droppedItems, limitReached);
	}

	private static void addCollectedDrop(List<ItemStack> collectedDrops, ItemStack drop) {
		ItemStack remaining = drop.copy();
		for (ItemStack collected : collectedDrops) {
			if (!ItemStack.isSameItemSameTags(collected, remaining))
				continue;

			int room = collected.getMaxStackSize() - collected.getCount();
			if (room <= 0)
				continue;

			int moved = Math.min(room, remaining.getCount());
			collected.grow(moved);
			remaining.shrink(moved);
			if (remaining.isEmpty())
				return;
		}

		while (!remaining.isEmpty()) {
			ItemStack split = remaining.split(Math.min(remaining.getCount(), remaining.getMaxStackSize()));
			collectedDrops.add(split);
		}
	}

	private static void spawnCollectedDropsAtPlayer(Level level, Player player, List<ItemStack> collectedDrops) {
		double x = player.getX();
		double y = player.getY();
		double z = player.getZ();
		for (ItemStack drop : collectedDrops) {
			if (drop.isEmpty())
				continue;

			ItemEntity itemEntity = new ItemEntity(level, x, y, z, drop.copy());
			itemEntity.setDefaultPickUpDelay();
			level.addFreshEntity(itemEntity);
		}
	}

	private static ItemStack createClearTool(Level level, ItemStack stack) {
		ItemStack toolForDrops = stack.copy();
		if (WorldshaperUtilitiesConfig.SILK_TOUCH_CLEAR_MODE.get())
			toolForDrops.enchant(Enchantments.SILK_TOUCH, 1);
		return toolForDrops;
	}

	private static boolean canClear(BlockState state) {
		return !state.isAir() && !isBlacklisted(state);
	}

	private static boolean isBlacklisted(BlockState state) {
		ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		for (String configuredId : WorldshaperUtilitiesConfig.BLACKLIST_BLOCKS.get()) {
			ResourceLocation parsed = ResourceLocation.tryParse(configuredId);
			if (blockId.equals(parsed))
				return true;
		}
		return false;
	}

	private record TargetSelection(List<BlockPos> positions, boolean capped) {
	}

	private record ClearResult(int clearedBlocks, int droppedItems, boolean dropLimitReached) {
	}
}
