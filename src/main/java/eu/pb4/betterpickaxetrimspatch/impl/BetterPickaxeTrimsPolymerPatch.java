package eu.pb4.betterpickaxetrimspatch.impl;

import eu.pb4.polymer.core.api.item.PolymerCreativeModeTabUtils;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import eu.pb4.polymer.core.api.other.PolymerComponent;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

public class BetterPickaxeTrimsPolymerPatch implements ModInitializer {
	public static final String MOD_ID = "better-pickaxe-trims-polymer-patch";
	public static final String TARGET_MOD_ID = "pickaxetrims";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	static final List<DataComponentType<?>> TRIM_COMPONENTS = new ArrayList<>();

	@Override
	public void onInitialize() {
		PolymerResourcePackUtils.addModAssets(TARGET_MOD_ID);

		// Register Polymer overlays for the target mod's *modded* items (the smithing
		// templates) and data components, so Polymer hides + renders them for vanilla
		// clients. These modded entries live at the end of their registries, so excluding
		// them does not shift any vanilla item's raw id.
		//
		// We must NOT overlay vanilla items (the trimmed pickaxes): registerOverlay calls
		// RegistrySyncUtils.setServerEntry, which marks the item server-only and reorders
		// the item registry - for a vanilla item that desyncs every later item's raw id
		// for vanilla clients (sticks render as pickaxes, etc.). The trimmed vanilla
		// pickaxes already carry a minecraft:item_model component, so a vanilla client
		// renders the trim from the bundled resource pack without any overlay.
		//
		// This patch can initialize before the target mod registers its entries (mod load
		// order is not guaranteed), so we handle entries already present AND any added
		// afterwards, instead of snapshotting the registry once.
		forEachTargetEntry(BuiltInRegistries.ITEM, (id, item) -> {
			PolymerItem.registerOverlay(item, new TrimPolymerItem());
			LOGGER.info("Registered Polymer item overlay for {}", id);
		});
		forEachTargetEntry(BuiltInRegistries.DATA_COMPONENT_TYPE, (id, type) -> {
			PolymerComponent.registerDataComponent(type);
			TRIM_COMPONENTS.add(type);
			LOGGER.info("Registered Polymer data component {}", id);
		});

		// This mod's modded items are all handled by TrimPolymerItem (overlay), so the event
		// below never fires for them. It only matters for plain vanilla items that carry one of
		// this mod's item_model components — which this mod doesn't produce, but we keep the hook
		// symmetric with the sword patch (where trimmed swords ARE vanilla items).
		PolymerItemUtils.ITEM_MODIFICATION_EVENT.register((original, polymerStack, context) -> {
			decorate(original, polymerStack);
			return polymerStack;
		});

		// A smithed trimmed pickaxe is a plain vanilla pickaxe carrying this mod's item_model +
		// trim components. The trim component is registered above, so Polymer treats the stack as
		// fully syncable and ships it untouched — the modification event never runs and no tooltip
		// is added. Flag these stacks so Polymer routes them through item conversion (and thus the
		// event). This does NOT overlay the vanilla item, so it can't reorder/desync item raw ids.
		PolymerItemUtils.CONTEXT_ITEM_CHECK.register((instance, context) -> {
			var model = instance.get(DataComponents.ITEM_MODEL);
			return model != null && TARGET_MOD_ID.equals(model.getNamespace());
		});

		// The mod's creative tab is a plain Fabric tab, invisible to vanilla clients. Register it
		// with Polymer so vanilla players can browse/grab trimmed tools (and the template) in
		// creative, like they can with the sword mod's items.
		forEachTargetEntry(BuiltInRegistries.CREATIVE_MODE_TAB, (id, tab) -> {
			if (!Boolean.TRUE.equals(PolymerCreativeModeTabUtils.contains(id))) {
				PolymerCreativeModeTabUtils.registerPolymerCreativeModeTab(id, tab);
				LOGGER.info("Registered Polymer creative tab {}", id);
			}
		});

		// Optional: make trimmed variants and both templates browsable/searchable in Polydex.
		var polydexLoaded = FabricLoader.getInstance().isModLoaded("polydex");
		LOGGER.info("Polydex present: {}", polydexLoaded);
		if (polydexLoaded) {
			PolydexCompat.register();
		}
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	/** Adds a trim tooltip to a plain vanilla item that carries one of this mod's item_model components. */
	private static void decorate(ItemStack original, ItemStack polymerStack) {
		var model = original.get(DataComponents.ITEM_MODEL);
		if (model != null && TARGET_MOD_ID.equals(model.getNamespace())) {
			TrimmedPickaxePolymerItem.addTrimLore(polymerStack, model.getPath());
		}
	}

	static void addLore(ItemStack stack, Component line) {
		stack.set(DataComponents.LORE, stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).withLineAdded(line));
	}

	/** "fracture_armor_trim_smithing_template" -> "Fracture". */
	static String patternName(String templatePath) {
		var idx = templatePath.indexOf("_armor_trim");
		return prettify(idx > 0 ? templatePath.substring(0, idx) : templatePath);
	}

	/** "placeholder_diamond_pickaxe_trimmed_copper" -> "Copper". */
	static String materialName(String modelPath) {
		var marker = "_trimmed_";
		var idx = modelPath.indexOf(marker);
		return prettify(idx >= 0 ? modelPath.substring(idx + marker.length()) : modelPath);
	}

	private static String prettify(String snakeCase) {
		var parts = snakeCase.split("_");
		var sb = new StringBuilder();
		for (var part : parts) {
			if (part.isEmpty()) continue;
			if (sb.length() > 0) sb.append(' ');
			sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase(Locale.ROOT));
		}
		return sb.toString();
	}

	static <T> void forEachTargetEntry(Registry<T> registry, BiConsumer<Identifier, T> action) {
		for (var entry : registry.entrySet()) {
			var id = entry.getKey().identifier();
			if (TARGET_MOD_ID.equals(id.getNamespace())) {
				action.accept(id, entry.getValue());
			}
		}
		RegistryEntryAddedCallback.event(registry).register((rawId, id, value) -> {
			if (TARGET_MOD_ID.equals(id.getNamespace())) {
				action.accept(id, value);
			}
		});
	}
}
