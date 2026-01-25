/**
 * ============================================================
 * [CreativeTankBlockEntity.java]
 * Description: Tank creatif qui se remplit automatiquement
 * ============================================================
 *
 * FONCTIONNEMENT:
 * - Une fois qu'un fluide est ajoute, remplit au max chaque tick
 * - Change de fluide quand un nouveau bucket est insere
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.alchemy;

import com.chapeau.beemancer.common.menu.alchemy.CreativeTankMenu;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

public class CreativeTankBlockEntity extends HoneyTankBlockEntity {

    // Le fluide "infini" configure
    private FluidStack infiniteFluid = FluidStack.EMPTY;

    public CreativeTankBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.CREATIVE_TANK.get(), pos, state, TIER1_CAPACITY);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CreativeTankBlockEntity be) {
        // Process bucket slot - peut changer le fluide infini
        be.processCreativeBucket();

        // Si on a un fluide infini, remplir au max
        if (!be.infiniteFluid.isEmpty()) {
            be.fluidTank.setFluid(new FluidStack(be.infiniteFluid.getFluid(), be.getCapacity()));
        }

        // Appeler le tick parent pour le transfert vers le bas
        HoneyTankBlockEntity.serverTick(level, pos, state, be);
    }

    private void processCreativeBucket() {
        ItemStack bucket = bucketSlot.getStackInSlot(0);
        if (bucket.isEmpty()) return;

        // Check if bucket has fluid we can accept
        FluidStack contained = getFluidFromBucket(bucket);
        if (contained.isEmpty() || !fluidTank.isFluidValid(contained)) return;

        // Set the infinite fluid type
        infiniteFluid = new FluidStack(contained.getFluid(), FluidType.BUCKET_VOLUME);

        // Replace bucket with empty bucket
        bucketSlot.setStackInSlot(0, new ItemStack(Items.BUCKET));

        // Fill tank immediately
        fluidTank.setFluid(new FluidStack(contained.getFluid(), getCapacity()));

        setChanged();
    }

    public FluidStack getInfiniteFluid() {
        return infiniteFluid;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.beemancer.creative_tank");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new CreativeTankMenu(containerId, playerInv, this, dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!infiniteFluid.isEmpty()) {
            tag.put("InfiniteFluid", infiniteFluid.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("InfiniteFluid")) {
            infiniteFluid = FluidStack.parse(registries, tag.getCompound("InfiniteFluid")).orElse(FluidStack.EMPTY);
        }
    }
}
