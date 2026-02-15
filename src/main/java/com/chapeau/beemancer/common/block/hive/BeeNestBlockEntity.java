/**
 * ============================================================
 * [BeeNestBlockEntity.java]
 * Description: BlockEntity du nid d'abeille avec spawning periodique
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance               | Raison                  | Utilisation                    |
 * |--------------------------|------------------------|--------------------------------|
 * | MagicBeeEntity           | Entite abeille         | Spawn dans le monde            |
 * | BeeGeneData              | Donnees genetiques     | Genes par defaut + espece      |
 * | GeneRegistry             | Registre genes         | Lookup gene espece             |
 * | BeemancerEntities        | Registre entites       | Creation MagicBee              |
 * | BeemancerBlockEntities   | Registre BE            | Type BlockEntity               |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BeeNestBlock.java (creation + ticker)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.hive;

import com.chapeau.beemancer.common.entity.bee.MagicBeeEntity;
import com.chapeau.beemancer.core.gene.BeeGeneData;
import com.chapeau.beemancer.core.gene.Gene;
import com.chapeau.beemancer.core.gene.GeneCategory;
import com.chapeau.beemancer.core.gene.GeneRegistry;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * Spawne periodiquement une MagicBee de l'espece correspondante.
 * Maximum 1 abeille active par nid. L'espece est lue depuis le blockstate.
 */
public class BeeNestBlockEntity extends BlockEntity {

    private static final int SPAWN_INTERVAL = 200;

    private int tickCounter;
    private UUID spawnedBeeUUID;

    public BeeNestBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.BEE_NEST.get(), pos, state);
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        tickCounter++;
        if (tickCounter < SPAWN_INTERVAL) return;
        tickCounter = 0;

        if (isSpawnedBeeAlive()) return;
        spawnedBeeUUID = null;

        spawnBee();
    }

    private boolean isSpawnedBeeAlive() {
        if (spawnedBeeUUID == null || !(level instanceof ServerLevel serverLevel)) return false;
        Entity entity = serverLevel.getEntity(spawnedBeeUUID);
        return entity != null && !entity.isRemoved() && entity instanceof MagicBeeEntity;
    }

    private void spawnBee() {
        if (!(level instanceof ServerLevel serverLevel)) return;

        String species = getBlockState().getValue(BeeNestBlock.SPECIES).getSerializedName();
        MagicBeeEntity bee = BeemancerEntities.MAGIC_BEE.get().create(level);
        if (bee == null) return;

        BlockPos spawnPos = worldPosition.above();
        bee.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                level.getRandom().nextFloat() * 360, 0);

        BeeGeneData geneData = new BeeGeneData();
        Gene speciesGene = GeneRegistry.getGene(GeneCategory.SPECIES, species);
        if (speciesGene != null) {
            geneData.setGene(speciesGene);
        }
        bee.getGeneData().copyFrom(geneData);
        for (Gene gene : geneData.getAllGenes()) {
            bee.setGene(gene);
        }

        serverLevel.addFreshEntity(bee);
        spawnedBeeUUID = bee.getUUID();
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("TickCounter", tickCounter);
        if (spawnedBeeUUID != null) {
            tag.putUUID("SpawnedBee", spawnedBeeUUID);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tickCounter = tag.getInt("TickCounter");
        if (tag.hasUUID("SpawnedBee")) {
            spawnedBeeUUID = tag.getUUID("SpawnedBee");
        }
    }
}
