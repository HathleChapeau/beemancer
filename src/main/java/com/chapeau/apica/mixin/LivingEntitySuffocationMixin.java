/**
 * ============================================================
 * [LivingEntitySuffocationMixin.java]
 * Description: Empeche le joueur de suffoquer quand il monte le Hoverbike
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikeEntity     | Entite cible         | Detection du hoverbike         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - apica.mixins.json: Declaration du mixin
 *
 * REFERENCE:
 * - Cobblemon LivingEntityMixin.java L66-72: Meme pattern pour empecher
 *   la suffocation du passager quand la monture traverse un espace etroit.
 *
 * ============================================================
 */
package com.chapeau.apica.mixin;

import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Intercepte LivingEntity.baseTick() -> isInWall() pour empecher
 * le joueur monte sur le Hoverbike de prendre des degats de suffocation.
 * Sans ca, le joueur meurt quand la moto passe sous un plafond bas
 * ou quand la hitbox du joueur chevauche un bloc solide.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntitySuffocationMixin {

    @WrapOperation(
            method = "baseTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;isInWall()Z"
            )
    )
    private boolean apica$preventMountSuffocation(LivingEntity instance, Operation<Boolean> original) {
        if (instance.getVehicle() instanceof HoverbikeEntity) {
            return false;
        }
        return original.call(instance);
    }
}
