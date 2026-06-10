package eu.pb4.betterpickaxetrimspatch.impl;

import eu.pb4.polydex.api.v1.recipe.PageBuilder;
import eu.pb4.polydex.api.v1.recipe.PageIcons;
import eu.pb4.polydex.api.v1.recipe.PageTextures;
import eu.pb4.polydex.api.v1.recipe.PolydexCategory;
import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.api.v1.recipe.PolydexIngredient;
import eu.pb4.polydex.api.v1.recipe.PolydexPage;
import eu.pb4.polydex.api.v1.recipe.PolydexStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Optional Polydex integration. Loaded only when the "polydex" mod is present.
 *
 * <p>This mod registers every trimmed pickaxe as its own real item ({@code pickaxetrims:placeholder_*})
 * plus one smithing template. Each has a distinct registry id, so Polydex won't collapse them — but
 * to be safe against the source mod's creative tab failing to enumerate, we register each through
 * BOTH {@code registerEntryCreator} (creative-tab path) and {@code registerBuilder} (item-registry
 * path). We also give each a clean display name so they read well in the Polydex list and are
 * searchable by material/pattern.
 */
final class PolydexCompat {
	private static final Map<Identifier, Item> TEMPLATES = new LinkedHashMap<>();

	private PolydexCompat() {}

	static void register() {
		var log = BetterPickaxeTrimsPolymerPatch.LOGGER;
		log.info("[Polydex] registering pickaxe-trim entry hooks");
		BetterPickaxeTrimsPolymerPatch.forEachTargetEntry(BuiltInRegistries.ITEM, (id, item) -> {
			if (TrimPolymerItem.isTemplate(id)) {
				TEMPLATES.put(id, item);
			}
			PolydexEntry.registerEntryCreator(item, stack -> entryFor(id, stack));
			PolydexEntry.registerBuilder(item, it -> List.of(entryFor(id, it.getDefaultInstance())));
			log.info("[Polydex] hooked entry for {}", id);
		});
		PolydexPage.register((server, consumer) -> TEMPLATES.forEach((id, item) ->
				consumer.accept(new TemplateRecipePage(id, item))));
	}

	private static PolydexEntry entryFor(Identifier id, ItemStack source) {
		BetterPickaxeTrimsPolymerPatch.LOGGER.info("[Polydex] building entry for {}", id);
		var display = source.copy();
		display.set(DataComponents.ITEM_MODEL, id);
		if (TrimPolymerItem.isTemplate(id)) {
			display.set(DataComponents.CUSTOM_NAME, name(TrimPolymerItem.displayName(id)));
		} else {
			display.set(DataComponents.CUSTOM_NAME, name(TrimPolymerItem.displayName(id)));
		}
		return PolydexEntry.of(id, display);
	}

	private static Component name(String text) {
		return Component.literal(text).withStyle(s -> s.withItalic(false));
	}

	private static final class TemplateRecipePage implements PolydexPage {
		private final Identifier id;
		private final ItemStack output;

		private TemplateRecipePage(Identifier templateId, Item template) {
			this.id = BetterPickaxeTrimsPolymerPatch.id(templateId.getPath() + "_crafting");
			this.output = template.getDefaultInstance();
			this.output.setCount(2);
			this.output.set(DataComponents.ITEM_MODEL, templateId);
			this.output.set(DataComponents.CUSTOM_NAME, name(TrimPolymerItem.displayName(templateId)));
		}

		@Override
		public Identifier identifier() {
			return this.id;
		}

		@Override
		public ItemStack typeIcon(ServerPlayer player) {
			return PageIcons.CRAFTING_TABLE_RECIPE_ICON;
		}

		@Override
		public ItemStack entryIcon(PolydexEntry entry, ServerPlayer player) {
			return this.output.copy();
		}

		@Override
		public Component texture(ServerPlayer player) {
			return PageTextures.CRAFTING;
		}

		@Override
		public void createPage(PolydexEntry entry, ServerPlayer player, PageBuilder builder) {
			builder.setIngredient(3, 1, new ItemStack(Items.COPPER_INGOT));
			builder.setIngredient(4, 1, new ItemStack(Items.IRON_INGOT));
			builder.setIngredient(2, 2, new ItemStack(Items.COPPER_INGOT));
			builder.setIngredient(3, 2, new ItemStack(Items.EMERALD));
			builder.setIngredient(4, 2, new ItemStack(Items.COPPER_INGOT));
			builder.setIngredient(2, 3, new ItemStack(Items.IRON_INGOT));
			builder.setIngredient(3, 3, new ItemStack(Items.COPPER_INGOT));
			builder.setOutput(6, 2, this.output.copy());
		}

		@Override
		public List<PolydexIngredient<?>> ingredients() {
			return List.of(
					PolydexStack.of(new ItemStack(Items.COPPER_INGOT, 4)),
					PolydexStack.of(new ItemStack(Items.IRON_INGOT, 2)),
					PolydexStack.of(Items.EMERALD));
		}

		@Override
		public List<PolydexCategory> categories() {
			return List.of(PolydexCategory.CRAFTING);
		}

		@Override
		public boolean isOwner(MinecraftServer server, PolydexEntry entry) {
			return entry.identifier().equals(BuiltInRegistries.ITEM.getKey(this.output.getItem()));
		}
	}
}
