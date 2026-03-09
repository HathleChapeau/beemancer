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
package com.chapeau.apica.client.gui.hud;

import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import com.chapeau.apica.common.entity.mount.HoverbikePart;
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
        // Saddle : selle sur le dos de l'abeille (6x1x5 + backrest 5x1x1)
        HITBOXES.put(HoverbikePart.SADDLE, List.of(
                new AABB(-0.20, 0.20, -0.10, 0.20, 0.35, 0.55)
        ));

        // Wing Protector : protection d'aile (6x1x5 sur les ailes)
        HITBOXES.put(HoverbikePart.WING_PROTECTOR, List.of(
                new AABB(-0.70, 0.20, -0.50, 0.70, 0.35, 0.20)
        ));

        // Control Left : systeme de controle gauche (1x3x3 cote gauche arriere)
        HITBOXES.put(HoverbikePart.CONTROL_LEFT, List.of(
                new AABB(-0.40, 0.05, 0.05, -0.20, 0.25, 0.25)
        ));

        // Control Right : systeme de controle droit (1x3x3 cote droit arriere)
        HITBOXES.put(HoverbikePart.CONTROL_RIGHT, List.of(
                new AABB(0.20, 0.05, 0.05, 0.40, 0.25, 0.25)
        ));

        // Edit offsets (direction d'ecartement en mode edit)
        EDIT_OFFSETS.put(HoverbikePart.SADDLE, new Vec3(0, -1, 0));
        EDIT_OFFSETS.put(HoverbikePart.WING_PROTECTOR, new Vec3(0, -1, 0));
        EDIT_OFFSETS.put(HoverbikePart.CONTROL_LEFT, new Vec3(-1, 0, 0));
        EDIT_OFFSETS.put(HoverbikePart.CONTROL_RIGHT, new Vec3(1, 0, 0));
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
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);
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
