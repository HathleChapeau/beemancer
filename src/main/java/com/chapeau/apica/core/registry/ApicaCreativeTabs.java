/**
 * ============================================================
 * [ApicaCreativeTabs.java]
 * Description: Registre des onglets du mode créatif
 * ============================================================
 *
 * Onglets:
 * - MAIN_TAB: Abeilles, machines, outils, stockage, blocs décoratifs
 * - COMBS_TAB: Combs, pollens, fragments, shards, dusts
 *
 * ============================================================
 */
package com.chapeau.apica.core.registry;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.item.magazine.MagazineItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ApicaCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Apica.MOD_ID);

    // =========================================================================
    // MAIN TAB - Machines, Tools, Storage, Decorative Blocks
    // =========================================================================

    public static final Supplier<CreativeModeTab> MAIN_TAB = CREATIVE_TABS.register("main_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + Apica.MOD_ID + ".main"))
                    .icon(() -> new ItemStack(ApicaItems.CODEX.get()))
                    .displayItems((parameters, output) -> {
                        addCodexAndTools(output);
                        addBees(output);
                        addBeeMachines(output);
                        addAlchemyMachines(output);
                        addPipesAndTanks(output);
                        addFluidBuckets(output);
                        addStorageBlocks(output);
                        addHoneyAltarBlocks(output);
                        addEssenceExtractor(output);
                        addEssences(output);
                        addCrystalItems(output);
                        addBeeStatue(output);
                        addBuildingBlocks(output);
                        addCraftingMaterials(output);
                    })
                    .build());


    // =========================================================================
    // COMBS TAB - Combs, Pollens, Fragments, Shards, Dusts
    // =========================================================================

    public static final Supplier<CreativeModeTab> COMBS_TAB = CREATIVE_TABS.register("combs_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + Apica.MOD_ID + ".combs"))
                    .icon(() -> new ItemStack(Items.HONEYCOMB))
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
    // HOVERBIKE TAB - Hoverbike Parts and Assembly
    // =========================================================================

    public static final Supplier<CreativeModeTab> HOVERBIKE_TAB = CREATIVE_TABS.register("hoverbike_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + Apica.MOD_ID + ".hoverbike"))
                    .icon(() -> new ItemStack(ApicaItems.HOVERBIKE_SPAWN.get()))
                    .displayItems((parameters, output) -> {
                        addHoverbikeParts(output);
                    })
                    .build());

    // =========================================================================
    // DEBUG TAB - Debug and Creative Tools
    // =========================================================================

    public static final Supplier<CreativeModeTab> DEBUG_TAB = CREATIVE_TABS.register("debug_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + Apica.MOD_ID + ".debug"))
                    .icon(() -> new ItemStack(ApicaItems.DEBUG_WAND.get()))
                    .displayItems((parameters, output) -> {
                        addDebugTools(output);
                    })
                    .build());

    // =========================================================================
    // HELPER METHODS - MAIN TAB
    // =========================================================================

    private static void addCodexAndTools(CreativeModeTab.Output output) {
        output.accept(ApicaItems.CODEX.get());
        output.accept(ApicaItems.SCOOP.get());
        output.accept(ApicaItems.RESONATOR.get());
        output.accept(ApicaItems.LEAF_BLOWER.get());
        output.accept(ApicaItems.MINING_LASER.get());
        output.accept(ApicaItems.RAILGUN.get());
        output.accept(ApicaItems.CHOPPER_HIVE.get());
        // Magazines
        output.accept(ApicaItems.MAGAZINE.get());
        output.accept(MagazineItem.createFilled("apica:honey", 1000));
        output.accept(MagazineItem.createFilled("apica:royal_jelly", 1000));
        output.accept(MagazineItem.createFilled("apica:nectar", 1000));
        // Backpack
        output.accept(ApicaItems.BACKPACK.get());
    }

    private static void addBees(CreativeModeTab.Output output) {
        output.accept(ApicaItems.MAGIC_BEE.get());
        output.accept(ApicaItems.BEE_LARVA.get());
    }

    private static void addBeeMachines(CreativeModeTab.Output output) {
        output.accept(ApicaItems.MAGIC_HIVE.get());
        output.accept(ApicaItems.INCUBATOR.get());
        output.accept(ApicaItems.ANTIBREEDING_CRYSTAL.get());
        output.accept(ApicaItems.HIVE_MULTIBLOCK.get());
        output.accept(ApicaItems.BEE_NEST.get());
        output.accept(ApicaItems.BEE_CREATOR.get());
    }

    private static void addAlchemyMachines(CreativeModeTab.Output output) {
        output.accept(ApicaItems.MANUAL_CENTRIFUGE.get());
        output.accept(ApicaItems.POWERED_CENTRIFUGE.get());
        output.accept(ApicaItems.POWERED_CENTRIFUGE_TIER2.get());
        output.accept(ApicaItems.CENTRIFUGE_HEART.get());
        output.accept(ApicaItems.CRYSTALLIZER.get());
        output.accept(ApicaItems.ALEMBIC_HEART.get());
        output.accept(ApicaItems.INFUSER.get());
        // output.accept(ApicaItems.INFUSER_TIER2.get());
        // output.accept(ApicaItems.INFUSER_HEART.get());
        output.accept(ApicaItems.POLLEN_POT.get());
        output.accept(ApicaItems.CRANK.get());
        output.accept(ApicaItems.HONEY_FURNACE.get());
        output.accept(ApicaItems.ROYAL_FURNACE.get());
        output.accept(ApicaItems.NECTAR_FURNACE.get());
        output.accept(ApicaItems.HONEY_LAMP.get());
        output.accept(ApicaItems.UNCRAFTING_TABLE.get());
        output.accept(ApicaItems.LAUNCHPAD.get());
    }

    private static void addPipesAndTanks(CreativeModeTab.Output output) {
        // Tanks
        output.accept(ApicaItems.HONEY_TANK.get());
        output.accept(ApicaItems.MULTIBLOCK_TANK.get());

        // Liquid Pipes
        output.accept(ApicaItems.LIQUID_PIPE.get());
        output.accept(ApicaItems.LIQUID_PIPE_MK2.get());
        output.accept(ApicaItems.LIQUID_PIPE_MK3.get());
        output.accept(ApicaItems.LIQUID_PIPE_MK4.get());

        // Item Pipes
        output.accept(ApicaItems.ITEM_PIPE.get());
        output.accept(ApicaItems.ITEM_PIPE_MK2.get());
        output.accept(ApicaItems.ITEM_PIPE_MK3.get());
        output.accept(ApicaItems.ITEM_PIPE_MK4.get());

        // Item Filter
        output.accept(ApicaItems.ITEM_FILTER.get());
    }

    private static void addFluidBuckets(CreativeModeTab.Output output) {
        output.accept(ApicaItems.HONEY_BUCKET.get());
        output.accept(ApicaItems.ROYAL_JELLY_BUCKET.get());
        output.accept(ApicaItems.NECTAR_BUCKET.get());
        output.accept(ApicaItems.ROYAL_JELLY_BOTTLE.get());
        output.accept(ApicaItems.NECTAR_BOTTLE.get());
    }

    private static void addStorageBlocks(CreativeModeTab.Output output) {
        output.accept(ApicaItems.STORAGE_CONTROLLER.get());
        output.accept(ApicaItems.STORAGE_TERMINAL.get());
        output.accept(ApicaItems.STORAGE_RELAY.get());
        output.accept(ApicaItems.IMPORT_INTERFACE.get());
        output.accept(ApicaItems.EXPORT_INTERFACE.get());
        output.accept(ApicaItems.CONTROLLED_HIVE.get());
        output.accept(ApicaItems.STORAGE_HIVE.get());
        output.accept(ApicaItems.STORAGE_HIVE_TIER2.get());
        output.accept(ApicaItems.STORAGE_HIVE_TIER3.get());

        // Storage Barrels
        output.accept(ApicaItems.STORAGE_BARREL_MK1.get());
        output.accept(ApicaItems.STORAGE_BARREL_MK2.get());
        output.accept(ApicaItems.STORAGE_BARREL_MK3.get());
        output.accept(ApicaItems.STORAGE_BARREL_MK4.get());
        output.accept(ApicaItems.VOID_UPGRADE.get());
        output.accept(ApicaItems.BARREL_MK2_UPGRADE.get());
        output.accept(ApicaItems.BARREL_MK3_UPGRADE.get());
        output.accept(ApicaItems.BARREL_MK4_UPGRADE.get());

        // Wooden Frame
        output.accept(ApicaItems.WOODEN_FRAME.get());

        // Trash Cans
        output.accept(ApicaItems.TRASH_CAN.get());
        output.accept(ApicaItems.LIQUID_TRASH_CAN.get());
    }

    private static void addHoneyAltarBlocks(CreativeModeTab.Output output) {
        output.accept(ApicaItems.HONEYED_STONE.get());
        output.accept(ApicaItems.HONEYED_STONE_STAIR.get());
        output.accept(ApicaItems.HONEYED_SLAB.get());
        output.accept(ApicaItems.HONEYED_STONE_BRICK.get());
        output.accept(ApicaItems.HONEYED_STONE_BRICK_STAIR.get());
        output.accept(ApicaItems.HONEYED_STONE_BRICK_SLAB.get());
        output.accept(ApicaItems.HONEY_RESERVOIR.get());
        output.accept(ApicaItems.HONEY_PEDESTAL.get());
        output.accept(ApicaItems.HONEY_CRYSTAL_CONDUIT.get());
        output.accept(ApicaItems.HONEY_CRYSTAL.get());
        output.accept(ApicaItems.ALTAR_HEART.get());
    }

    private static void addCrystalItems(CreativeModeTab.Output output) {
    }

    private static void addEssenceExtractor(CreativeModeTab.Output output) {
        output.accept(ApicaItems.EXTRACTOR_HEART.get());
        output.accept(ApicaItems.INJECTOR.get());
    }

    private static void addBeeStatue(CreativeModeTab.Output output) {
        output.accept(ApicaItems.BEE_STATUE.get());
        output.accept(ApicaItems.API.get());
    }

    private static void addDebugTools(CreativeModeTab.Output output) {
        output.accept(ApicaItems.DEBUG_WAND.get());
        output.accept(ApicaItems.BUILDING_STAFF.get());
        output.accept(ApicaItems.CREATIVE_TANK.get());
        output.accept(ApicaItems.CREATIVE_BREEDING_CRYSTAL.get());
        output.accept(ApicaItems.CREATIVE_TOLERANCE_CRYSTAL.get());
        output.accept(ApicaItems.CREATIVE_FOCUS.get());
        output.accept(ApicaItems.CREATIVE_MAGAZINE.get());
    }

    private static void addBuildingBlocks(CreativeModeTab.Output output) {
        // Honeyed Logs & Wood
        output.accept(ApicaItems.HONEYED_LOG.get());
        output.accept(ApicaItems.STRIPPED_HONEYED_LOG.get());
        output.accept(ApicaItems.HONEYED_WOOD.get());
        output.accept(ApicaItems.STRIPPED_HONEYED_WOOD.get());

        // Honeyed Planks
        output.accept(ApicaItems.HONEYED_PLANKS.get());
        output.accept(ApicaItems.HONEYED_WOOD_STAIR.get());
        output.accept(ApicaItems.HONEYED_WOOD_SLAB.get());
        output.accept(ApicaItems.HONEYED_FENCE.get());
        output.accept(ApicaItems.HONEYED_FENCE_GATE.get());
        output.accept(ApicaItems.HONEYED_TRAPDOOR.get());
        output.accept(ApicaItems.HONEYED_DOOR.get());
        output.accept(ApicaItems.HONEYED_BUTTON.get());

        // Honeyed Stone
        output.accept(ApicaItems.HONEYED_STONE_WALL.get());
        output.accept(ApicaItems.HONEYED_STONE_BRICK_WALL.get());
        output.accept(ApicaItems.HONEYED_STONE_BRICK_PANE.get());

        // Iron Foundation
        output.accept(ApicaItems.IRON_FOUNDATION.get());
        output.accept(ApicaItems.IRON_FOUNDATION_PANE.get());
        output.accept(ApicaItems.IRON_FOUNDATION_STAIR.get());
        output.accept(ApicaItems.IRON_FOUNDATION_SLAB.get());
        output.accept(ApicaItems.IRON_FOUNDATION_WALL.get());
        output.accept(ApicaItems.IRON_FOUNDATION_TRAPDOOR.get());
        output.accept(ApicaItems.IRON_FOUNDATION_DOOR.get());

        // Honeyed Glass
        output.accept(ApicaItems.HONEYED_GLASS.get());
        output.accept(ApicaItems.HONEYED_GLASS_PANE.get());
        output.accept(ApicaItems.HONEYED_PLANK_PANE.get());
        output.accept(ApicaItems.HONEYED_STONE_PANE.get());
    }

    private static void addCraftingMaterials(CreativeModeTab.Output output) {
        output.accept(ApicaItems.HONEYED_IRON.get());
        output.accept(ApicaItems.HONEYED_IRON_BLOCK.get());
        output.accept(ApicaItems.ROYAL_CRYSTAL.get());
        output.accept(ApicaItems.ROYAL_GOLD.get());
        output.accept(ApicaItems.ROYAL_GOLD_BLOCK.get());
        output.accept(ApicaItems.NECTAR_DIAMOND.get());
        output.accept(ApicaItems.NECTAR_DIAMOND_BLOCK.get());
        output.accept(ApicaItems.ROYAL_JELLY_BLOCK.get());
        output.accept(ApicaItems.NECTAR_BLOCK.get());
        output.accept(ApicaItems.NECTAR_CRYSTAL.get());
        output.accept(ApicaItems.BLANK_RUNE.get());
        output.accept(ApicaItems.HONEY_ARTIFACT_CORE.get());
        output.accept(ApicaItems.ROYAL_ARTIFACT_CORE.get());
        output.accept(ApicaItems.NECTAR_ARTIFACT_CORE.get());
        output.accept(ApicaItems.HONEY_BREAD.get());
        output.accept(ApicaItems.ROYAL_BREAD.get());
        output.accept(ApicaItems.NECTAR_BREAD.get());
    }

    private static void addEssences(CreativeModeTab.Output output) {
        // Drop Essences
        output.accept(ApicaItems.LESSER_DROP_ESSENCE.get());
        output.accept(ApicaItems.DROP_ESSENCE.get());
        output.accept(ApicaItems.GREATER_DROP_ESSENCE.get());
        output.accept(ApicaItems.PERFECT_DROP_ESSENCE.get());

        // Speed Essences
        output.accept(ApicaItems.LESSER_SPEED_ESSENCE.get());
        output.accept(ApicaItems.SPEED_ESSENCE.get());
        output.accept(ApicaItems.GREATER_SPEED_ESSENCE.get());
        output.accept(ApicaItems.PERFECT_SPEED_ESSENCE.get());

        // Foraging Essences
        output.accept(ApicaItems.LESSER_FORAGING_ESSENCE.get());
        output.accept(ApicaItems.FORAGING_ESSENCE.get());
        output.accept(ApicaItems.GREATER_FORAGING_ESSENCE.get());
        output.accept(ApicaItems.PERFECT_FORAGING_ESSENCE.get());

        // Tolerance Essences
        output.accept(ApicaItems.LESSER_TOLERANCE_ESSENCE.get());
        output.accept(ApicaItems.TOLERANCE_ESSENCE.get());
        output.accept(ApicaItems.GREATER_TOLERANCE_ESSENCE.get());
        output.accept(ApicaItems.PERFECT_TOLERANCE_ESSENCE.get());

        // Day/Night Essences
        output.accept(ApicaItems.DIURNAL_ESSENCE.get());
        output.accept(ApicaItems.NOCTURNAL_ESSENCE.get());
        output.accept(ApicaItems.INSOMNIA_ESSENCE.get());
        output.accept(ApicaItems.SPECIES_ESSENCE.get());
    }

    // =========================================================================
    // HELPER METHODS - HOVERBIKE TAB
    // =========================================================================

    private static void addHoverbikeParts(CreativeModeTab.Output output) {
        output.accept(ApicaItems.HOVERBIKE_SPAWN.get());
        output.accept(ApicaItems.ASSEMBLY_TABLE.get());
        // Saddle
        output.accept(ApicaItems.SADDLE_STANDARD.get());
        output.accept(ApicaItems.SADDLE_REINFORCED.get());
        output.accept(ApicaItems.SADDLE_LIGHT.get());
        // Wing Protector
        output.accept(ApicaItems.WING_PROTECTOR_STANDARD.get());
        output.accept(ApicaItems.WING_PROTECTOR_HEAVY.get());
        output.accept(ApicaItems.WING_PROTECTOR_AERODYNAMIC.get());
        // Control Left
        output.accept(ApicaItems.CONTROL_LEFT_STANDARD.get());
        output.accept(ApicaItems.CONTROL_LEFT_PRECISION.get());
        output.accept(ApicaItems.CONTROL_LEFT_RESPONSIVE.get());
        // Control Right
        output.accept(ApicaItems.CONTROL_RIGHT_STANDARD.get());
        output.accept(ApicaItems.CONTROL_RIGHT_EFFICIENT.get());
        output.accept(ApicaItems.CONTROL_RIGHT_BOOSTED.get());
    }

    // =========================================================================
    // HELPER METHODS - COMBS TAB
    // =========================================================================

    private static void addAlchemyIngredients(CreativeModeTab.Output output) {
    }

    private static void addLegacyCombs(CreativeModeTab.Output output) {
        output.accept(ApicaItems.ROYAL_COMB.get());
        output.accept(ApicaItems.BIG_ROYAL_COMB.get());
    }

    private static void addAllCombs(CreativeModeTab.Output output) {
        // Tier II combs
        output.accept(ApicaItems.SPARK_COMB.get());

        // Tier III combs
        output.accept(ApicaItems.CARBON_COMB.get());
        output.accept(ApicaItems.CUPRIC_COMB.get());
        output.accept(ApicaItems.FESTERING_COMB.get());
        output.accept(ApicaItems.SKELETAL_COMB.get());
        output.accept(ApicaItems.ARACHNID_COMB.get());
        output.accept(ApicaItems.TANNER_COMB.get());
        output.accept(ApicaItems.QUERCUS_COMB.get());
        output.accept(ApicaItems.BOREAL_COMB.get());
        output.accept(ApicaItems.TROPICAL_COMB.get());
        output.accept(ApicaItems.PAPERBARK_COMB.get());
        output.accept(ApicaItems.REED_COMB.get());

        // Tier IV combs
        output.accept(ApicaItems.ARGIL_COMB.get());
        output.accept(ApicaItems.UMBRA_COMB.get());
        output.accept(ApicaItems.SAVANNA_COMB.get());
        output.accept(ApicaItems.FROST_COMB.get());
        output.accept(ApicaItems.VOLATILE_COMB.get());

        // Tier V combs
        output.accept(ApicaItems.FERROUS_COMB.get());
        output.accept(ApicaItems.FLUX_COMB.get());
        output.accept(ApicaItems.LAZULI_COMB.get());
        output.accept(ApicaItems.QUARTZOSE_COMB.get());
        output.accept(ApicaItems.LUMINOUS_COMB.get());
        output.accept(ApicaItems.VISCOUS_COMB.get());
        output.accept(ApicaItems.INKY_COMB.get());
        output.accept(ApicaItems.GEODE_COMB.get());
        output.accept(ApicaItems.MARSH_COMB.get());

        // Tier VI combs
        output.accept(ApicaItems.MAJESTIC_COMB.get());
        output.accept(ApicaItems.AURIC_COMB.get());
        output.accept(ApicaItems.MAGMATIC_COMB.get());
        output.accept(ApicaItems.CRIMSON_COMB.get());

        // Tier VII combs
        output.accept(ApicaItems.PRISMATIC_COMB.get());
        output.accept(ApicaItems.CRYSTALLINE_COMB.get());
        output.accept(ApicaItems.BLAZING_COMB.get());

        // Tier VIII combs
        output.accept(ApicaItems.DIAMANTINE_COMB.get());
        output.accept(ApicaItems.VENERABLE_COMB.get());
        output.accept(ApicaItems.VOLCANIC_COMB.get());
        output.accept(ApicaItems.TRAVELER_COMB.get());
        output.accept(ApicaItems.SORROW_COMB.get());
        output.accept(ApicaItems.IMPERIAL_COMB.get());

        // Tier IX combs
        output.accept(ApicaItems.ANCIENT_COMB.get());

        // Tier X combs
        output.accept(ApicaItems.DRACONIC_COMB.get());
        output.accept(ApicaItems.STELLAR_COMB.get());
    }

    private static void addAllPollens(CreativeModeTab.Output output) {
        output.accept(ApicaItems.FLOWER_POLLEN.get());
        output.accept(ApicaItems.MUSHROOM_SPORE.get());
        output.accept(ApicaItems.TREE_POLLEN.get());
        output.accept(ApicaItems.CRYSTAL_POLLEN.get());
        output.accept(ApicaItems.POLLEN_OF_WIND.get());
        output.accept(ApicaItems.POLLEN_OF_THUNDER.get());
        output.accept(ApicaItems.POLLEN_OF_WATER.get());
        output.accept(ApicaItems.POLLEN_OF_FIRE.get());
        output.accept(ApicaItems.POLLEN_OF_LIGHT.get());
        output.accept(ApicaItems.POLLEN_OF_DARKNESS.get());
        output.accept(ApicaItems.VOID_POLLEN.get());
    }

    private static void addAllFragments(CreativeModeTab.Output output) {
        output.accept(ApicaItems.COAL_FRAGMENT.get());
        output.accept(ApicaItems.LEATHER_FRAGMENT.get());
        output.accept(ApicaItems.OAK_LOG_FRAGMENT.get());
        output.accept(ApicaItems.SPRUCE_LOG_FRAGMENT.get());
        output.accept(ApicaItems.JUNGLE_LOG_FRAGMENT.get());
        output.accept(ApicaItems.BIRCH_LOG_FRAGMENT.get());
        output.accept(ApicaItems.DARK_OAK_LOG_FRAGMENT.get());
        output.accept(ApicaItems.ACACIA_LOG_FRAGMENT.get());
        output.accept(ApicaItems.MANGROVE_LOG_FRAGMENT.get());
        output.accept(ApicaItems.ICE_FRAGMENT.get());
        output.accept(ApicaItems.GUNPOWDER_FRAGMENT.get());
        output.accept(ApicaItems.BLAZE_ROD_FRAGMENT.get());
    }

    private static void addAllShards(CreativeModeTab.Output output) {
        // Ore shards
        output.accept(ApicaItems.RAW_COPPER_SHARD.get());
        output.accept(ApicaItems.RAW_IRON_SHARD.get());
        output.accept(ApicaItems.RAW_GOLD_SHARD.get());
        output.accept(ApicaItems.LAPIS_LAZULI_SHARD.get());
        output.accept(ApicaItems.QUARTZ_SHARD.get());
        output.accept(ApicaItems.SLIME_BALL_SHARD.get());
        output.accept(ApicaItems.MAGMA_CREAM_SHARD.get());
        output.accept(ApicaItems.GHAST_TEAR_SHARD.get());

        // Shard fragments
        output.accept(ApicaItems.RAW_COPPER_FRAGMENT.get());
        output.accept(ApicaItems.RAW_IRON_FRAGMENT.get());
        output.accept(ApicaItems.RAW_GOLD_FRAGMENT.get());
        output.accept(ApicaItems.LAPIS_LAZULI_FRAGMENT.get());
        output.accept(ApicaItems.QUARTZ_FRAGMENT.get());
        output.accept(ApicaItems.SLIME_BALL_FRAGMENT.get());
        output.accept(ApicaItems.MAGMA_CREAM_FRAGMENT.get());
        output.accept(ApicaItems.GHAST_TEAR_FRAGMENT.get());
    }

    private static void addAllDusts(CreativeModeTab.Output output) {
        // Dusts
        output.accept(ApicaItems.DIAMOND_DUST.get());
        output.accept(ApicaItems.EMERALD_DUST.get());
        output.accept(ApicaItems.OBSIDIAN_DUST.get());
        output.accept(ApicaItems.ENDER_PEARL_DUST.get());
        output.accept(ApicaItems.NETHERITE_SCRAP_DUST.get());
        output.accept(ApicaItems.DRAGON_BREATH_DUST.get());
        output.accept(ApicaItems.NETHER_STAR_DUST.get());

        // Dust shards
        output.accept(ApicaItems.DIAMOND_SHARD.get());
        output.accept(ApicaItems.EMERALD_SHARD.get());
        output.accept(ApicaItems.OBSIDIAN_SHARD.get());
        output.accept(ApicaItems.ENDER_PEARL_SHARD.get());
        output.accept(ApicaItems.NETHERITE_SCRAP_SHARD.get());
        output.accept(ApicaItems.DRAGON_BREATH_SHARD.get());
        output.accept(ApicaItems.NETHER_STAR_SHARD.get());
    }

    private static void addAllDustFragments(CreativeModeTab.Output output) {
        output.accept(ApicaItems.DIAMOND_FRAGMENT.get());
        output.accept(ApicaItems.EMERALD_FRAGMENT.get());
        output.accept(ApicaItems.OBSIDIAN_FRAGMENT.get());
        output.accept(ApicaItems.ENDER_PEARL_FRAGMENT.get());
        output.accept(ApicaItems.NETHERITE_SCRAP_FRAGMENT.get());
        output.accept(ApicaItems.DRAGON_BREATH_FRAGMENT.get());
        output.accept(ApicaItems.NETHER_STAR_FRAGMENT.get());
    }

    // =========================================================================
    // REGISTRATION
    // =========================================================================

    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }
}
