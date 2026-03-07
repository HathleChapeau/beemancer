/**
 * ============================================================
 * [BeeCreatorMenu.java]
 * Description: Menu du Bee Creator — sync ContainerData pour 7 couleurs + 1 body type
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ApicaMenu           | Base menu            | Infrastructure                 |
 * | ApicaMenus          | Type registre        | BEE_CREATOR menu type          |
 * | BeePart             | Enum parties         | COUNT pour ContainerData       |
 * | BeeCreatorBlockEntity| DATA_COUNT           | Taille ContainerData (8)       |
 * | ContainerData       | Sync data            | 7 couleurs + 1 body type       |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BeeCreatorBlockEntity (createMenu)
 * - BeeCreatorScreen (GUI)
 * - ApicaMenus (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.apica.common.menu;

import com.chapeau.apica.common.block.beecreator.BeeAntennaType;
import com.chapeau.apica.common.block.beecreator.BeeBodyType;
import com.chapeau.apica.common.block.beecreator.BeeCreatorBlockEntity;
import com.chapeau.apica.common.block.beecreator.BeePart;
import com.chapeau.apica.common.block.beecreator.BeeStingerType;
import com.chapeau.apica.common.block.beecreator.BeeWingType;
import com.chapeau.apica.core.registry.ApicaMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class BeeCreatorMenu extends ApicaMenu {

    private final ContainerData data;
    private final BlockPos blockPos;

    /** Client constructor (from network). */
    public BeeCreatorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, new SimpleContainerData(BeeCreatorBlockEntity.DATA_COUNT), buf.readBlockPos());
    }

    /** Server constructor (from BlockEntity via openMenu buf callback). */
    public BeeCreatorMenu(int containerId, Inventory playerInventory, ContainerData data, BlockPos pos) {
        super(ApicaMenus.BEE_CREATOR.get(), containerId);
        this.data = data;
        this.blockPos = pos;
        addDataSlots(data);
    }

    public BlockPos getBlockPos() { return blockPos; }

    /** Couleur de la partie a l'index donne (0xRRGGBB). */
    public int getPartColor(BeePart part) {
        return data.get(part.getIndex());
    }

    /** Type de corps selectionne (sync via ContainerData slot 7). */
    public BeeBodyType getBodyType() {
        return BeeBodyType.byIndex(data.get(BeeCreatorBlockEntity.BODY_TYPE_SLOT));
    }

    /** Type d'ailes selectionne (sync via ContainerData slot 8). */
    public BeeWingType getWingType() {
        return BeeWingType.byIndex(data.get(BeeCreatorBlockEntity.WING_TYPE_SLOT));
    }

    /** Type de dard selectionne (sync via ContainerData slot 9). */
    public BeeStingerType getStingerType() {
        return BeeStingerType.byIndex(data.get(BeeCreatorBlockEntity.STINGER_TYPE_SLOT));
    }

    /** Type d'antennes selectionne (sync via ContainerData slot 10). */
    public BeeAntennaType getAntennaType() {
        return BeeAntennaType.byIndex(data.get(BeeCreatorBlockEntity.ANTENNA_TYPE_SLOT));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.blockPosition().distSqr(blockPos) <= 64 * 64;
    }
}
