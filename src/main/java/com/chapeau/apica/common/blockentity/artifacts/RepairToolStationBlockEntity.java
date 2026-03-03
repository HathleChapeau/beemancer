/**
 * ============================================================
 * [RepairToolStationBlockEntity.java]
 * Description: BlockEntity pour la Repair Tool Station — repare les outils avec du fluide
 * ============================================================
 *
 * FONCTIONNEMENT:
 * - Accepte honey, royal jelly ou nectar dans son tank
 * - Stocke un outil endommage dans un slot unique
 * - Repare l'outil progressivement (rate selon fluide)
 * - Consomme 50mb par point de durabilite restaure
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | RepairToolStationBlock  | Bloc parent          | REPAIRING property             |
 * | ApicaFluids             | Fluides du mod       | Validation honey/rj/nectar     |
 * | ApicaBlockEntities      | Registre BE          | Type factory                   |
 * | ParticleHelper          | Particules           | orbitingRing pendant reparation |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - RepairToolStationBlock (newBlockEntity, ticker)
 *
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.artifacts;

import com.chapeau.apica.common.block.artifacts.RepairToolStationBlock;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import com.chapeau.apica.core.registry.ApicaFluids;
import com.chapeau.apica.core.util.ParticleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public class RepairToolStationBlockEntity extends BlockEntity {
    public static final int TANK_CAPACITY = 4000;
    public static final int FLUID_COST = 50;
    private static final int REPAIR_INTERVAL = 20;

    private static final int HONEY_RATE = 2;
    private static final int ROYAL_JELLY_RATE = 10;
    private static final int NECTAR_RATE = 30;

    private final FluidTank fluidTank;
    private final ItemStackHandler itemHandler;
    private int repairCooldown;

    public RepairToolStationBlockEntity(BlockPos pos, BlockState state) {
        super(ApicaBlockEntities.REPAIR_TOOL_STATION.get(), pos, state);

        this.fluidTank = new FluidTank(TANK_CAPACITY) {
            @Override
            public boolean isFluidValid(FluidStack stack) {
                return stack.getFluid().isSame(ApicaFluids.HONEY_SOURCE.get())
                        || stack.getFluid().isSame(ApicaFluids.ROYAL_JELLY_SOURCE.get())
                        || stack.getFluid().isSame(ApicaFluids.NECTAR_SOURCE.get());
            }

            @Override
            protected void onContentsChanged() {
                setChanged();
                syncToClient();
            }
        };

        this.itemHandler = new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.isDamageableItem();
            }

            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                syncToClient();
            }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RepairToolStationBlockEntity be) {
        if (be.repairCooldown > 0) {
            be.repairCooldown--;
            return;
        }

        ItemStack tool = be.itemHandler.getStackInSlot(0);
        boolean shouldRepair = !tool.isEmpty() && tool.isDamaged() && be.fluidTank.getFluidAmount() > 0;

        if (!shouldRepair) {
            if (state.getValue(RepairToolStationBlock.REPAIRING)) {
                level.setBlock(pos, state.setValue(RepairToolStationBlock.REPAIRING, false), 3);
            }
            return;
        }

        int rate = be.getRepairRate();
        if (rate <= 0) return;

        int damage = tool.getDamageValue();
        int pointsToRepair = Math.min(rate, damage);
        int fluidCost = pointsToRepair * FLUID_COST;

        if (be.fluidTank.getFluidAmount() < fluidCost) {
            pointsToRepair = be.fluidTank.getFluidAmount() / FLUID_COST;
            if (pointsToRepair <= 0) {
                if (state.getValue(RepairToolStationBlock.REPAIRING)) {
                    level.setBlock(pos, state.setValue(RepairToolStationBlock.REPAIRING, false), 3);
                }
                return;
            }
            fluidCost = pointsToRepair * FLUID_COST;
        }

        be.fluidTank.drain(fluidCost, IFluidHandler.FluidAction.EXECUTE);
        tool.setDamageValue(damage - pointsToRepair);
        be.repairCooldown = REPAIR_INTERVAL;

        if (!state.getValue(RepairToolStationBlock.REPAIRING)) {
            level.setBlock(pos, state.setValue(RepairToolStationBlock.REPAIRING, true), 3);
        }

        if (level instanceof ServerLevel serverLevel) {
            Vec3 center = Vec3.atBottomCenterOf(pos).add(0, 6.0 / 16.0, 0);
            ParticleHelper.orbitingRing(serverLevel, ParticleTypes.WAX_ON, center, 0.4, 8, 0.15);
        }

        be.setChanged();
        be.syncToClient();
    }

    private int getRepairRate() {
        FluidStack fluid = fluidTank.getFluid();
        if (fluid.isEmpty()) return 0;
        if (fluid.getFluid().isSame(ApicaFluids.NECTAR_SOURCE.get())) return NECTAR_RATE;
        if (fluid.getFluid().isSame(ApicaFluids.ROYAL_JELLY_SOURCE.get())) return ROYAL_JELLY_RATE;
        return HONEY_RATE;
    }

    public boolean placeItem(ItemStack stack) {
        if (!itemHandler.getStackInSlot(0).isEmpty() || stack.isEmpty()) return false;
        itemHandler.setStackInSlot(0, stack.copyWithCount(1));
        setChanged();
        syncToClient();
        return true;
    }

    public ItemStack removeItem() {
        ItemStack stored = itemHandler.getStackInSlot(0);
        if (stored.isEmpty()) return ItemStack.EMPTY;
        ItemStack removed = stored.copy();
        itemHandler.setStackInSlot(0, ItemStack.EMPTY);
        setChanged();
        syncToClient();
        return removed;
    }

    public boolean isEmpty() {
        return itemHandler.getStackInSlot(0).isEmpty();
    }

    public ItemStack getStoredItem() {
        return itemHandler.getStackInSlot(0);
    }

    public FluidTank getFluidTank() {
        return fluidTank;
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    // ==================== Sync ====================

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("FluidTank", fluidTank.writeToNBT(registries, new CompoundTag()));
        tag.put("ItemHandler", itemHandler.serializeNBT(registries));
        tag.putInt("RepairCooldown", repairCooldown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fluidTank.readFromNBT(registries, tag.getCompound("FluidTank"));
        itemHandler.deserializeNBT(registries, tag.getCompound("ItemHandler"));
        repairCooldown = tag.getInt("RepairCooldown");
    }
}
