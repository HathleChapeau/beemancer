/**
 * ============================================================
 * [CodexPage.java]
 * Description: Enum des pages/onglets du Codex
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ResourceLocation    | Chemin fichier JSON  | Localisation des données       |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - CodexManager (chargement des pages)
 * - CodexScreen (affichage des onglets)
 * - CodexPlayerData (stockage progression par page)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.codex;

import com.chapeau.beemancer.Beemancer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public enum CodexPage {
    BEES("bees", 0xFFAA00, true),
    BEE("bee", 0xFFD700, false),
    ALCHEMY("alchemy", 0x9932CC, false),
    LOGISTICS("logistics", 0x4169E1, false);

    private final String id;
    private final int color;
    private final ResourceLocation dataPath;
    private final boolean isBeeTree;

    CodexPage(String id, int color, boolean isBeeTree) {
        this.id = id;
        this.color = color;
        this.isBeeTree = isBeeTree;
        this.dataPath = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "codex/" + id + ".json"
        );
    }

    public String getId() {
        return id;
    }

    public int getColor() {
        return color;
    }

    public ResourceLocation getDataPath() {
        return dataPath;
    }

    public boolean isBeeTree() {
        return isBeeTree;
    }

    public Component getDisplayName() {
        return Component.translatable("codex." + Beemancer.MOD_ID + ".page." + id);
    }

    public static CodexPage fromId(String id) {
        for (CodexPage page : values()) {
            if (page.id.equals(id)) {
                return page;
            }
        }
        return BEE;
    }
}
