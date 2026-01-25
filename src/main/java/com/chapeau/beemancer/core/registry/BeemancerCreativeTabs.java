/**
 * ============================================================
 * [BeemancerCreativeTabs.java]
 * Description: Registre des onglets du mode créatif
 * ============================================================
 *
 * Onglets:
 * - MAIN_TAB: Machines, outils, stockage, blocs décoratifs
 * - BEES_TAB: Abeilles et larves
 * - COMBS_TAB: Combs, pollens, fragments, shards, dusts
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

    // =========================================================================
    // MAIN TAB - Machines, Tools, Storage, Decorative Blocks
    // =========================================================================

    public static final Supplier<CreativeModeTab> MAIN_TAB = CREATIVE_TABS.register("main_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + Beemancer.MOD_ID + ".main"))
                    .icon(() -> new ItemStack(BeemancerItems.MAGIC_HIVE.get()))
                    .displayItems((parameters, output) -> {
                        addCodexAndTools(output);
                        addBeeMachines(output);
                        addAlchemyMachines(output);
                        addPipesAndTanks(output);
                        addFluidBuckets(output);
                        addStorageBlocks(output);
                        addHoneyAltarBlocks(output);
                        addCrystalItems(output);
                    })
                    .build());

    // =========================================================================
    // BEES TAB - Bees and Larvae
    // =========================================================================

    public static final Supplier<CreativeModeTab> BEES_TAB = CREATIVE_TABS.register("bees_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + Beemancer.MOD_ID + ".bees"))
                    .icon(() -> new ItemStack(BeemancerItems.MAGIC_BEE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(BeemancerItems.MAGIC_BEE.get());
                        output.accept(BeemancerItems.BEE_LARVA.get());
                    })
                    .build());

    // =========================================================================
    // COMBS TAB - Combs, Pollens, Fragments, Shards, Dusts
    // =========================================================================

    public static final Supplier<CreativeModeTab> COMBS_TAB = CREATIVE_TABS.register("combs_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + Beemancer.MOD_ID + ".combs"))
                    .icon(() -> new ItemStack(BeemancerItems.ROYAL_COMB.get()))
                    .displayItems((parameters, output) -> {
                        addAlchemyIngredients(output);
                        addLegacyCombs(output);
                        addAllCombs(output);
                        addAllPollens(output);
                        addAllFragments(output);
                        addAllShards(output);
                        addAllDusts(output);
                        addAllDustFragments(output);
                    })
                    .build());

    // =========================================================================
    // HELPER METHODS - MAIN TAB
    // =========================================================================

    private static void addCodexAndTools(CreativeModeTab.Output output) {
        output.accept(BeemancerItems.CODEX.get());
        output.accept(BeemancerItems.BEE_WAND.get());
        output.accept(BeemancerItems.BUILDING_WAND.get());
    }

    private static void addBeeMachines(CreativeModeTab.Output output) {
        output.accept(BeemancerItems.BEE_CREATOR.get());
        output.accept(BeemancerItems.MAGIC_HIVE.get());
        output.accept(BeemancerItems.INCUBATOR.get());
        output.accept(BeemancerItems.BREEDING_CRYSTAL.get());
        output.accept(BeemancerItems.HIVE_MULTIBLOCK.get());
    }

    private static void addAlchemyMachines(CreativeModeTab.Output output) {
        output.accept(BeemancerItems.MANUAL_CENTRIFUGE.get());
        output.accept(BeemancerItems.POWERED_CENTRIFUGE.get());
        output.accept(BeemancerItems.POWERED_CENTRIFUGE_TIER2.get());
        output.accept(BeemancerItems.POWERED_CENTRIFUGE_TIER3.get());
        output.accept(BeemancerItems.CRYSTALLIZER.get());
        output.accept(BeemancerItems.ALEMBIC.get());
        output.accept(BeemancerItems.INFUSER.get());
        output.accept(BeemancerItems.INFUSER_TIER2.get());
        output.accept(BeemancerItems.INFUSER_TIER3.get());
    }

    private static void addPipesAndTanks(CreativeModeTab.Output output) {
        // Tanks
        output.accept(BeemancerItems.HONEY_TANK.get());
        output.accept(BeemancerItems.MULTIBLOCK_TANK.get());
        output.accept(BeemancerItems.CREATIVE_TANK.get());

        // Honey Pipes
        output.accept(BeemancerItems.HONEY_PIPE.get());
        output.accept(BeemancerItems.HONEY_PIPE_TIER2.get());
        output.accept(BeemancerItems.HONEY_PIPE_TIER3.get());
        output.accept(BeemancerItems.HONEY_PIPE_TIER4.get());

        // Item Pipes
        output.accept(BeemancerItems.ITEM_PIPE.get());
        output.accept(BeemancerItems.ITEM_PIPE_TIER2.get());
        output.accept(BeemancerItems.ITEM_PIPE_TIER3.get());
        output.accept(BeemancerItems.ITEM_PIPE_TIER4.get());
    }

    private static void addFluidBuckets(CreativeModeTab.Output output) {
        output.accept(BeemancerItems.HONEY_BUCKET.get());
        output.accept(BeemancerItems.ROYAL_JELLY_BUCKET.get());
        output.accept(BeemancerItems.NECTAR_BUCKET.get());
    }

    private static void addStorageBlocks(CreativeModeTab.Output output) {
        output.accept(BeemancerItems.STORAGE_CRATE.get());
        output.accept(BeemancerItems.STORAGE_CONTROLLER.get());
        output.accept(BeemancerItems.STORAGE_TERMINAL.get());
    }

    private static void addHoneyAltarBlocks(CreativeModeTab.Output output) {
        output.accept(BeemancerItems.HONEYED_STONE.get());
        output.accept(BeemancerItems.HONEYED_STONE_STAIR.get());
        output.accept(BeemancerItems.HONEYED_SLAB.get());
        output.accept(BeemancerItems.HONEY_PEDESTAL.get());
        output.accept(BeemancerItems.HONEY_CRYSTAL_CONDUIT.get());
        output.accept(BeemancerItems.HONEY_CRYSTAL.get());
    }

    private static void addCrystalItems(CreativeModeTab.Output output) {
        output.accept(BeemancerItems.RAW_CRYSTAL_SHARD.get());
        output.accept(BeemancerItems.ENRICHED_CRYSTAL_SHARD.get());
        output.accept(BeemancerItems.RADIANT_CRYSTAL_SHARD.get());
        output.accept(BeemancerItems.ENRICHED_HONEY_CRYSTAL.get());
        output.accept(BeemancerItems.RADIANT_HONEY_CRYSTAL.get());
        output.accept(BeemancerItems.ROYAL_HONEY_CRYSTAL.get());
    }

    // =========================================================================
    // HELPER METHODS - COMBS TAB
    // =========================================================================

    private static void addAlchemyIngredients(CreativeModeTab.Output output) {
        output.accept(BeemancerItems.BEESWAX.get());
        output.accept(BeemancerItems.PROPOLIS.get());
        output.accept(BeemancerItems.POLLEN.get());
        output.accept(BeemancerItems.HONEYED_WOOD.get());
    }

    private static void addLegacyCombs(CreativeModeTab.Output output) {
        output.accept(BeemancerItems.COMMON_COMB.get());
        output.accept(BeemancerItems.NOBLE_COMB.get());
        output.accept(BeemancerItems.DILIGENT_COMB.get());
        output.accept(BeemancerItems.ROYAL_COMB.get());
    }

    private static void addAllCombs(CreativeModeTab.Output output) {
        // Tier I combs
        output.accept(BeemancerItems.MEADOW_COMB.get());
        output.accept(BeemancerItems.FOREST_COMB.get());
        output.accept(BeemancerItems.RIVER_COMB.get());
        output.accept(BeemancerItems.NETHER_COMB.get());
        output.accept(BeemancerItems.END_COMB.get());

        // Tier II combs
        output.accept(BeemancerItems.MONSTY_COMB.get());
        output.accept(BeemancerItems.DOCILE_COMB.get());
        output.accept(BeemancerItems.SPARK_COMB.get());
        output.accept(BeemancerItems.CULTURAL_COMB.get());

        // Tier III combs
        output.accept(BeemancerItems.CARBON_COMB.get());
        output.accept(BeemancerItems.CUPRIC_COMB.get());
        output.accept(BeemancerItems.FESTERING_COMB.get());
        output.accept(BeemancerItems.SKELETAL_COMB.get());
        output.accept(BeemancerItems.ARACHNID_COMB.get());
        output.accept(BeemancerItems.TANNER_COMB.get());
        output.accept(BeemancerItems.QUERCUS_COMB.get());
        output.accept(BeemancerItems.BOREAL_COMB.get());
        output.accept(BeemancerItems.TROPICAL_COMB.get());
        output.accept(BeemancerItems.PAPERBARK_COMB.get());
        output.accept(BeemancerItems.REED_COMB.get());

        // Tier IV combs
        output.accept(BeemancerItems.FLOWER_COMB.get());
        output.accept(BeemancerItems.MUSHROOM_COMB.get());
        output.accept(BeemancerItems.TREE_COMB.get());
        output.accept(BeemancerItems.CRYSTAL_COMB.get());
        output.accept(BeemancerItems.ARGIL_COMB.get());
        output.accept(BeemancerItems.UMBRA_COMB.get());
        output.accept(BeemancerItems.SAVANNA_COMB.get());
        output.accept(BeemancerItems.FROST_COMB.get());
        output.accept(BeemancerItems.VOLATILE_COMB.get());

        // Tier V combs
        output.accept(BeemancerItems.SWIFT_COMB.get());
        output.accept(BeemancerItems.FERROUS_COMB.get());
        output.accept(BeemancerItems.FLUX_COMB.get());
        output.accept(BeemancerItems.LAZULI_COMB.get());
        output.accept(BeemancerItems.QUARTZOSE_COMB.get());
        output.accept(BeemancerItems.LUMINOUS_COMB.get());
        output.accept(BeemancerItems.VISCOUS_COMB.get());
        output.accept(BeemancerItems.INKY_COMB.get());
        output.accept(BeemancerItems.GEODE_COMB.get());
        output.accept(BeemancerItems.MARSH_COMB.get());

        // Tier VI combs
        output.accept(BeemancerItems.MAJESTIC_COMB.get());
        output.accept(BeemancerItems.STEADY_COMB.get());
        output.accept(BeemancerItems.AURIC_COMB.get());
        output.accept(BeemancerItems.MAGMATIC_COMB.get());
        output.accept(BeemancerItems.CRIMSON_COMB.get());

        // Tier VII combs
        output.accept(BeemancerItems.TREASURE_COMB.get());
        output.accept(BeemancerItems.SIPHONING_COMB.get());
        output.accept(BeemancerItems.ZEPHYR_COMB.get());
        output.accept(BeemancerItems.PRISMATIC_COMB.get());
        output.accept(BeemancerItems.CRYSTALLINE_COMB.get());
        output.accept(BeemancerItems.BLAZING_COMB.get());

        // Tier VIII combs
        output.accept(BeemancerItems.DIAMANTINE_COMB.get());
        output.accept(BeemancerItems.VENERABLE_COMB.get());
        output.accept(BeemancerItems.VOLCANIC_COMB.get());
        output.accept(BeemancerItems.TRAVELER_COMB.get());
        output.accept(BeemancerItems.SORROW_COMB.get());
        output.accept(BeemancerItems.LIGHT_COMB.get());
        output.accept(BeemancerItems.DARK_COMB.get());
        output.accept(BeemancerItems.IMPERIAL_COMB.get());

        // Tier IX combs
        output.accept(BeemancerItems.DEMONIC_COMB.get());
        output.accept(BeemancerItems.PALADIN_COMB.get());
        output.accept(BeemancerItems.ANCIENT_COMB.get());
        output.accept(BeemancerItems.VOID_COMB.get());

        // Tier X combs
        output.accept(BeemancerItems.DRACONIC_COMB.get());
        output.accept(BeemancerItems.STELLAR_COMB.get());
    }

    private static void addAllPollens(CreativeModeTab.Output output) {
        output.accept(BeemancerItems.FLOWER_POLLEN.get());
        output.accept(BeemancerItems.MUSHROOM_SPORE.get());
        output.accept(BeemancerItems.TREE_POLLEN.get());
        output.accept(BeemancerItems.CRYSTAL_POLLEN.get());
        output.accept(BeemancerItems.POLLEN_OF_WIND.get());
        output.accept(BeemancerItems.POLLEN_OF_THUNDER.get());
        output.accept(BeemancerItems.POLLEN_OF_WATER.get());
        output.accept(BeemancerItems.POLLEN_OF_FIRE.get());
        output.accept(BeemancerItems.POLLEN_OF_LIGHT.get());
        output.accept(BeemancerItems.POLLEN_OF_DARKNESS.get());
        output.accept(BeemancerItems.VOID_POLLEN.get());
    }

    private static void addAllFragments(CreativeModeTab.Output output) {
        output.accept(BeemancerItems.COAL_FRAGMENT.get());
        output.accept(BeemancerItems.LEATHER_FRAGMENT.get());
        output.accept(BeemancerItems.OAK_LOG_FRAGMENT.get());
        output.accept(BeemancerItems.SPRUCE_LOG_FRAGMENT.get());
        output.accept(BeemancerItems.JUNGLE_LOG_FRAGMENT.get());
        output.accept(BeemancerItems.BIRCH_LOG_FRAGMENT.get());
        output.accept(BeemancerItems.DARK_OAK_LOG_FRAGMENT.get());
        output.accept(BeemancerItems.ACACIA_LOG_FRAGMENT.get());
        output.accept(BeemancerItems.MANGROVE_LOG_FRAGMENT.get());
        output.accept(BeemancerItems.ICE_FRAGMENT.get());
        output.accept(BeemancerItems.GUNPOWDER_FRAGMENT.get());
        output.accept(BeemancerItems.BLAZE_ROD_FRAGMENT.get());
    }

    private static void addAllShards(CreativeModeTab.Output output) {
        output.accept(BeemancerItems.RAW_COPPER_SHARD.get());
        output.accept(BeemancerItems.RAW_IRON_SHARD.get());
        output.accept(BeemancerItems.RAW_GOLD_SHARD.get());
        output.accept(BeemancerItems.LAPIS_LAZULI_SHARD.get());
        output.accept(BeemancerItems.QUARTZ_SHARD.get());
        output.accept(BeemancerItems.GLOWSTONE_DUST_SHARD.get());
        output.accept(BeemancerItems.SLIME_BALL_SHARD.get());
        output.accept(BeemancerItems.MAGMA_CREAM_SHARD.get());
        output.accept(BeemancerItems.GHAST_TEAR_SHARD.get());
    }

    private static void addAllDusts(CreativeModeTab.Output output) {
        output.accept(BeemancerItems.DIAMOND_DUST.get());
        output.accept(BeemancerItems.EMERALD_DUST.get());
        output.accept(BeemancerItems.OBSIDIAN_DUST.get());
        output.accept(BeemancerItems.ENDER_PEARL_DUST.get());
        output.accept(BeemancerItems.NETHERITE_SCRAP_DUST.get());
        output.accept(BeemancerItems.DRAGON_BREATH_DUST.get());
        output.accept(BeemancerItems.NETHER_STAR_DUST.get());
    }

    private static void addAllDustFragments(CreativeModeTab.Output output) {
        output.accept(BeemancerItems.DIAMOND_FRAGMENT.get());
        output.accept(BeemancerItems.EMERALD_FRAGMENT.get());
        output.accept(BeemancerItems.OBSIDIAN_FRAGMENT.get());
        output.accept(BeemancerItems.ENDER_PEARL_FRAGMENT.get());
        output.accept(BeemancerItems.NETHERITE_SCRAP_FRAGMENT.get());
        output.accept(BeemancerItems.DRAGON_BREATH_FRAGMENT.get());
        output.accept(BeemancerItems.NETHER_STAR_FRAGMENT.get());
    }

    // =========================================================================
    // REGISTRATION
    // =========================================================================

    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }
}
