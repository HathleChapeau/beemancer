/**
 * ============================================================
 * [BeeCreatorBlockEntity.java]
 * Description: Stocke les couleurs des 7 parties d'abeille du Bee Creator
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance               | Raison                | Utilisation                    |
 * |--------------------------|----------------------|--------------------------------|
 * | ApicaBlockEntities       | Type enregistre      | Construction                   |
 * | BeeCreatorMenu           | Menu associe         | createMenu()                   |
 * | BeePart                  | Enum parties         | Indices couleurs               |
 * | ContainerData            | Sync serveur→client  | 7 couleurs                     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BeeCreatorBlock (newBlockEntity, openMenu)
 * - BeeCreatorMenu (server constructor)
 * - BeeCreatorUpdatePacket (setPartColor)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.beecreator;

import com.chapeau.apica.common.menu.BeeCreatorMenu;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class BeeCreatorBlockEntity extends BlockEntity implements MenuProvider {

    private final int[] partColors = new int[BeePart.COUNT];

    public final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= 0 && index < BeePart.COUNT) return partColors[index];
            return 0;
        }

        @Override
        public void set(int index, int value) {
            if (index >= 0 && index < BeePart.COUNT) {
                partColors[index] = value;
                setChanged();
            }
        }

        @Override
        public int getCount() {
            return BeePart.COUNT;
        }
    };

    public BeeCreatorBlockEntity(BlockPos pos, BlockState state) {
        super(ApicaBlockEntities.BEE_CREATOR.get(), pos, state);
        for (BeePart part : BeePart.values()) {
            partColors[part.getIndex()] = part.getDefaultColor();
        }
    }

    public int getPartColor(BeePart part) {
        return partColors[part.getIndex()];
    }

    public void setPartColor(BeePart part, int color) {
        partColors[part.getIndex()] = color;
        setChanged();
        syncToClient();
    }

    // ========== MENU ==========

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.apica.bee_creator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BeeCreatorMenu(containerId, playerInventory, containerData, worldPosition);
    }

    // ========== NBT ==========

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (BeePart part : BeePart.values()) {
            tag.putInt("Color_" + part.getId(), partColors[part.getIndex()]);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (BeePart part : BeePart.values()) {
            String key = "Color_" + part.getId();
            if (tag.contains(key)) {
                partColors[part.getIndex()] = tag.getInt(key);
            }
        }
    }

    // ========== CLIENT SYNC ==========

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        for (BeePart part : BeePart.values()) {
            tag.putInt("Color_" + part.getId(), partColors[part.getIndex()]);
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
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt,
                             HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            loadAdditional(tag, registries);
        }
    }
}
