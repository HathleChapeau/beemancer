/**
 * ============================================================
 * [EntityCollisionMixin.java]
 * Description: Force le step-up pour le Hoverbike meme quand onGround est faux
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikeEntity     | Entite cible         | Detection du hoverbike monte   |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - apica.mixins.json: Declaration du mixin
 *
 * REFERENCE:
 * - Cobblemon EntityMixin.java L43-63: Meme pattern pour forcer le step-up
 *   sur les montures qui ne sont pas toujours "onGround" selon vanilla.
 *
 * ============================================================
 */
package com.chapeau.apica.mixin;

import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Intercepte Entity.collide() pour forcer onGround=true quand le Hoverbike
 * est monte et a un bloc solide en dessous. Sans ca, le step-up vanilla
 * ne se declenche pas (il requiert onGround=true) et la moto se coince
 * dans les blocs au lieu de les enjamber.
 */
@Mixin(Entity.class)
public abstract class EntityCollisionMixin {

    @WrapOperation(
            method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;onGround()Z"
            )
    )
    private boolean apica$forceOnGroundForStepUp(Entity entity, Operation<Boolean> original) {
        if (entity instanceof HoverbikeEntity && entity.hasControllingPassenger()) {
            BlockPos below = entity.blockPosition().below();
            Level level = entity.level();
            BlockState blockStateBelow = level.getBlockState(below);
            boolean isAirOrLiquid = blockStateBelow.isAir() || !blockStateBelow.getFluidState().isEmpty();
            boolean canSupportEntity = blockStateBelow.isFaceSturdy(level, below, Direction.UP);
            if (canSupportEntity && !isAirOrLiquid) {
                return true;
            }
        }
        return original.call(entity);
    }
}
