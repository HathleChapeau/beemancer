/**
 * ============================================================
 * [CombItem.java]
 * Description: Item de rayon de miel avec tinting body/stripe
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BeeSpeciesManager   | Couleurs espèce      | Tinting body/stripe            |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ApicaItems (enregistrement de toutes les combs espèce)
 * - ClientSetup (tinting via ItemColor)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item;

import com.chapeau.apica.core.bee.BeeSpeciesManager;
import net.minecraft.world.item.Item;

/**
 * Item rayon de miel avec 2 layers tintées (body + stripe).
 * Peut être configuré soit via un speciesId (résolution dynamique),
 * soit via 2 couleurs fixes (bodyColor, stripeColor).
 * Le flag inverted inverse les 2 couleurs.
 */
public class CombItem extends Item {

    private final String speciesId;
    private final int bodyColor;
    private final int stripeColor;
    private final boolean inverted;

    /**
     * Constructeur par espèce (couleurs résolues dynamiquement via BeeSpeciesManager).
     */
    public CombItem(Properties properties, String speciesId) {
        this(properties, speciesId, false);
    }

    /**
     * Constructeur par espèce avec inversion.
     */
    public CombItem(Properties properties, String speciesId, boolean inverted) {
        super(properties);
        this.speciesId = speciesId;
        this.bodyColor = -1;
        this.stripeColor = -1;
        this.inverted = inverted;
    }

    /**
     * Constructeur par couleurs fixes.
     */
    public CombItem(Properties properties, int bodyColor, int stripeColor) {
        this(properties, bodyColor, stripeColor, false);
    }

    /**
     * Constructeur par couleurs fixes avec inversion.
     */
    public CombItem(Properties properties, int bodyColor, int stripeColor, boolean inverted) {
        super(properties);
        this.speciesId = null;
        this.bodyColor = bodyColor;
        this.stripeColor = stripeColor;
        this.inverted = inverted;
    }

    /**
     * Retourne la couleur body (layer0), ou stripe si inverted.
     */
    public int getBodyColor() {
        int body;
        int stripe;
        if (speciesId != null) {
            BeeSpeciesManager.ensureClientLoaded();
            BeeSpeciesManager.BeeSpeciesData data = BeeSpeciesManager.getSpecies(speciesId);
            if (data == null) return 0xFFFFFF;
            body = data.partColorBody;
            stripe = data.partColorStripe;
        } else {
            body = bodyColor;
            stripe = stripeColor;
        }
        return inverted ? stripe : body;
    }

    /**
     * Retourne la couleur stripe (layer1), ou body si inverted.
     */
    public int getStripeColor() {
        int body;
        int stripe;
        if (speciesId != null) {
            BeeSpeciesManager.ensureClientLoaded();
            BeeSpeciesManager.BeeSpeciesData data = BeeSpeciesManager.getSpecies(speciesId);
            if (data == null) return 0xFFFFFF;
            body = data.partColorBody;
            stripe = data.partColorStripe;
        } else {
            body = bodyColor;
            stripe = stripeColor;
        }
        return inverted ? body : stripe;
    }
}
