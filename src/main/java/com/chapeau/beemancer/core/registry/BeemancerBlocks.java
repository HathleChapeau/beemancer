/**
 * ============================================================
 * [BeemancerBlocks.java]
 * Description: Registre centralisé de tous les blocs du mod
 * ============================================================
 * 
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation         |
 * |---------------------|----------------------|---------------------|
 * | Beemancer           | MOD_ID               | Clé du registre     |
 * | StorageCrateBlock   | Bloc à enregistrer   | Création instance   |
 * ------------------------------------------------------------
 * 
 * UTILISÉ PAR:
 * - Beemancer.java (enregistrement)
 * - BeemancerItems.java (BlockItems)
 * - BeemancerCreativeTabs.java (ajout aux tabs)
 * 
 * ============================================================
 */
package com.chapeau.beemancer.core.registry;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.block.storage.StorageCrateBlock;
import net.minecraft.world.level.block.Block;
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

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
