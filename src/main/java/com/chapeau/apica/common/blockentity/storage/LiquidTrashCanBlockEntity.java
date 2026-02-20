/**
 * ============================================================
 * [LiquidTrashCanBlockEntity.java]
 * Description: BlockEntity pour la poubelle a liquides
 * ============================================================
 *
 * FONCTIONNEMENT:
 * - Accepte tout fluide et le detruit immediatement
 * - GUI avec 1 slot pour bucket: vide le bucket, garde le sceau
 * - Expose IFluidHandler pour automation (pipes)
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.storage;

import com.chapeau.apica.common.menu.LiquidTrashCanMenu;
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
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class LiquidTrashCanBlockEntity extends BlockEntity implements MenuProvider {

    private final ItemStackHandler bucketSlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            processBucket();
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return stack.getItem() instanceof BucketItem;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    public LiquidTrashCanBlockEntity(BlockPos pos, BlockState state) {
        super(ApicaBlockEntities.LIQUID_TRASH_CAN.get(), pos, state);
    }

    public ItemStackHandler getBucketSlot() {
        return bucketSlot;
    }

    /**
     * Si un bucket plein est place, le vider et garder le bucket vide.
     */
    private void processBucket() {
        ItemStack stack = bucketSlot.getStackInSlot(0);
        if (stack.isEmpty()) return;

        if (stack.getItem() instanceof BucketItem bucket) {
            // Si ce n'est pas un bucket vide, le remplacer par un bucket vide
            if (!stack.is(Items.BUCKET)) {
                bucketSlot.setStackInSlot(0, new ItemStack(Items.BUCKET));
            }
        }
    }

    /**
     * Handler pour automation: accepte tout fluide, void immediat.
     */
    private final IFluidHandler automationFluidHandler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Nonnull
        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
            return true;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return 0;
            return resource.getAmount(); // Tout accepte, void immediat
        }

        @Nonnull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Nonnull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    };

    public IFluidHandler getFluidHandler() {
        return automationFluidHandler;
    }

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.apica.liquid_trash_can");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new LiquidTrashCanMenu(containerId, playerInventory, this);
    }

    // --- Sync ---

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

    // --- NBT ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("BucketSlot", bucketSlot.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("BucketSlot")) {
            bucketSlot.deserializeNBT(registries, tag.getCompound("BucketSlot"));
        }
    }
}
