/**
 * ============================================================
 * [BeemancerBlocks.java]
 * Description: Registre centralisé de tous les blocs du mod
 * ============================================================
 */
package com.chapeau.beemancer.core.registry;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.block.alchemy.*;
import com.chapeau.beemancer.common.block.altar.*;
import com.chapeau.beemancer.common.block.beecreator.BeeCreatorBlock;
import com.chapeau.beemancer.common.block.crystal.MagicBreedingCrystalBlock;
import com.chapeau.beemancer.common.block.hive.MagicHiveBlock;
import com.chapeau.beemancer.common.block.incubator.IncubatorBlock;
import com.chapeau.beemancer.common.block.storage.StorageControllerBlock;
import com.chapeau.beemancer.common.block.storage.StorageCrateBlock;
import com.chapeau.beemancer.common.block.storage.StorageTerminalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BeemancerBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Beemancer.MOD_ID);

    // --- STORAGE ---
    public static final DeferredBlock<Block> STORAGE_CRATE = BLOCKS.register("storage_crate",
            () -> new StorageCrateBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<StorageControllerBlock> STORAGE_CONTROLLER = BLOCKS.register("storage_controller",
            () -> new StorageControllerBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<StorageTerminalBlock> STORAGE_TERMINAL = BLOCKS.register("storage_terminal",
            () -> new StorageTerminalBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    // --- BEE MACHINES ---
    public static final DeferredBlock<Block> BEE_CREATOR = BLOCKS.register("bee_creator",
            () -> new BeeCreatorBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> MAGIC_HIVE = BLOCKS.register("magic_hive",
            () -> new MagicHiveBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)));

    public static final DeferredBlock<Block> INCUBATOR = BLOCKS.register("incubator",
            () -> new IncubatorBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .requiresCorrectToolForDrops()));

    // --- BREEDING ---
    public static final DeferredBlock<Block> BREEDING_CRYSTAL = BLOCKS.register("breeding_crystal",
            () -> new MagicBreedingCrystalBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 7)
                    .noOcclusion()));

    // --- ALCHEMY MACHINES ---
    public static final DeferredBlock<ManualCentrifugeBlock> MANUAL_CENTRIFUGE = BLOCKS.register("manual_centrifuge",
            () -> new ManualCentrifugeBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .sound(SoundType.WOOD)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<PoweredCentrifugeBlock> POWERED_CENTRIFUGE = BLOCKS.register("powered_centrifuge",
            () -> new PoweredCentrifugeBlock(BlockBehaviour.Properties.of()
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<PoweredCentrifugeBlock> POWERED_CENTRIFUGE_TIER2 = BLOCKS.register("powered_centrifuge_tier2",
            () -> new PoweredCentrifugeBlock(BlockBehaviour.Properties.of()
                    .strength(4.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops(), 2));

    public static final DeferredBlock<PoweredCentrifugeBlock> POWERED_CENTRIFUGE_TIER3 = BLOCKS.register("powered_centrifuge_tier3",
            () -> new PoweredCentrifugeBlock(BlockBehaviour.Properties.of()
                    .strength(4.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops(), 3));

    public static final DeferredBlock<HoneyTankBlock> HONEY_TANK = BLOCKS.register("honey_tank",
            () -> new HoneyTankBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()));

    public static final DeferredBlock<HoneyTankBlock> HONEY_TANK_TIER2 = BLOCKS.register("honey_tank_tier2",
            () -> new HoneyTankBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f)
                    .sound(SoundType.GLASS)
                    .noOcclusion(), 2));

    public static final DeferredBlock<HoneyTankBlock> HONEY_TANK_TIER3 = BLOCKS.register("honey_tank_tier3",
            () -> new HoneyTankBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .sound(SoundType.GLASS)
                    .noOcclusion(), 3));

    public static final DeferredBlock<CreativeTankBlock> CREATIVE_TANK = BLOCKS.register("creative_tank",
            () -> new CreativeTankBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()));

    public static final DeferredBlock<HoneyPipeBlock> HONEY_PIPE = BLOCKS.register("honey_pipe",
            () -> new HoneyPipeBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.COPPER)
                    .noOcclusion()));

    public static final DeferredBlock<HoneyPipeBlock> HONEY_PIPE_TIER2 = BLOCKS.register("honey_pipe_tier2",
            () -> new HoneyPipeBlock(BlockBehaviour.Properties.of()
                    .strength(1.8f)
                    .sound(SoundType.COPPER)
                    .noOcclusion(), 2));

    public static final DeferredBlock<HoneyPipeBlock> HONEY_PIPE_TIER3 = BLOCKS.register("honey_pipe_tier3",
            () -> new HoneyPipeBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.COPPER)
                    .noOcclusion(), 3));

    public static final DeferredBlock<HoneyPipeBlock> HONEY_PIPE_TIER4 = BLOCKS.register("honey_pipe_tier4",
            () -> new HoneyPipeBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f)
                    .sound(SoundType.COPPER)
                    .noOcclusion(), 4));

    public static final DeferredBlock<ItemPipeBlock> ITEM_PIPE = BLOCKS.register("item_pipe",
            () -> new ItemPipeBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.COPPER)
                    .noOcclusion()));

    public static final DeferredBlock<ItemPipeBlock> ITEM_PIPE_TIER2 = BLOCKS.register("item_pipe_tier2",
            () -> new ItemPipeBlock(BlockBehaviour.Properties.of()
                    .strength(1.8f)
                    .sound(SoundType.COPPER)
                    .noOcclusion(), 2));

    public static final DeferredBlock<ItemPipeBlock> ITEM_PIPE_TIER3 = BLOCKS.register("item_pipe_tier3",
            () -> new ItemPipeBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.COPPER)
                    .noOcclusion(), 3));

    public static final DeferredBlock<ItemPipeBlock> ITEM_PIPE_TIER4 = BLOCKS.register("item_pipe_tier4",
            () -> new ItemPipeBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f)
                    .sound(SoundType.COPPER)
                    .noOcclusion(), 4));

    public static final DeferredBlock<CrystallizerBlock> CRYSTALLIZER = BLOCKS.register("crystallizer",
            () -> new CrystallizerBlock(BlockBehaviour.Properties.of()
                    .strength(3.5f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> state.getValue(CrystallizerBlock.ACTIVE) ? 10 : 4)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<AlembicBlock> ALEMBIC = BLOCKS.register("alembic",
            () -> new AlembicBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .sound(SoundType.COPPER)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<InfuserBlock> INFUSER = BLOCKS.register("infuser",
            () -> new InfuserBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .sound(SoundType.WOOD)
                    .requiresCorrectToolForDrops()));

    // --- HONEY ALTAR ---
    public static final DeferredBlock<HoneyedStoneBlock> HONEYED_STONE = BLOCKS.register("honeyed_stone",
            () -> new HoneyedStoneBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<HoneyedStoneStairBlock> HONEYED_STONE_STAIR = BLOCKS.register("honeyed_stone_stair",
            () -> new HoneyedStoneStairBlock(
                    HONEYED_STONE.get().defaultBlockState(),
                    BlockBehaviour.Properties.of()
                            .strength(1.5f)
                            .sound(SoundType.STONE)
                            .requiresCorrectToolForDrops()));

    public static final DeferredBlock<HoneyPedestalBlock> HONEY_PEDESTAL = BLOCKS.register("honey_pedestal",
            () -> new HoneyPedestalBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<HoneyCrystalConduitBlock> HONEY_CRYSTAL_CONDUIT = BLOCKS.register("honey_crystal_conduit",
            () -> new HoneyCrystalConduitBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 8)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<HoneyCrystalBlock> HONEY_CRYSTAL = BLOCKS.register("honey_crystal",
            () -> new HoneyCrystalBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> state.getValue(HoneyCrystalBlock.FORMED) ? 15 : 10)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    // --- FLUID BLOCKS ---
    public static final DeferredBlock<LiquidBlock> HONEY_FLUID_BLOCK = BLOCKS.register("honey",
            () -> new LiquidBlock(BeemancerFluids.HONEY_SOURCE.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .replaceable().noCollission().strength(100.0F)
                    .pushReaction(PushReaction.DESTROY).noLootTable().liquid()));

    public static final DeferredBlock<LiquidBlock> ROYAL_JELLY_FLUID_BLOCK = BLOCKS.register("royal_jelly",
            () -> new LiquidBlock(BeemancerFluids.ROYAL_JELLY_SOURCE.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .replaceable().noCollission().strength(100.0F)
                    .pushReaction(PushReaction.DESTROY).noLootTable().liquid()));

    public static final DeferredBlock<LiquidBlock> NECTAR_FLUID_BLOCK = BLOCKS.register("nectar",
            () -> new LiquidBlock(BeemancerFluids.NECTAR_SOURCE.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .replaceable().noCollission().strength(100.0F)
                    .pushReaction(PushReaction.DESTROY).noLootTable().liquid()
                    .lightLevel(state -> 8)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
