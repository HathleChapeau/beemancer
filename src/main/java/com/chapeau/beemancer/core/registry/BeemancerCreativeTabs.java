/**
 * ============================================================
 * [BeemancerCreativeTabs.java]
 * Description: Registre des onglets du mode créatif
 * ============================================================
 * 
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation         |
 * |---------------------|----------------------|---------------------|
 * | Beemancer           | MOD_ID               | Clé du registre     |
 * | BeemancerBlocks     | Blocs à afficher     | Icône et contenu    |
 * | BeemancerItems      | Items à afficher     | Contenu de l'onglet |
 * ------------------------------------------------------------
 * 
 * UTILISÉ PAR:
 * - Beemancer.java (enregistrement)
 * 
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
                    .icon(() -> new ItemStack(BeemancerItems.BEE_DEBUG.get()))
                    .displayItems((parameters, output) -> {
                        // Blocs
                        output.accept(BeemancerItems.STORAGE_CRATE.get());
                        
                        // Abeilles
                        output.accept(BeemancerItems.BEE_DEBUG.get());
                        
                        // Outils
                        output.accept(BeemancerItems.BEE_WAND.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }
}
