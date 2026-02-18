/**
 * ============================================================
 * [PlayerDimensionsMixin.java]
 * Description: Force les dimensions STANDING du joueur quand il monte le Hoverbike
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
 * - Cobblemon PlayerMixin.java L129-135: Force STANDING_DIMENSIONS quand
 *   le joueur est sur un PokemonEntity pour eviter que la hitbox change
 *   de taille (pose accroupie, nage, etc.) et cause des collisions parasites.
 *
 * ============================================================
 */
package com.chapeau.apica.mixin;

import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * Force les dimensions du joueur a STANDING quand il est sur le Hoverbike.
 * Empeche le joueur de passer en pose accroupie/nage avec une hitbox differente
 * qui pourrait causer des problemes de collision (coincement dans les blocs).
 */
@Mixin(Player.class)
public abstract class PlayerDimensionsMixin {

    @Shadow @Final public static EntityDimensions STANDING_DIMENSIONS;
    @Shadow @Final private static Map<Pose, EntityDimensions> POSES;

    @Inject(
            method = "getDefaultDimensions",
            at = @At("HEAD"),
            cancellable = true
    )
    private void apica$forceStandingOnHoverbike(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Player player = (Player) (Object) this;
        if (player.getVehicle() instanceof HoverbikeEntity) {
            cir.setReturnValue(STANDING_DIMENSIONS);
        }
    }
}
