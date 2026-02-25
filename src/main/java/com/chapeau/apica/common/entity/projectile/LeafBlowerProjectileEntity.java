/**
 * ============================================================
 * [LeafBlowerProjectileEntity.java]
 * Description: Projectile orbe du Leaf Blower — vole puis pulse pour détruire la végétation
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ParticleHelper      | Effets visuels       | Sphère de particules par pulse |
 * | ApicaEntities       | Type d'entité        | Registration                   |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - LeafBlowerItem.java (spawn au relâchement)
 * - LeafBlowerProjectileRenderer.java (rendu client)
 *
 * ============================================================
 */
package com.chapeau.apica.common.entity.projectile;

import com.chapeau.apica.core.registry.ApicaEntities;
import com.chapeau.apica.core.util.ParticleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class LeafBlowerProjectileEntity extends ThrowableProjectile {

    private static final EntityDataAccessor<Integer> DATA_CHARGE_LEVEL =
            SynchedEntityData.defineId(LeafBlowerProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PULSING =
            SynchedEntityData.defineId(LeafBlowerProjectileEntity.class, EntityDataSerializers.BOOLEAN);

    /** Radii for each successive pulse: index 0 = first pulse, etc. */
    private static final int[] PULSE_RADII = {3, 5, 7};

    /** Ticks between each pulse */
    private static final int PULSE_INTERVAL = 20;

    private int pulseTimer = 0;
    private int currentPulse = 0;

    public LeafBlowerProjectileEntity(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
    }

    public LeafBlowerProjectileEntity(Level level, double x, double y, double z, int chargeLevel) {
        super(ApicaEntities.LEAF_BLOWER_ORB.get(), x, y, z, level);
        entityData.set(DATA_CHARGE_LEVEL, chargeLevel);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_CHARGE_LEVEL, 1);
        builder.define(DATA_PULSING, false);
    }

    public int getChargeLevel() {
        return entityData.get(DATA_CHARGE_LEVEL);
    }

    public boolean isPulsing() {
        return entityData.get(DATA_PULSING);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide()) {
            setDeltaMovement(Vec3.ZERO);
            entityData.set(DATA_PULSING, true);
            pulseTimer = 0;
            currentPulse = 0;
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) return;

        if (!isPulsing()) {
            // Discard if flying too long (3 seconds max, but not if already pulsing)
            if (tickCount > 60) {
                discard();
            }
            return;
        }

        pulseTimer++;
        if (pulseTimer >= PULSE_INTERVAL) {
            pulseTimer = 0;
            doPulse();
        }
    }

    private void doPulse() {
        if (!(level() instanceof ServerLevel serverLevel)) return;

        int maxPulses = getChargeLevel();
        if (currentPulse >= maxPulses || currentPulse >= PULSE_RADII.length) {
            discard();
            return;
        }

        int radius = PULSE_RADII[currentPulse];
        destroyFoliageInRadius(serverLevel, radius);

        Vec3 center = position();
        ParticleHelper.sphere(serverLevel, center, radius, ParticleTypes.CLOUD, radius * 8);
        ParticleHelper.spawnRingBurst(serverLevel, ParticleTypes.CHERRY_LEAVES, center, 24, 0.3);

        currentPulse++;
        if (currentPulse >= maxPulses) {
            discard();
        }
    }

    private void destroyFoliageInRadius(ServerLevel serverLevel, int radius) {
        BlockPos center = blockPosition();
        int rSq = radius * radius;

        // First pass: destroy foliage (no drops)
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > rSq) continue;
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = serverLevel.getBlockState(pos);
                    if (isFoliageBlock(state)) {
                        serverLevel.destroyBlock(pos, false);
                    }
                }
            }
        }

        // Second pass: destroy isolated logs (only touching air/leaves/logs)
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > rSq) continue;
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = serverLevel.getBlockState(pos);
                    if (state.is(BlockTags.LOGS) && isIsolatedLog(serverLevel, pos)) {
                        serverLevel.destroyBlock(pos, false);
                    }
                }
            }
        }
    }

    /** A log is "isolated" if all 6 neighbors are air, leaves, or other logs. */
    private boolean isIsolatedLog(ServerLevel serverLevel, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockState neighbor = serverLevel.getBlockState(pos.relative(dir));
            if (neighbor.isAir()) continue;
            if (neighbor.is(BlockTags.LEAVES)) continue;
            if (neighbor.is(BlockTags.LOGS)) continue;
            return false;
        }
        return true;
    }

    private boolean isFoliageBlock(BlockState state) {
        if (state.isAir()) return false;
        if (state.is(BlockTags.LEAVES)) return true;
        if (state.is(BlockTags.FLOWERS)) return true;
        if (state.is(BlockTags.REPLACEABLE_BY_TREES)) return true;
        return state.getBlock() instanceof VineBlock;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ChargeLevel", getChargeLevel());
        tag.putBoolean("Pulsing", isPulsing());
        tag.putInt("PulseTimer", pulseTimer);
        tag.putInt("CurrentPulse", currentPulse);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_CHARGE_LEVEL, tag.getInt("ChargeLevel"));
        entityData.set(DATA_PULSING, tag.getBoolean("Pulsing"));
        pulseTimer = tag.getInt("PulseTimer");
        currentPulse = tag.getInt("CurrentPulse");
    }
}
