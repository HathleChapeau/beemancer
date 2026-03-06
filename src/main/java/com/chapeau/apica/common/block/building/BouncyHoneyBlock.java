/**
 * ============================================================
 * [BouncyHoneyBlock.java]
 * Description: Variante du honey block vanilla avec rebond configurable
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HalfTransparentBlock| Base semi-transparente| Rendu comme honey/slime block  |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaBlocks.java (royal_jelly_block, nectar_block)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BouncyHoneyBlock extends HalfTransparentBlock {

    private static final VoxelShape SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.9375, 1.0);
    private final float bounceFactor;

    public BouncyHoneyBlock(float bounceFactor, Properties properties) {
        super(properties);
        this.bounceFactor = bounceFactor;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (entity.isSuppressingBounce()) {
            super.fallOn(level, state, pos, entity, fallDistance);
        } else {
            entity.causeFallDamage(fallDistance, 0.0f, level.damageSources().fall());
        }
    }

    @Override
    public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        if (entity.isSuppressingBounce()) {
            super.updateEntityAfterFallOn(level, entity);
        } else {
            bounceUp(entity);
        }
    }

    private void bounceUp(Entity entity) {
        Vec3 velocity = entity.getDeltaMovement();
        if (velocity.y < 0.0) {
            double bounceY = -velocity.y * bounceFactor;
            entity.setDeltaMovement(velocity.x, bounceY, velocity.z);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (Math.abs(entity.getDeltaMovement().y) < 0.1 && !entity.isSteppingCarefully()) {
            double slow = 0.4 + Math.abs(entity.getDeltaMovement().y) * 0.2;
            entity.setDeltaMovement(
                entity.getDeltaMovement().x * slow,
                entity.getDeltaMovement().y,
                entity.getDeltaMovement().z * slow
            );
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (isSlidingDown(pos, entity)) {
            maybeDoSlideEffects(level, entity);
            Vec3 vel = entity.getDeltaMovement();
            if (vel.y < -0.13) {
                double d = -0.05 / vel.y;
                entity.setDeltaMovement(vel.x * d, -0.05, vel.z * d);
            } else {
                entity.setDeltaMovement(vel.x, -0.05, vel.z);
            }
            entity.resetFallDistance();
        }
        super.entityInside(state, level, pos, entity);
    }

    private static boolean isSlidingDown(BlockPos pos, Entity entity) {
        if (entity.onGround()) return false;
        if (entity.getY() > (double) pos.getY() + 0.9375 - 1.0E-7) return false;
        if (entity.getDeltaMovement().y >= -0.08) return false;
        double dx = Math.abs((double) pos.getX() + 0.5 - entity.getX());
        double dz = Math.abs((double) pos.getZ() + 0.5 - entity.getZ());
        double edge = 0.4375 + (double) (entity.getBbWidth() / 2.0f);
        return dx + 1.0E-7 > edge || dz + 1.0E-7 > edge;
    }

    private static void maybeDoSlideEffects(Level level, Entity entity) {
        if (entity instanceof LivingEntity living) {
            if (level.getGameTime() % 10L == 0L) {
                living.playSound(SoundEvents.HONEY_BLOCK_SLIDE, 1.0f, 1.0f);
                showSlideParticles(level, entity);
            }
        }
    }

    private static void showSlideParticles(Level level, Entity entity) {
        for (int i = 0; i < 5; i++) {
            level.addParticle(
                ParticleTypes.LANDING_HONEY,
                entity.getX() + (level.random.nextDouble() - 0.5) * entity.getBbWidth(),
                entity.getY(),
                entity.getZ() + (level.random.nextDouble() - 0.5) * entity.getBbWidth(),
                0.0, 0.0, 0.0
            );
        }
    }
}
