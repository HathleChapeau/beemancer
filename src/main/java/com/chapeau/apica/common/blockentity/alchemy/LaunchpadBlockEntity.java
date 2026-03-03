/**
 * ============================================================
 * [LaunchpadBlockEntity.java]
 * Description: BlockEntity pour le Launchpad — projette les entites avec du fluide
 * ============================================================
 *
 * FONCTIONNEMENT:
 * - Accepte honey, royal jelly ou nectar dans son tank
 * - Detecte les entites dans une zone 12x1x12 au-dessus de la plaque
 * - Projette l'entite dans la direction du bloc avec l'angle configure
 * - La puissance augmente selon le type de fluide
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | LaunchpadBlock          | Bloc parent          | FACING, ANGLE properties       |
 * | ApicaFluids             | Fluides du mod       | Validation honey/rj/nectar     |
 * | ApicaBlockEntities      | Registre BE          | Type factory                   |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - LaunchpadBlock (newBlockEntity, ticker)
 *
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.alchemy;

import com.chapeau.apica.common.block.alchemy.LaunchpadBlock;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import com.chapeau.apica.core.registry.ApicaFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;
import java.util.List;

public class LaunchpadBlockEntity extends BlockEntity {
    public static final int TANK_CAPACITY = 4000;
    public static final int FLUID_COST = 250;
    public static final int COOLDOWN_TICKS = 20;

    private static final double HONEY_POWER = 1.5;
    private static final double ROYAL_JELLY_POWER = 2.25;
    private static final double NECTAR_POWER = 3.0;

    private final FluidTank fluidTank;
    private int cooldown;

    public LaunchpadBlockEntity(BlockPos pos, BlockState state) {
        super(ApicaBlockEntities.LAUNCHPAD.get(), pos, state);

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
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LaunchpadBlockEntity be) {
        if (be.cooldown > 0) {
            be.cooldown--;
            return;
        }

        if (be.fluidTank.getFluidAmount() < FLUID_COST) {
            return;
        }

        AABB detectionBox = new AABB(
                pos.getX() + 2.0 / 16.0, pos.getY() + 4.0 / 16.0, pos.getZ() + 2.0 / 16.0,
                pos.getX() + 14.0 / 16.0, pos.getY() + 9.0 / 16.0, pos.getZ() + 14.0 / 16.0
        );

        List<Entity> entities = level.getEntities(null, detectionBox);
        if (entities.isEmpty()) {
            return;
        }

        Entity target = entities.getFirst();
        be.launchEntity(target, state, level, pos);
    }

    private void launchEntity(Entity entity, BlockState state, Level level, BlockPos pos) {
        double power = getPowerMultiplier();
        int angleIndex = state.getValue(LaunchpadBlock.ANGLE);
        double angleRad = Math.toRadians(angleIndex * 10.0);
        Direction facing = state.getValue(LaunchpadBlock.FACING);

        double adjustedPower = power * (1.0 + angleIndex * 0.1);
        double horizontalSpeed = adjustedPower * Math.sin(angleRad);
        double verticalSpeed = adjustedPower * Math.cos(angleRad);

        double motionX = facing.getStepX() * horizontalSpeed;
        double motionZ = facing.getStepZ() * horizontalSpeed;

        entity.setDeltaMovement(motionX, verticalSpeed, motionZ);
        entity.fallDistance = 0;
        entity.hurtMarked = true;

        fluidTank.drain(FLUID_COST, IFluidHandler.FluidAction.EXECUTE);
        cooldown = COOLDOWN_TICKS;

        level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.0f, 1.2f);

        setChanged();
    }

    private double getPowerMultiplier() {
        FluidStack fluid = fluidTank.getFluid();
        if (fluid.isEmpty()) {
            return 0.0;
        }
        if (fluid.getFluid().isSame(ApicaFluids.NECTAR_SOURCE.get())) {
            return NECTAR_POWER;
        }
        if (fluid.getFluid().isSame(ApicaFluids.ROYAL_JELLY_SOURCE.get())) {
            return ROYAL_JELLY_POWER;
        }
        return HONEY_POWER;
    }

    public FluidTank getFluidTank() {
        return fluidTank;
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
        tag.putInt("Cooldown", cooldown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fluidTank.readFromNBT(registries, tag.getCompound("FluidTank"));
        cooldown = tag.getInt("Cooldown");
    }
}
