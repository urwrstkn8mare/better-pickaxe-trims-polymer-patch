package eu.pb4.betterpickaxetrimspatch.impl;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class TemplatePolymerItem implements PolymerItem {
	@Override
	public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
		return Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE;
	}
}
