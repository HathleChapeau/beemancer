/**
 * ============================================================
 * [StorageTab.java]
 * Description: Enum des onglets du Storage Terminal
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance     | Raison                | Utilisation       |
 * |----------------|----------------------|-------------------|
 * | (aucune)       |                      |                   |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - StorageTerminalScreen.java (sélection d'onglet)
 * - StorageTerminalMenu.java (visibilité des slots)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.storage;

/**
 * Onglets disponibles dans le Storage Terminal.
 * Chaque onglet contrôle la visibilité des slots et le contenu affiché.
 */
public enum StorageTab {
    STORAGE("gui.beemancer.tab.storage"),
    TASKS("gui.beemancer.tab.tasks"),
    CONTROLLER("gui.beemancer.tab.controller");

    private final String translationKey;

    StorageTab(String translationKey) {
        this.translationKey = translationKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }
}
