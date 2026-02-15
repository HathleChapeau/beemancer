/**
 * ============================================================
 * [AssemblyTableBlockEntity.java]
 * Description: BlockEntity pour stocker une piece de moto sur la table
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | BeemancerBlockEntities  | Type de BlockEntity  | Enregistrement                 |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - AssemblyTableBlock.java: Clic droit pour placer/retirer piece
 * - AssemblyTableRenderer.java: Rendu de la piece sur le bloc
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.mount;

import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Stocke une piece de hoverbike sur l'Assembly Table.
 * Pattern identique a HoneyPedestalBlockEntity.
 */
public class AssemblyTableBlockEntity extends BlockEntity {

    private ItemStack storedItem = ItemStack.EMPTY;

    public AssemblyTableBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.ASSEMBLY_TABLE.get(), pos, state);
    }

    public ItemStack getStoredItem() {
        return storedItem;
    }

    public boolean isEmpty() {
        return storedItem.isEmpty();
    }

    /**
     * Place une piece sur la table.
     * @param stack L'item a placer (sera copie avec count=1)
     * @return true si l'item a ete place
     */
    public boolean placeItem(ItemStack stack) {
        if (!storedItem.isEmpty() || stack.isEmpty()) {
            return false;
        }
        storedItem = stack.copyWithCount(1);
        setChanged();
        syncToClient();
        return true;
    }

    /**
     * Retire la piece de la table.
     * @return L'item retire ou ItemStack.EMPTY si vide
     */
    public ItemStack removeItem() {
        if (storedItem.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = storedItem.copy();
        storedItem = ItemStack.EMPTY;
        setChanged();
        syncToClient();
        return removed;
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // =========================================================================
    // NBT SERIALIZATION
    // =========================================================================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!storedItem.isEmpty()) {
            tag.put("StoredItem", storedItem.save(registries, new CompoundTag()));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("StoredItem")) {
            storedItem = ItemStack.parse(registries, tag.getCompound("StoredItem"))
                    .orElse(ItemStack.EMPTY);
        } else {
            storedItem = ItemStack.EMPTY;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (!storedItem.isEmpty()) {
            tag.put("StoredItem", storedItem.save(registries, new CompoundTag()));
        }
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net,
                              ClientboundBlockEntityDataPacket pkt,
                              HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            loadAdditional(tag, registries);
        } else {
            storedItem = ItemStack.EMPTY;
        }
    }
}
