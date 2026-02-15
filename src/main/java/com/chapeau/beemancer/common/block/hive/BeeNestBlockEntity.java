/**
 * ============================================================
 * [BeeNestBlockEntity.java]
 * Description: BlockEntity du nid d'abeille avec cycle spawn/retour/repos
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
 * - WildBeePatrolGoal.java (onBeeReturned)
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
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Nid d'abeille naturel avec cycle de vie:
 * - Spawne des abeilles qui patrouillent puis rentrent au nid
 * - L'abeille est supprimee en rentrant, et respawn apres un repos (10-20s)
 * - Si une abeille meurt, le nid perd 1 de capacite max (permanent)
 * - Capacite initiale: 3 abeilles
 */
public class BeeNestBlockEntity extends BlockEntity {

    private static final int DEFAULT_MAX_BEES = 3;
    private static final int SPAWN_INTERVAL = 200;
    private static final int MIN_RESPAWN_TICKS = 200;  // 10 secondes
    private static final int MAX_RESPAWN_TICKS = 400;  // 20 secondes

    private int maxBees = DEFAULT_MAX_BEES;
    private int tickCounter;
    private final List<UUID> activeBeeUUIDs = new ArrayList<>();
    private final List<Integer> respawnTimers = new ArrayList<>();

    public BeeNestBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.BEE_NEST.get(), pos, state);
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        tickRespawnTimers();
        detectDeadBees();

        tickCounter++;
        if (tickCounter < SPAWN_INTERVAL) return;
        tickCounter = 0;

        // Sync periodique au client (toutes les 10 ticks pour les timers)
        if (tickCounter % 10 == 0) {
            syncToClient();
        }

        // Spawn initial: s'il reste de la place et pas de respawn en attente
        int totalBees = activeBeeUUIDs.size() + respawnTimers.size();
        if (totalBees < maxBees) {
            spawnBee();
        }
    }

    /**
     * Decremente les timers de respawn et spawne les abeilles pretes.
     */
    private void tickRespawnTimers() {
        List<Integer> ready = new ArrayList<>();
        for (int i = 0; i < respawnTimers.size(); i++) {
            int remaining = respawnTimers.get(i) - 1;
            if (remaining <= 0) {
                ready.add(i);
            } else {
                respawnTimers.set(i, remaining);
            }
        }

        // Spawn les abeilles dont le timer est echoue (en ordre inverse pour les index)
        for (int i = ready.size() - 1; i >= 0; i--) {
            respawnTimers.remove((int) ready.get(i));
            spawnBee();
        }
    }

    /**
     * Detecte les abeilles mortes (tuees) et reduit maxBees pour chacune.
     */
    private void detectDeadBees() {
        if (!(level instanceof ServerLevel serverLevel)) return;

        activeBeeUUIDs.removeIf(uuid -> {
            Entity entity = serverLevel.getEntity(uuid);
            if (entity == null || entity.isRemoved() || !(entity instanceof MagicBeeEntity)) {
                // L'abeille est morte (pas retournee via onBeeReturned)
                maxBees = Math.max(0, maxBees - 1);
                setChanged();
                return true;
            }
            return false;
        });
    }

    /**
     * Appelee par WildBeePatrolGoal quand l'abeille revient au nid.
     * Retire l'abeille du monde et programme un respawn apres repos.
     */
    public void onBeeReturned(MagicBeeEntity bee) {
        // Retirer l'UUID AVANT de discard pour que detectDeadBees ne la compte pas comme morte
        activeBeeUUIDs.remove(bee.getUUID());

        // Programmer le respawn
        int restTime = MIN_RESPAWN_TICKS + level.getRandom().nextInt(
                MAX_RESPAWN_TICKS - MIN_RESPAWN_TICKS + 1);
        respawnTimers.add(restTime);

        bee.discard();
        setChanged();
        syncToClient();
    }

    private void spawnBee() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (maxBees <= 0) return;

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

        bee.setHomeNestPos(worldPosition);

        serverLevel.addFreshEntity(bee);
        activeBeeUUIDs.add(bee.getUUID());
        setChanged();
    }

    // --- Client Getters (for debug display) ---

    public int getMaxBees() {
        return maxBees;
    }

    public int getActiveBeeCount() {
        return activeBeeUUIDs.size();
    }

    public List<Integer> getRespawnTimers() {
        return respawnTimers;
    }

    // --- Client Sync ---

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("MaxBees", maxBees);
        tag.putInt("TickCounter", tickCounter);

        tag.putInt("ActiveBeeCount", activeBeeUUIDs.size());
        for (int i = 0; i < activeBeeUUIDs.size(); i++) {
            tag.putUUID("ActiveBee" + i, activeBeeUUIDs.get(i));
        }

        tag.putInt("RespawnCount", respawnTimers.size());
        for (int i = 0; i < respawnTimers.size(); i++) {
            tag.putInt("Respawn" + i, respawnTimers.get(i));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        maxBees = tag.contains("MaxBees") ? tag.getInt("MaxBees") : DEFAULT_MAX_BEES;
        tickCounter = tag.getInt("TickCounter");

        activeBeeUUIDs.clear();
        int activeCount = tag.getInt("ActiveBeeCount");
        for (int i = 0; i < activeCount; i++) {
            if (tag.hasUUID("ActiveBee" + i)) {
                activeBeeUUIDs.add(tag.getUUID("ActiveBee" + i));
            }
        }

        respawnTimers.clear();
        int respawnCount = tag.getInt("RespawnCount");
        for (int i = 0; i < respawnCount; i++) {
            respawnTimers.add(tag.getInt("Respawn" + i));
        }
    }
}
