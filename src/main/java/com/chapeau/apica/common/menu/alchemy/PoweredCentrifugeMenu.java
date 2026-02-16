/**
 * ============================================================
 * [PoweredCentrifugeMenu.java]
 * Description: Menu pour la centrifugeuse automatique (standalone et multibloc)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                      | Raison                | Utilisation           |
 * |---------------------------------|----------------------|-----------------------|
 * | PoweredCentrifugeBlockEntity    | Standalone centrifuge| Slots accesseurs      |
 * | CentrifugeHeartBlockEntity      | Multibloc centrifuge | Slots accesseurs      |
 * | ApicaBlocks                 | Validation block     | stillValid            |
 * | ApicaMenus                  | Type menu            | Registration          |
 * ------------------------------------------------------------
 *
 * SLOTS:
 * - 1 slot entree (index 0)
 * - 4 slots sortie (index 1-4)
 * - 27 slots inventaire (index 5-31)
 * - 9 slots hotbar (index 32-40)
 *
 * UTILISE PAR:
 * - PoweredCentrifugeBlockEntity.java (createMenu)
 * - CentrifugeHeartBlockEntity.java (createMenu)
 * - PoweredCentrifugeScreen.java (client GUI)
 *
 * ============================================================
 */
package com.chapeau.apica.common.menu.alchemy;

import com.chapeau.apica.client.gui.widget.ApicaSlot;
import com.chapeau.apica.common.blockentity.alchemy.CentrifugeHeartBlockEntity;
import com.chapeau.apica.common.blockentity.alchemy.PoweredCentrifugeBlockEntity;
import com.chapeau.apica.common.menu.ApicaMenu;
import com.chapeau.apica.common.quest.QuestEvents;
import com.chapeau.apica.core.registry.ApicaBlocks;
import com.chapeau.apica.core.registry.ApicaMenus;
import com.chapeau.apica.core.registry.ApicaTags;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

public class PoweredCentrifugeMenu extends ApicaMenu {
    private final BlockEntity blockEntity;
    private final FluidTank fuelTank;
    private final FluidTank outputTank;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    // Slot indices
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_START = 1;
    private static final int OUTPUT_END = 5;
    private static final int PLAYER_INV_START = 5;
    private static final int PLAYER_INV_END = 32;
    private static final int HOTBAR_START = 32;
    private static final int HOTBAR_END = 41;

    // Client constructor
    public PoweredCentrifugeMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(containerId, playerInv, playerInv.player.level().getBlockEntity(buf.readBlockPos()),
             new SimpleContainerData(6));
    }

    // Server constructor
    public PoweredCentrifugeMenu(int containerId, Inventory playerInv, BlockEntity be, ContainerData data) {
        super(ApicaMenus.POWERED_CENTRIFUGE.get(), containerId);
        this.blockEntity = be;
        this.data = data;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        ItemStackHandler inputHandler;
        ItemStackHandler outputHandler;
        if (be instanceof CentrifugeHeartBlockEntity heart) {
            inputHandler = heart.getInputSlot();
            outputHandler = heart.getOutputSlots();
            this.fuelTank = heart.getFuelTank();
            this.outputTank = heart.getOutputTank();
        } else {
            PoweredCentrifugeBlockEntity centrifuge = (PoweredCentrifugeBlockEntity) be;
            inputHandler = centrifuge.getInputSlot();
            outputHandler = centrifuge.getOutputSlots();
            this.fuelTank = centrifuge.getFuelTank();
            this.outputTank = centrifuge.getOutputTank();
        }

        addDataSlots(data);

        // Input slot (gauche)
        addSlot(ApicaSlot.combInput(inputHandler, 0, 40, 45)
            .withFilter(stack -> stack.is(ApicaTags.Items.COMBS)));

        // Output slots (droite, 2x2) avec callback pour les quêtes
        addSlot(ApicaSlot.output(outputHandler, 0, 116, 36)
                .withOnExtract((p, s) -> QuestEvents.onMachineExtract(p, "powered_centrifuge", s)));
        addSlot(ApicaSlot.output(outputHandler, 1, 134, 36)
                .withOnExtract((p, s) -> QuestEvents.onMachineExtract(p, "powered_centrifuge", s)));
        addSlot(ApicaSlot.output(outputHandler, 2, 116, 54)
                .withOnExtract((p, s) -> QuestEvents.onMachineExtract(p, "powered_centrifuge", s)));
        addSlot(ApicaSlot.output(outputHandler, 3, 134, 54)
                .withOnExtract((p, s) -> QuestEvents.onMachineExtract(p, "powered_centrifuge", s)));

        // Player inventory (centered in 190px container)
        addPlayerInventory(playerInv, 15, 107);
        addPlayerHotbar(playerInv, 15, 165);
    }

    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return data.get(1); }
    public int getFuelAmount() { return data.get(2); }
    public int getOutputAmount() { return data.get(3); }
    public int getFuelCapacity() { int c = data.get(4); return c > 0 ? c : 8000; }
    public int getOutputCapacity() { int c = data.get(5); return c > 0 ? c : 8000; }

    public float getProgressRatio() {
        int max = getMaxProgress();
        return max > 0 ? (float) getProgress() / max : 0;
    }

    public BlockEntity getBlockEntity() { return blockEntity; }
    public FluidTank getFuelTank() { return fuelTank; }
    public FluidTank getOutputTank() { return outputTank; }

    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType clickType, Player player) {
        ItemStack before = slotId == INPUT_SLOT ? slots.get(INPUT_SLOT).getItem().copy() : ItemStack.EMPTY;
        super.clicked(slotId, button, clickType, player);
        if (slotId == INPUT_SLOT && before.isEmpty()) {
            ItemStack after = slots.get(INPUT_SLOT).getItem();
            if (!after.isEmpty()) {
                QuestEvents.onMachineInsert(player, "powered_centrifuge", after);
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack before = slots.get(INPUT_SLOT).getItem().copy();
        ItemStack result = doQuickMove(index, PLAYER_INV_START, INPUT_SLOT, INPUT_SLOT + 1,
                           this::isValidComb);
        if (before.isEmpty() && !slots.get(INPUT_SLOT).getItem().isEmpty()) {
            QuestEvents.onMachineInsert(player, "powered_centrifuge", slots.get(INPUT_SLOT).getItem());
        }
        return result;
    }

    private boolean isValidComb(ItemStack stack) {
        return stack.is(ApicaTags.Items.COMBS);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ApicaBlocks.POWERED_CENTRIFUGE.get())
            || stillValid(access, player, ApicaBlocks.CENTRIFUGE_HEART.get());
    }
}
