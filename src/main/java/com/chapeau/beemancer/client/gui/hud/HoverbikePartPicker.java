/**
 * ============================================================
 * [HoverbikePartPicker.java]
 * Description: Raycast pour detecter quelle partie du Hoverbike le curseur vise
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikePart       | Enum des parties     | Identification du resultat     |
 * | HoverbikeEntity     | Entite cible         | Position, yaw, edit mode       |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikeEditModeHandler.java: Detection hover chaque tick
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.hud;

import com.chapeau.beemancer.common.entity.mount.HoverbikeEntity;
import com.chapeau.beemancer.common.entity.mount.HoverbikePart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Detecte quelle partie du Hoverbike le joueur vise avec son curseur.
 * Utilise un raycast custom en espace local de l'entite, verifiant
 * l'intersection avec les hitboxes de chaque partie.
 *
 * Les hitboxes sont en coordonnees monde-relatives (blocs, relatif a
 * la position de l'entite, avant rotation yaw). Elles sont calculees
 * a partir de la geometrie des modeles avec la formule:
 *   worldRelX = -modelX/16
 *   worldRelY = 1.501 - modelY/16
 *   worldRelZ = modelZ/16
 */
@OnlyIn(Dist.CLIENT)
public class HoverbikePartPicker {

    private static final double MAX_RANGE = 8.0;

    /** Hitboxes par partie, en espace entite-local (blocs). */
    private static final Map<HoverbikePart, List<AABB>> HITBOXES = new EnumMap<>(HoverbikePart.class);

    /** Edit offsets par partie (dupliques depuis les modeles pour eviter la dependance). */
    private static final Map<HoverbikePart, Vec3> EDIT_OFFSETS = new EnumMap<>(HoverbikePart.class);

    static {
        // Chassis : rails lateraux + plaque inferieure (AABB combinee)
        HITBOXES.put(HoverbikePart.CHASSIS, List.of(
                new AABB(-0.65, 0.60, -1.05, 0.65, 1.10, 1.05)
        ));

        // Coeur : cube central 6x6x6 (legrement padde pour faciliter le clic)
        HITBOXES.put(HoverbikePart.COEUR, List.of(
                new AABB(-0.25, -0.50, -0.25, 0.25, 0.0, 0.25)
        ));

        // Propulseur : 2 exhausts arriere (AABB combinee)
        HITBOXES.put(HoverbikePart.PROPULSEUR, List.of(
                new AABB(-0.40, 0.48, 0.95, 0.40, 0.80, 1.40)
        ));

        // Radiateur : 2 panneaux lateraux (AABBs separees, paddees en X)
        HITBOXES.put(HoverbikePart.RADIATEUR, List.of(
                new AABB(0.50, 0.20, -0.80, 0.75, 0.80, 0.55),
                new AABB(-0.75, 0.20, -0.80, -0.50, 0.80, 0.55)
        ));

        // Edit offsets (identiques aux valeurs dans les PartModel)
        EDIT_OFFSETS.put(HoverbikePart.CHASSIS, new Vec3(0, 1, 1));
        EDIT_OFFSETS.put(HoverbikePart.COEUR, new Vec3(0, 1, -1));
        EDIT_OFFSETS.put(HoverbikePart.PROPULSEUR, new Vec3(0, 0, 1));
        EDIT_OFFSETS.put(HoverbikePart.RADIATEUR, new Vec3(0, 0, -1));
    }

    /**
     * Detecte quelle partie du hoverbike le joueur local vise.
     * @return la partie visee, ou null si aucune
     */
    public static HoverbikePart pick(HoverbikeEntity entity, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return null;

        Vec3 eyePos = player.getEyePosition(partialTick);
        Vec3 lookDir = player.getViewVector(partialTick);
        Vec3 endPos = eyePos.add(lookDir.scale(MAX_RANGE));

        // Transformer le rayon en espace local de l'entite
        Vec3 entityPos = entity.getPosition(partialTick);
        float bodyYaw = Mth.lerp(partialTick, entity.yBodyRotO, entity.yBodyRot);

        Vec3 localEye = worldToLocal(eyePos.subtract(entityPos), bodyYaw);
        Vec3 localEnd = worldToLocal(endPos.subtract(entityPos), bodyYaw);

        boolean isEdit = entity.isEditMode();

        HoverbikePart closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Map.Entry<HoverbikePart, List<AABB>> entry : HITBOXES.entrySet()) {
            HoverbikePart part = entry.getKey();
            Vec3 editDelta = getEditWorldDelta(part, isEdit);

            for (AABB box : entry.getValue()) {
                AABB shifted = box.move(editDelta);
                Optional<Vec3> hit = shifted.clip(localEye, localEnd);
                if (hit.isPresent()) {
                    double dist = hit.get().distanceToSqr(localEye);
                    if (dist < closestDist) {
                        closestDist = dist;
                        closest = part;
                    }
                }
            }
        }

        return closest;
    }

    /**
     * Transforme un vecteur monde-relatif en espace local de l'entite.
     * Applique l'inverse de la rotation yaw du MobRenderer (180 - bodyYaw).
     */
    private static Vec3 worldToLocal(Vec3 worldRel, float bodyYaw) {
        double yawRad = Math.toRadians(180.0 - bodyYaw);
        double cos = Math.cos(-yawRad);
        double sin = Math.sin(-yawRad);
        return new Vec3(
                worldRel.x * cos - worldRel.z * sin,
                worldRel.y,
                worldRel.x * sin + worldRel.z * cos
        );
    }

    /**
     * Calcule le delta monde-relatif de l'edit offset d'une partie.
     * L'offset PoseStack (dx, dy, dz) se traduit en monde par (-dx, -dy, dz)
     * a cause du scale(-1,-1,1) du MobRenderer.
     */
    private static Vec3 getEditWorldDelta(HoverbikePart part, boolean isEditMode) {
        if (!isEditMode) return Vec3.ZERO;
        Vec3 offset = EDIT_OFFSETS.getOrDefault(part, Vec3.ZERO);
        return new Vec3(-offset.x, -offset.y, offset.z);
    }
}
