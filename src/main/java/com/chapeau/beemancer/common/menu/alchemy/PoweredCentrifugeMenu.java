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
 * | BeemancerBlocks                 | Validation block     | stillValid            |
 * | BeemancerMenus                  | Type menu            | Registration          |
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
package com.chapeau.beemancer.common.menu.alchemy;

import com.chapeau.beemancer.client.gui.widget.BeemancerSlot;
import com.chapeau.beemancer.common.blockentity.alchemy.CentrifugeHeartBlockEntity;
import com.chapeau.beemancer.common.blockentity.alchemy.PoweredCentrifugeBlockEntity;
import com.chapeau.beemancer.common.menu.BeemancerMenu;
import com.chapeau.beemancer.common.quest.QuestEvents;
import com.chapeau.beemancer.core.registry.BeemancerBlocks;
import com.chapeau.beemancer.core.registry.BeemancerMenus;
import com.chapeau.beemancer.core.registry.BeemancerTags;
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

public class PoweredCentrifugeMenu extends BeemancerMenu {
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
        super(BeemancerMenus.POWERED_CENTRIFUGE.get(), containerId);
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
        addSlot(BeemancerSlot.combInput(inputHandler, 0, 40, 45)
            .withFilter(stack -> stack.is(BeemancerTags.Items.COMBS)));

        // Output slots (droite, 2x2) avec callback pour les quêtes
        addSlot(BeemancerSlot.output(outputHandler, 0, 116, 36)
                .withOnExtract((p, s) -> QuestEvents.onMachineExtract(p, "powered_centrifuge", s)));
        addSlot(BeemancerSlot.output(outputHandler, 1, 134, 36)
                .withOnExtract((p, s) -> QuestEvents.onMachineExtract(p, "powered_centrifuge", s)));
        addSlot(BeemancerSlot.output(outputHandler, 2, 116, 54)
                .withOnExtract((p, s) -> QuestEvents.onMachineExtract(p, "powered_centrifuge", s)));
        addSlot(BeemancerSlot.output(outputHandler, 3, 134, 54)
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
    public ItemStack quickMoveStack(Player player, int index) {
        return doQuickMove(index, PLAYER_INV_START, INPUT_SLOT, INPUT_SLOT + 1,
                           this::isValidComb);
    }

    private boolean isValidComb(ItemStack stack) {
        return stack.is(BeemancerTags.Items.COMBS);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, BeemancerBlocks.POWERED_CENTRIFUGE.get())
            || stillValid(access, player, BeemancerBlocks.CENTRIFUGE_HEART.get());
    }
}
