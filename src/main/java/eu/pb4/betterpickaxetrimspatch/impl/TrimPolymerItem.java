package eu.pb4.betterpickaxetrimspatch.impl;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Polymer overlay for the pickaxe trim mod's modded items. Handles both the smithing template
 * and the per-trim "placeholder" pickaxe items the mod registers.
 *
 * <p>Note: {@code ITEM_MODIFICATION_EVENT} is NOT fired for items registered through
 * {@code registerOverlay} (Polymer returns the PolymerItem's own stack first), so the trim
 * tooltip and texture must be supplied here, not via that event.
 */
public final class TrimPolymerItem implements PolymerItem {
	static boolean isTemplate(Identifier id) {
		return id.getPath().endsWith("_armor_trim_smithing_template");
	}

	@Override
	public Item getPolymerItem(ItemStack stack, PacketContext context) {
		var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return isTemplate(id) ? Items.PAPER : basePickaxe(id.getPath());
	}

	@Override
	public Identifier getPolymerItemModel(ItemStack stack, PacketContext context, HolderLookup.Provider lookup) {
		// The modded item's client model lives at assets/<ns>/items/<path>.json — i.e. its own id.
		return BuiltInRegistries.ITEM.getKey(stack.getItem());
	}

	@Override
	public void modifyBasePolymerItemStack(ItemStack out, ItemStack stack, PacketContext context, HolderLookup.Provider lookup) {
		var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		out.set(DataComponents.CUSTOM_NAME, Component.literal(displayName(id)).withStyle(s -> s.withItalic(false)));
	}

	@Override
	public void modifyClientTooltip(List<Component> tooltip, ItemStack stack, PacketContext context) {
		if (tooltip.stream().anyMatch(line -> line.getString().equals("Upgrade:") || line.getString().contains("Applies to:"))) {
			return;
		}

		var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (isTemplate(id)) {
			int index = Math.min(1, tooltip.size());
			tooltip.add(index++, Component.literal("Smithing Template").withStyle(ChatFormatting.GRAY));
			tooltip.add(index++, Component.empty());
			tooltip.add(index++, Component.literal("Applies to:").withStyle(ChatFormatting.GRAY));
			tooltip.add(index++, Component.literal(" Pickaxes & Armour").withStyle(ChatFormatting.BLUE));
			tooltip.add(index++, Component.literal("Ingredients:").withStyle(ChatFormatting.GRAY));
			tooltip.add(index++, Component.literal(" Ingots & Crystals").withStyle(ChatFormatting.BLUE));
			tooltip.add(index, Component.literal("Crafts 2: 4 Copper, 2 Iron, 1 Emerald").withStyle(ChatFormatting.DARK_GRAY));
			return;
		}

		var trim = TrimmedPickaxePolymerItem.trimFromModelPath(id.getPath());
		if (trim != null) {
			TrimmedPickaxePolymerItem.appendTrimTooltip(tooltip, trim);
		}
	}

	static String displayName(Identifier id) {
		if (isTemplate(id)) {
			return BetterPickaxeTrimsPolymerPatch.patternName(id.getPath()) + " Pickaxe Trim";
		}

		return basePickaxeName(id.getPath()) + " (" + BetterPickaxeTrimsPolymerPatch.materialName(id.getPath()) + ")";
	}

	private static Item basePickaxe(String path) {
		if (path.contains("netherite")) return Items.NETHERITE_PICKAXE;
		if (path.contains("diamond")) return Items.DIAMOND_PICKAXE;
		if (path.contains("gold")) return Items.GOLDEN_PICKAXE; // covers gold_ and golden_
		if (path.contains("iron")) return Items.IRON_PICKAXE;
		if (path.contains("stone")) return Items.STONE_PICKAXE;
		if (path.contains("wood")) return Items.WOODEN_PICKAXE;
		return Items.DIAMOND_PICKAXE;
	}

	private static String basePickaxeName(String path) {
		if (path.contains("netherite")) return "Netherite Pickaxe";
		if (path.contains("diamond")) return "Diamond Pickaxe";
		if (path.contains("gold")) return "Golden Pickaxe";
		if (path.contains("iron")) return "Iron Pickaxe";
		if (path.contains("stone")) return "Stone Pickaxe";
		if (path.contains("wood")) return "Wooden Pickaxe";
		return "Pickaxe";
	}
}
