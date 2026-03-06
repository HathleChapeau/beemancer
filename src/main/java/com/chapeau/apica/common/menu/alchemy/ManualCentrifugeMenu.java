/**
 * ============================================================
 * [ManualCentrifugeMenu.java]
 * Description: Menu pour la centrifugeuse manuelle
 * ============================================================
 *
 * SLOTS:
 * - 1 slot entree (index 0)
 * - 4 slots sortie (index 1-4)
 * - 27 slots inventaire (index 5-31)
 * - 9 slots hotbar (index 32-40)
 * ============================================================
 */
package com.chapeau.apica.common.menu.alchemy;

import com.chapeau.apica.client.gui.widget.ApicaSlot;
import com.chapeau.apica.common.blockentity.alchemy.ManualCentrifugeBlockEntity;
import com.chapeau.apica.common.menu.ApicaMenu;
import com.chapeau.apica.common.quest.QuestEvents;
import com.chapeau.apica.core.registry.ApicaBlocks;
import com.chapeau.apica.core.registry.ApicaMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ManualCentrifugeMenu extends ApicaMenu {
    private final ManualCentrifugeBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    // Slot indices
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_START = 1;
    private static final int OUTPUT_END = 5;
    private static final int PLAYER_INV_START = 5;
    private static final int PLAYER_INV_END = 32;
    private static final int HOTBAR_START = 32;
    private static final int HOTBAR_END = 41;

    public ManualCentrifugeMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(containerId, playerInv, playerInv.player.level().getBlockEntity(buf.readBlockPos()),
             new SimpleContainerData(3));
    }

    public ManualCentrifugeMenu(int containerId, Inventory playerInv, BlockEntity be, ContainerData data) {
        super(ApicaMenus.MANUAL_CENTRIFUGE.get(), containerId);
        this.blockEntity = (ManualCentrifugeBlockEntity) be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.data = data;

        addDataSlots(data);

        // Input slot (gauche) - filtre par recette de centrifugation
        addSlot(ApicaSlot.combInput(blockEntity.getInputSlot(), 0, 14, 45)
            .withFilter(stack -> blockEntity.isValidComb(stack)));

        // Output slots (droite, 2x2) avec callback pour les quêtes
        addSlot(ApicaSlot.output(blockEntity.getOutputSlots(), 0, 112, 36)
                .withOnExtract((p, s) -> QuestEvents.onMachineExtract(p, "manual_centrifuge", s)));
        addSlot(ApicaSlot.output(blockEntity.getOutputSlots(), 1, 130, 36)
                .withOnExtract((p, s) -> QuestEvents.onMachineExtract(p, "manual_centrifuge", s)));
        addSlot(ApicaSlot.output(blockEntity.getOutputSlots(), 2, 112, 54)
                .withOnExtract((p, s) -> QuestEvents.onMachineExtract(p, "manual_centrifuge", s)));
        addSlot(ApicaSlot.output(blockEntity.getOutputSlots(), 3, 130, 54)
                .withOnExtract((p, s) -> QuestEvents.onMachineExtract(p, "manual_centrifuge", s)));

        // Player inventory (centered in 190px container)
        addPlayerInventory(playerInv, 15, 107);
        addPlayerHotbar(playerInv, 15, 165);
    }

    public ManualCentrifugeBlockEntity getBlockEntity() { return blockEntity; }
    public int getProgress() { return data.get(0); }
    public int getFluidAmount() { return data.get(1); }
    public int getMaxProgress() { return data.get(2); }

    /**
     * Alias pour compatibilite avec ManualCentrifugeScreen
     */
    public int getSpinCount() { return data.get(0); }

    public float getProgressRatio() {
        int max = getMaxProgress();
        return max > 0 ? (float) getProgress() / max : 0;
    }

    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType clickType, Player player) {
        ItemStack before = slotId == INPUT_SLOT ? slots.get(INPUT_SLOT).getItem().copy() : ItemStack.EMPTY;
        super.clicked(slotId, button, clickType, player);
        if (slotId == INPUT_SLOT && before.isEmpty()) {
            ItemStack after = slots.get(INPUT_SLOT).getItem();
            if (!after.isEmpty()) {
                QuestEvents.onMachineInsert(player, "manual_centrifuge", after);
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack before = slots.get(INPUT_SLOT).getItem().copy();
        ItemStack result = doQuickMove(index, PLAYER_INV_START, INPUT_SLOT, INPUT_SLOT + 1,
                           stack -> blockEntity.isValidComb(stack));
        if (before.isEmpty() && !slots.get(INPUT_SLOT).getItem().isEmpty()) {
            QuestEvents.onMachineInsert(player, "manual_centrifuge", slots.get(INPUT_SLOT).getItem());
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ApicaBlocks.MANUAL_CENTRIFUGE.get());
    }
}
