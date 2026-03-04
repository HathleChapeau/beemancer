/**
 * ============================================================
 * [PlayMode.java]
 * Description: Modes de lecture du sequenceur — play, page play, loop, page loop
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | (aucune)            |                      |                                |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - SequencePlaybackEngine.java (mode de lecture actif)
 * - TransportBarWidget.java (affichage/toggle du mode)
 * - DubstepRadioScreen.java (stockage du mode courant)
 *
 * ============================================================
 */
package com.chapeau.apica.client.audio;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 4 modes de lecture pour le sequenceur.
 * PLAY/LOOP operent sur toute la sequence, PAGE variants sur la page courante uniquement.
 */
@OnlyIn(Dist.CLIENT)
public enum PlayMode {
    PLAY("Play"),
    PLAY_PAGE("Pg Play"),
    LOOP("Loop"),
    PAGE_LOOP("Pg Loop");

    private final String label;

    PlayMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public PlayMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
