package com.pockyl.petrichor.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The shared "Pocky Mods" creative tab. Every mod of the author ships this same class: the first one to load registers
 * the tab, the others find it already registered, and each adds its own items. No common library is needed.
 * Keep this file identical across mods (it lives in the workspace template).
 */
public final class PockyModsTab {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("pockymods", "pocky_mods");
    public static final ResourceKey<CreativeModeTab> KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, ID);

    private PockyModsTab() {
    }

    /**
     * @param icon  icon used if this mod is the one registering the tab
     * @param items adds this mod's items to the tab
     */
    public static void register(Supplier<ItemStack> icon, Consumer<CreativeModeTab.Output> items) {
        var registry = net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB;
        if (!registry.containsKey(ID)) net.minecraft.core.Registry.register(registry, ID,
                net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab.builder().title(Component.translatable("itemGroup.pockymods.pocky_mods")).icon(icon).build());
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(KEY).register(entries -> items.accept(entries));
    }
}
