/**
 * ============================================================
 * [BeeCreatorBlockEntity.java]
 * Description: Stocke les couleurs des 7 parties + le type de corps du Bee Creator
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance               | Raison                | Utilisation                    |
 * |--------------------------|----------------------|--------------------------------|
 * | ApicaBlockEntities       | Type enregistre      | Construction                   |
 * | BeeCreatorMenu           | Menu associe         | createMenu()                   |
 * | BeePart                  | Enum parties         | Indices couleurs               |
 * | BeeBodyType              | Types de corps       | Index body type                |
 * | ContainerData            | Sync serveur→client  | 7 couleurs + 1 body type       |
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

    /** 7 color slots + 4 type slots (body, wing, stinger, antenna). */
    public static final int DATA_COUNT = BeePart.COUNT + 4;
    public static final int BODY_TYPE_SLOT = BeePart.COUNT;
    public static final int WING_TYPE_SLOT = BeePart.COUNT + 1;
    public static final int STINGER_TYPE_SLOT = BeePart.COUNT + 2;
    public static final int ANTENNA_TYPE_SLOT = BeePart.COUNT + 3;

    private final int[] partColors = new int[BeePart.COUNT];
    private int bodyTypeIndex = 0;
    private int wingTypeIndex = 0;
    private int stingerTypeIndex = 0;
    private int antennaTypeIndex = 0;

    public final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= 0 && index < BeePart.COUNT) return partColors[index];
            if (index == BODY_TYPE_SLOT) return bodyTypeIndex;
            if (index == WING_TYPE_SLOT) return wingTypeIndex;
            if (index == STINGER_TYPE_SLOT) return stingerTypeIndex;
            if (index == ANTENNA_TYPE_SLOT) return antennaTypeIndex;
            return 0;
        }

        @Override
        public void set(int index, int value) {
            if (index >= 0 && index < BeePart.COUNT) {
                partColors[index] = value;
                setChanged();
            } else if (index == BODY_TYPE_SLOT) {
                bodyTypeIndex = value;
                setChanged();
            } else if (index == WING_TYPE_SLOT) {
                wingTypeIndex = value;
                setChanged();
            } else if (index == STINGER_TYPE_SLOT) {
                stingerTypeIndex = value;
                setChanged();
            } else if (index == ANTENNA_TYPE_SLOT) {
                antennaTypeIndex = value;
                setChanged();
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
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

    public int getBodyTypeIndex() { return bodyTypeIndex; }
    public int getWingTypeIndex() { return wingTypeIndex; }
    public int getStingerTypeIndex() { return stingerTypeIndex; }
    public int getAntennaTypeIndex() { return antennaTypeIndex; }

    public void setBodyType(int index) {
        this.bodyTypeIndex = BeeBodyType.byIndex(index).getIndex();
        setChanged();
        syncToClient();
    }

    public void setWingType(int index) {
        this.wingTypeIndex = BeeWingType.byIndex(index).getIndex();
        setChanged();
        syncToClient();
    }

    public void setStingerType(int index) {
        this.stingerTypeIndex = BeeStingerType.byIndex(index).getIndex();
        setChanged();
        syncToClient();
    }

    public void setAntennaType(int index) {
        this.antennaTypeIndex = BeeAntennaType.byIndex(index).getIndex();
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
        tag.putInt("BodyType", bodyTypeIndex);
        tag.putInt("WingType", wingTypeIndex);
        tag.putInt("StingerType", stingerTypeIndex);
        tag.putInt("AntennaType", antennaTypeIndex);
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
        if (tag.contains("BodyType")) bodyTypeIndex = tag.getInt("BodyType");
        if (tag.contains("WingType")) wingTypeIndex = tag.getInt("WingType");
        if (tag.contains("StingerType")) stingerTypeIndex = tag.getInt("StingerType");
        if (tag.contains("AntennaType")) antennaTypeIndex = tag.getInt("AntennaType");
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
        tag.putInt("BodyType", bodyTypeIndex);
        tag.putInt("WingType", wingTypeIndex);
        tag.putInt("StingerType", stingerTypeIndex);
        tag.putInt("AntennaType", antennaTypeIndex);
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
