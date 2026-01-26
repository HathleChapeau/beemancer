/**
 * ============================================================
 * [BeeStatueBlockEntity.java]
 * Description: BlockEntity stockant l'espèce affichée sur la statue
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation           |
 * |---------------------|----------------------|-----------------------|
 * | BeeSpeciesManager   | Liste des espèces    | Cycle entre espèces   |
 * | BeemancerBlockEntities | Type BlockEntity  | Création              |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - BeeStatueBlock.java (création BlockEntity)
 * - BeeStatueRenderer.java (rendu)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.statue;

import com.chapeau.beemancer.core.bee.BeeSpeciesManager;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * BlockEntity de la statue d'abeille.
 * Stocke l'espèce actuellement affichée.
 */
public class BeeStatueBlockEntity extends BlockEntity {

    private String speciesId = "meadow";

    // Cache pour éviter de recréer/trier la liste à chaque appel
    private List<String> cachedSortedSpecies = null;
    private int cachedSpeciesHash = 0;

    public BeeStatueBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.BEE_STATUE.get(), pos, state);
    }

    // ==================== Species Management ====================

    public String getSpeciesId() {
        return speciesId;
    }

    public void setSpeciesId(String speciesId) {
        this.speciesId = speciesId;
        setChanged();
        syncToClient();
    }

    /**
     * Retourne la liste triée des espèces avec cache.
     */
    private List<String> getSortedSpeciesList() {
        Set<String> allIds = BeeSpeciesManager.getAllSpeciesIds();
        int currentHash = allIds.hashCode();

        if (cachedSortedSpecies == null || cachedSpeciesHash != currentHash) {
            cachedSortedSpecies = new ArrayList<>(allIds);
            cachedSortedSpecies.sort(String::compareTo);
            cachedSpeciesHash = currentHash;
        }

        return cachedSortedSpecies;
    }

    /**
     * Passe à l'espèce suivante dans la liste.
     */
    public void cycleToNextSpecies() {
        List<String> sortedIds = getSortedSpeciesList();
        if (sortedIds.isEmpty()) return;

        int currentIndex = sortedIds.indexOf(speciesId);
        // Si espèce non trouvée (-1), on commence à 0
        int nextIndex = (currentIndex + 1) % sortedIds.size();

        setSpeciesId(sortedIds.get(nextIndex));
    }

    /**
     * Retourne le nombre total d'espèces disponibles.
     */
    public int getTotalSpeciesCount() {
        return getSortedSpeciesList().size();
    }

    /**
     * Retourne l'index actuel de l'espèce (1-based pour affichage).
     */
    public int getCurrentSpeciesIndex() {
        List<String> sortedIds = getSortedSpeciesList();
        if (sortedIds.isEmpty()) return 0;

        int index = sortedIds.indexOf(speciesId);
        // Si espèce non trouvée, retourner 1 (premier élément)
        return (index < 0 ? 0 : index) + 1;
    }

    // ==================== Sync ====================

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("SpeciesId", speciesId);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("SpeciesId")) {
            speciesId = tag.getString("SpeciesId");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
