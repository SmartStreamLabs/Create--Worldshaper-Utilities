package com.smartstreamlabs.worldshaperutilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.content.equipment.zapper.terrainzapper.Brush;
import com.simibubi.create.content.equipment.zapper.terrainzapper.PlacementOptions;
import com.simibubi.create.content.equipment.zapper.terrainzapper.TerrainBrushes;
import com.simibubi.create.content.equipment.zapper.terrainzapper.TerrainTools;

import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;

public class SmartWorldshaperRenderHandler {
	private static Supplier<Collection<BlockPos>> renderedPositions;

	public static void tick() {
		gatherSelectedBlocks();
		if (renderedPositions == null)
			return;

		Outliner.getInstance().showCluster("smartWorldshaper", renderedPositions.get())
			.colored(0xc9823a)
			.disableLineNormals()
			.lineWidth(1 / 32f)
			.withFaceTexture(AllSpecialTextures.CHECKERED);
	}

	private static void gatherSelectedBlocks() {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			renderedPositions = null;
			return;
		}

		ItemStack heldMain = player.getMainHandItem();
		ItemStack heldOff = player.getOffhandItem();
		boolean shaperInMain = WorldshaperUtilitiesItems.SMART_WORLDSHAPER.get() == heldMain.getItem();
		boolean shaperInOff = WorldshaperUtilitiesItems.SMART_WORLDSHAPER.get() == heldOff.getItem();

		if (shaperInMain) {
			if (!heldMain.getOrCreateTag().getBoolean("Swap") || !shaperInOff) {
				createBrushOutline(player, heldMain);
				return;
			}
		}

		if (shaperInOff) {
			createBrushOutline(player, heldOff);
			return;
		}

		renderedPositions = null;
	}

	private static void createBrushOutline(LocalPlayer player, ItemStack shaper) {
		CompoundTag settings = shaper.getOrCreateTag();
		if (!settings.contains("BrushParams")) {
			renderedPositions = null;
			return;
		}

		TerrainBrushes brushType = NBTHelper.readEnum(settings, "Brush", TerrainBrushes.class);
		Brush brush = brushType.get();
		PlacementOptions placement = NBTHelper.readEnum(settings, "Placement", PlacementOptions.class);
		TerrainTools tool = NBTHelper.readEnum(settings, "Tool", TerrainTools.class);
		BlockPos params = NbtUtils.readBlockPos(settings.getCompound("BrushParams"));
		brush.set(params.getX(), params.getY(), params.getZ());

		Vec3 start = player.position()
			.add(0, player.getEyeHeight(), 0);
		Vec3 range = player.getLookAngle()
			.scale(WorldshaperUtilitiesConfig.MAX_RANGE.get());
		BlockHitResult raytrace = player.level()
			.clip(new ClipContext(start, start.add(range), Block.OUTLINE, Fluid.NONE, player));
		if (raytrace == null || raytrace.getType() == Type.MISS) {
			renderedPositions = null;
			return;
		}

		BlockPos pos = raytrace.getBlockPos()
			.offset(brush.getOffset(player.getLookAngle(), raytrace.getDirection(), placement));
		if (brushType == TerrainBrushes.Cuboid) {
			renderedPositions = () -> {
				List<BlockPos> positions = new ArrayList<>();
				addCuboidShellPositions(pos, params, positions,
					WorldshaperUtilitiesConfig.MAX_BLOCKS_PER_ACTION.get());
				return positions;
			};
		} else {
			renderedPositions =
			() -> brush.addToGlobalPositions(player.level(), pos, raytrace.getDirection(), new ArrayList<>(), tool);
		}
	}

	private static void addCuboidShellPositions(BlockPos targetPos, BlockPos params, List<BlockPos> positions, int limit) {
		BlockPos from = BlockPos.ZERO.offset((params.getX() - 1) / -2, (params.getY() - 1) / -2, (params.getZ() - 1) / -2);
		BlockPos to = BlockPos.ZERO.offset(params.getX() / 2, params.getY() / 2, params.getZ() / 2);
		int added = 0;
		for (BlockPos localPos : BlockPos.betweenClosed(from, to)) {
			boolean onShell = localPos.getX() == from.getX() || localPos.getX() == to.getX()
				|| localPos.getY() == from.getY() || localPos.getY() == to.getY()
				|| localPos.getZ() == from.getZ() || localPos.getZ() == to.getZ();
			if (!onShell)
				continue;
			if (added++ >= limit)
				break;
			positions.add(targetPos.offset(localPos));
		}
	}
}
