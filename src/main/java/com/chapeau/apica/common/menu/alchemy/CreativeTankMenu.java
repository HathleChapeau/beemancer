/**
 * ============================================================
 * [CreativeTankMenu.java]
 * Description: Menu pour le tank creatif
 * ============================================================
 */
package com.chapeau.apica.common.menu.alchemy;

import com.chapeau.apica.common.blockentity.alchemy.CreativeTankBlockEntity;
import com.chapeau.apica.core.registry.ApicaBlocks;
import com.chapeau.apica.core.registry.ApicaMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

public class CreativeTankMenu extends HoneyTankMenu {

    public CreativeTankMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(containerId, playerInv, playerInv.player.level().getBlockEntity(buf.readBlockPos()),
             new SimpleContainerData(1));
    }

    public CreativeTankMenu(int containerId, Inventory playerInv, BlockEntity be, ContainerData data) {
        super(ApicaMenus.CREATIVE_TANK.get(), containerId, playerInv, be, data);
    }

    @Override
    protected Block getValidBlock() {
        return ApicaBlocks.CREATIVE_TANK.get();
    }
}
