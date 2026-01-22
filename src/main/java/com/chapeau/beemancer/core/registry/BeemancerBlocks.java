/**
 * ============================================================
 * [BeemancerBlocks.java]
 * Description: Registre centralisé de tous les blocs du mod
 * ============================================================
 */
package com.chapeau.beemancer.core.registry;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.block.beecreator.BeeCreatorBlock;
import com.chapeau.beemancer.common.block.crystal.MagicBreedingCrystalBlock;
import com.chapeau.beemancer.common.block.hive.MagicHiveBlock;
import com.chapeau.beemancer.common.block.incubator.IncubatorBlock;
import com.chapeau.beemancer.common.block.storage.StorageCrateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
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

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
