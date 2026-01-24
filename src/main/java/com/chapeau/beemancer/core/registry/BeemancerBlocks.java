/**
 * ============================================================
 * [BeemancerBlocks.java]
 * Description: Registre centralisé de tous les blocs du mod
 * ============================================================
 */
package com.chapeau.beemancer.core.registry;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.block.alchemy.*;
import com.chapeau.beemancer.common.block.beecreator.BeeCreatorBlock;
import com.chapeau.beemancer.common.block.crystal.MagicBreedingCrystalBlock;
import com.chapeau.beemancer.common.block.hive.MagicHiveBlock;
import com.chapeau.beemancer.common.block.incubator.IncubatorBlock;
import com.chapeau.beemancer.common.block.storage.StorageCrateBlock;
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

    public static final DeferredBlock<HoneyTankBlock> HONEY_TANK = BLOCKS.register("honey_tank",
            () -> new HoneyTankBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()));

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

    public static final DeferredBlock<ItemPipeBlock> ITEM_PIPE = BLOCKS.register("item_pipe",
            () -> new ItemPipeBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.COPPER)
                    .noOcclusion()));

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
