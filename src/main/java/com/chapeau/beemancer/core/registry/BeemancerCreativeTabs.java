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
                        // Codex
                        output.accept(BeemancerItems.CODEX.get());

                        // Bees
                        output.accept(BeemancerItems.MAGIC_BEE.get());
                        output.accept(BeemancerItems.BEE_LARVA.get());

                        // Tools
                        output.accept(BeemancerItems.BEE_WAND.get());
                        output.accept(BeemancerItems.BUILDING_WAND.get());

                        // Bee Machines
                        output.accept(BeemancerItems.BEE_CREATOR.get());
                        output.accept(BeemancerItems.MAGIC_HIVE.get());
                        output.accept(BeemancerItems.INCUBATOR.get());
                        output.accept(BeemancerItems.BREEDING_CRYSTAL.get());

                        // Alchemy Machines
                        output.accept(BeemancerItems.MANUAL_CENTRIFUGE.get());
                        output.accept(BeemancerItems.POWERED_CENTRIFUGE.get());
                        output.accept(BeemancerItems.POWERED_CENTRIFUGE_TIER2.get());
                        output.accept(BeemancerItems.POWERED_CENTRIFUGE_TIER3.get());
                        output.accept(BeemancerItems.HONEY_TANK.get());
                        output.accept(BeemancerItems.MULTIBLOCK_TANK.get());
                        output.accept(BeemancerItems.CREATIVE_TANK.get());
                        output.accept(BeemancerItems.HONEY_PIPE.get());
                        output.accept(BeemancerItems.HONEY_PIPE_TIER2.get());
                        output.accept(BeemancerItems.HONEY_PIPE_TIER3.get());
                        output.accept(BeemancerItems.HONEY_PIPE_TIER4.get());
                        output.accept(BeemancerItems.ITEM_PIPE.get());
                        output.accept(BeemancerItems.ITEM_PIPE_TIER2.get());
                        output.accept(BeemancerItems.ITEM_PIPE_TIER3.get());
                        output.accept(BeemancerItems.ITEM_PIPE_TIER4.get());
                        output.accept(BeemancerItems.CRYSTALLIZER.get());
                        output.accept(BeemancerItems.ALEMBIC.get());
                        output.accept(BeemancerItems.INFUSER.get());
                        output.accept(BeemancerItems.INFUSER_TIER2.get());
                        output.accept(BeemancerItems.INFUSER_TIER3.get());

                        // Fluid Buckets
                        output.accept(BeemancerItems.HONEY_BUCKET.get());
                        output.accept(BeemancerItems.ROYAL_JELLY_BUCKET.get());
                        output.accept(BeemancerItems.NECTAR_BUCKET.get());

                        // Alchemy Ingredients - Combs
                        output.accept(BeemancerItems.COMMON_COMB.get());
                        output.accept(BeemancerItems.NOBLE_COMB.get());
                        output.accept(BeemancerItems.DILIGENT_COMB.get());
                        output.accept(BeemancerItems.ROYAL_COMB.get());

                        // Alchemy Ingredients - Other
                        output.accept(BeemancerItems.BEESWAX.get());
                        output.accept(BeemancerItems.PROPOLIS.get());
                        output.accept(BeemancerItems.POLLEN.get());
                        output.accept(BeemancerItems.HONEYED_WOOD.get());

                        // Crystal Shards
                        output.accept(BeemancerItems.RAW_CRYSTAL_SHARD.get());
                        output.accept(BeemancerItems.ENRICHED_CRYSTAL_SHARD.get());
                        output.accept(BeemancerItems.RADIANT_CRYSTAL_SHARD.get());

                        // Honey Crystals (higher tiers)
                        output.accept(BeemancerItems.ENRICHED_HONEY_CRYSTAL.get());
                        output.accept(BeemancerItems.RADIANT_HONEY_CRYSTAL.get());

                        // Storage
                        output.accept(BeemancerItems.STORAGE_CRATE.get());
                        output.accept(BeemancerItems.STORAGE_CONTROLLER.get());
                        output.accept(BeemancerItems.STORAGE_TERMINAL.get());

                        // Honey Altar
                        output.accept(BeemancerItems.HONEYED_STONE.get());
                        output.accept(BeemancerItems.HONEYED_STONE_STAIR.get());
                        output.accept(BeemancerItems.HONEYED_SLAB.get());
                        output.accept(BeemancerItems.HONEY_PEDESTAL.get());
                        output.accept(BeemancerItems.HONEY_CRYSTAL_CONDUIT.get());
                        output.accept(BeemancerItems.HONEY_CRYSTAL.get());

                        // Hive Multiblock
                        output.accept(BeemancerItems.HIVE_MULTIBLOCK.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }
}
