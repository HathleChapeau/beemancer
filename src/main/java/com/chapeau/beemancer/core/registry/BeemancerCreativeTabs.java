/**
 * ============================================================
 * [BeemancerCreativeTabs.java]
 * Description: Registre des onglets du mode créatif
 * ============================================================
 */
package com.chapeau.beemancer.core.registry;

import com.chapeau.beemancer.Beemancer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class BeemancerCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = 
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Beemancer.MOD_ID);

    public static final Supplier<CreativeModeTab> BEEMANCER_TAB = CREATIVE_TABS.register("beemancer_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + Beemancer.MOD_ID))
                    .icon(() -> new ItemStack(BeemancerItems.MAGIC_BEE.get()))
                    .displayItems((parameters, output) -> {
                        // Bees
                        output.accept(BeemancerItems.MAGIC_BEE.get());
                        output.accept(BeemancerItems.BEE_LARVA.get());
                        
                        // Tools
                        output.accept(BeemancerItems.BEE_WAND.get());
                        
                        // Machines
                        output.accept(BeemancerItems.BEE_CREATOR.get());
                        output.accept(BeemancerItems.MAGIC_HIVE.get());
                        output.accept(BeemancerItems.INCUBATOR.get());
                        output.accept(BeemancerItems.BREEDING_CRYSTAL.get());
                        
                        // Storage
                        output.accept(BeemancerItems.STORAGE_CRATE.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }
}
