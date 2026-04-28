package com.smartstreamlabs.worldshaperutilities;

import java.util.Vector;

import com.simibubi.create.content.equipment.zapper.terrainzapper.PlacementOptions;
import com.simibubi.create.content.equipment.zapper.terrainzapper.TerrainBrushes;
import com.simibubi.create.content.equipment.zapper.terrainzapper.TerrainTools;
import com.simibubi.create.content.equipment.zapper.terrainzapper.WorldshaperScreen;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class SmartWorldshaperScreen extends WorldshaperScreen {
	private static final int DROPS_BUTTON_X_OFFSET = 117;
	private static final int DROPS_BUTTON_Y_OFFSET = 79;

	private IconButton dropsOnClearButton;
	private boolean dropsOnClear;

	public SmartWorldshaperScreen(ItemStack zapper, InteractionHand hand) {
		super(zapper, hand);
		dropsOnClear = SmartWorldshaperItem.dropsOnClear(zapper);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		super.render(graphics, mouseX, mouseY, partialTicks);
		renderHoveredButtonLast(graphics, mouseX, mouseY, partialTicks);
	}

	@Override
	protected void initBrushParams(int x, int y) {
		if (currentBrush != TerrainBrushes.Cuboid) {
			super.initBrushParams(x, y);
			initDropsOnClearButton(x, y);
			return;
		}

		removeWidgets(brushParamLabels);
		removeWidgets(brushParams);

		brushParamLabels.clear();
		brushParams.clear();

		for (int index = 0; index < 3; index++) {
			Label label = new Label(x + 65 + 20 * index, y + 45, CommonComponents.EMPTY).withShadow();

			final int finalIndex = index;
			ScrollInput input = new ScrollInput(x + 56 + 20 * index, y + 40, 18, 18)
				.withRange(1, WorldshaperUtilitiesConfig.MAX_CUBOID_BRUSH_SIZE.get() + 1)
				.writingTo(label)
				.titled(getCuboidParamLabel(index))
				.calling(state -> {
					currentBrushParams[finalIndex] = state;
					label.setX(x + 65 + 20 * finalIndex - font.width(label.text) / 2);
				});
			input.setState(currentBrushParams[index]);
			input.onChanged();

			brushParamLabels.add(label);
			brushParams.add(input);
		}

		addRenderableWidgets(brushParamLabels);
		addRenderableWidgets(brushParams);

		removeConnectivityOptions();
		initToolButtons(x, y);
		initPlacementButtons(x, y);
		initDropsOnClearButton(x, y);
	}

	private static MutableComponent getCuboidParamLabel(int index) {
		return CreateLang.translateDirect(index == 0 ? "generic.width" : index == 1 ? "generic.height" : "generic.length");
	}

	private void removeConnectivityOptions() {
		if (followDiagonals == null)
			return;

		removeWidget(followDiagonals);
		removeWidget(followDiagonalsIndicator);
		removeWidget(acrossMaterials);
		removeWidget(acrossMaterialsIndicator);
		followDiagonals = null;
		followDiagonalsIndicator = null;
		acrossMaterials = null;
		acrossMaterialsIndicator = null;
	}

	private void initToolButtons(int x, int y) {
		if (toolButtons != null)
			removeWidgets(toolButtons);

		TerrainTools[] toolValues = TerrainTools.values();
		toolButtons = new Vector<>(toolValues.length);
		for (TerrainTools tool : toolValues) {
			IconButton toolButton = new IconButton(x + 7 + toolButtons.size() * 18, y + 79, tool.icon);
			toolButton.withCallback(() -> {
				toolButtons.forEach(b -> b.green = false);
				toolButton.green = true;
				currentTool = tool;
			});
			toolButton.setToolTip(CreateLang.translateDirect("gui.terrainzapper.tool." + tool.translationKey));
			toolButtons.add(toolButton);
		}

		int toolIndex = -1;
		for (int i = 0; i < toolValues.length; i++)
			if (currentTool == toolValues[i])
				toolIndex = i;
		if (toolIndex == -1) {
			currentTool = toolValues[0];
			toolIndex = 0;
		}
		toolButtons.get(toolIndex).green = true;

		addRenderableWidgets(toolButtons);
	}

	private void initPlacementButtons(int x, int y) {
		if (placementButtons != null)
			removeWidgets(placementButtons);

		PlacementOptions[] placementValues = PlacementOptions.values();
		placementButtons = new Vector<>(placementValues.length);
		for (PlacementOptions option : placementValues) {
			IconButton placementButton = new IconButton(x + 136 + placementButtons.size() * 18, y + 79, option.icon);
			placementButton.withCallback(() -> {
				placementButtons.forEach(b -> b.green = false);
				placementButton.green = true;
				currentPlacement = option;
			});
			placementButton.setToolTip(CreateLang.translateDirect("gui.terrainzapper.placement." + option.translationKey));
			placementButtons.add(placementButton);
		}

		placementButtons.get(currentPlacement.ordinal()).green = true;
		addRenderableWidgets(placementButtons);
	}

	private void initDropsOnClearButton(int x, int y) {
		if (dropsOnClearButton != null)
			removeWidget(dropsOnClearButton);

		dropsOnClearButton = new IconButton(x + DROPS_BUTTON_X_OFFSET, y + DROPS_BUTTON_Y_OFFSET, AllIcons.I_CLEAR_CHECKED);
		dropsOnClearButton.withCallback(() -> {
			dropsOnClear = !dropsOnClear;
			SmartWorldshaperItem.setDropsOnClear(zapper, dropsOnClear);
			WorldshaperUtilitiesNetwork.sendToServer(new SetWorldshaperDropsPacket(hand, dropsOnClear));
			updateDropsOnClearButton();
		});

		updateDropsOnClearButton();
		addRenderableWidget(dropsOnClearButton);
	}

	private void updateDropsOnClearButton() {
		dropsOnClearButton.green = dropsOnClear;
		dropsOnClearButton.setToolTip(Component.translatable(dropsOnClear
			? "gui.smart_worldshaper.drops_on_clear.enabled"
			: "gui.smart_worldshaper.drops_on_clear.disabled"));
	}

	private void renderHoveredButtonLast(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		if (renderHoveredButton(toolButtons, graphics, mouseX, mouseY, partialTicks))
			return;
		if (renderHoveredButton(placementButtons, graphics, mouseX, mouseY, partialTicks))
			return;
		if (dropsOnClearButton != null && dropsOnClearButton.isMouseOver(mouseX, mouseY))
			dropsOnClearButton.render(graphics, mouseX, mouseY, partialTicks);
	}

	private static boolean renderHoveredButton(Iterable<IconButton> buttons, GuiGraphics graphics, int mouseX, int mouseY,
		float partialTicks) {
		if (buttons == null)
			return false;

		for (IconButton button : buttons) {
			if (!button.isMouseOver(mouseX, mouseY))
				continue;

			button.render(graphics, mouseX, mouseY, partialTicks);
			return true;
		}
		return false;
	}
}
