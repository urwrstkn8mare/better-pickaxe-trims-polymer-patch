package eu.pb4.betterpickaxetrimspatch.impl;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.lang.reflect.Method;
import java.util.List;

public final class TrimmedPickaxePolymerItem implements PolymerItem {
	private final Item item;

	public TrimmedPickaxePolymerItem(Item item) {
		this.item = item;
	}

	@Override
	public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
		return this.item;
	}

	@Override
	public void modifyClientTooltip(List<Component> tooltip, ItemStack itemStack, PacketContext context) {
		var trim = getTrim(itemStack);
		if (trim == null) {
			return;
		}

		appendTrimTooltip(tooltip, trim);
	}

	static void addTrimLore(ItemStack stack, String modelPath) {
		var trim = trimFromModelPath(modelPath);
		if (trim == null) {
			return;
		}

		stack.set(DataComponents.LORE, new ItemLore(List.of(
				Component.literal("Pickaxe Trim:").withStyle(ChatFormatting.GRAY),
				Component.literal(" " + trim.material()).withStyle(trim.color()),
				Component.literal(" " + trim.description()).withStyle(trim.color())
		)));
	}

	static void appendTrimTooltip(List<Component> tooltip, PickaxeTrim trim) {
		if (tooltip.stream().anyMatch(line -> line.getString().equals("Pickaxe Trim:"))) {
			return;
		}

		removeDuplicateTrimLines(tooltip);
		int index = Math.min(1, tooltip.size());
		tooltip.add(index++, Component.literal("Pickaxe Trim:").withStyle(ChatFormatting.GRAY));
		tooltip.add(index++, Component.literal(" " + trim.material()).withStyle(trim.color()));
		tooltip.add(index, Component.literal(" " + trim.description()).withStyle(trim.color()));
	}

	private static void removeDuplicateTrimLines(List<Component> tooltip) {
		tooltip.removeIf(line -> {
			var text = line.getString();
			return text.equals("Upgrade:")
					|| text.endsWith(" Material")
					|| text.equals("Break 3x3 at once")
					|| text.equals("Make nearby ores glow")
					|| text.equals("Randomly double drops")
					|| text.equals("Bonus experience")
					|| text.equals("Break adjacent ores")
					|| text.equals("Double durability");
		});
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static PickaxeTrim getTrim(ItemStack stack) {
		for (DataComponentType<?> componentType : BetterPickaxeTrimsPolymerPatch.TRIM_COMPONENTS) {
			Object component = stack.get((DataComponentType) componentType);
			if (component == null || !showInTooltip(component)) {
				continue;
			}

			var trim = trimFromItem(getIngredientItem(component));
			if (trim != null) {
				return trim;
			}
		}

		return null;
	}

	private static boolean showInTooltip(Object component) {
		try {
			Method method = component.getClass().getMethod("showInTooltip");
			Object value = method.invoke(component);
			return !(value instanceof Boolean visible) || visible;
		} catch (ReflectiveOperationException ignored) {
			return true;
		}
	}

	private static Item getIngredientItem(Object component) {
		try {
			Method ingredient = component.getClass().getMethod("ingredient");
			Object holder = ingredient.invoke(component);
			Method value = holder.getClass().getMethod("value");
			Object item = value.invoke(holder);
			return item instanceof Item found ? found : null;
		} catch (ReflectiveOperationException ignored) {
			return null;
		}
	}

	private static PickaxeTrim trimFromItem(Item item) {
		if (item == null) {
			return null;
		}

		var id = BuiltInRegistries.ITEM.getKey(item).toString();
		return trimFromKey(id.substring(id.indexOf(':') + 1));
	}

	static PickaxeTrim trimFromModelPath(String modelPath) {
		var marker = "_trimmed_";
		var idx = modelPath.indexOf(marker);
		return trimFromKey(idx >= 0 ? modelPath.substring(idx + marker.length()) : modelPath);
	}

	private static PickaxeTrim trimFromKey(String key) {
		return switch (key) {
			case "crying_obsidian", "weeping_obsidian" -> new PickaxeTrim("Crying Obsidian", "Breaks blocks in 3x3", ChatFormatting.DARK_PURPLE);
			case "lapis", "lapis_lazuli" -> new PickaxeTrim("Lapis Lazuli", "Highlights same ores (2m)", ChatFormatting.BLUE);
			case "emerald" -> new PickaxeTrim("Emerald", "Chance to double ore drops", ChatFormatting.GREEN);
			case "quartz" -> new PickaxeTrim("Quartz", "Extra XP from ores", ChatFormatting.WHITE);
			case "redstone" -> new PickaxeTrim("Redstone", "Mines connected ores", ChatFormatting.RED);
			case "copper", "copper_ingot" -> new PickaxeTrim("Copper", "Increased durability", ChatFormatting.GOLD);
			default -> null;
		};
	}

	record PickaxeTrim(String material, String description, ChatFormatting color) {
	}
}
