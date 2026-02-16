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
package com.chapeau.apica.client.gui.screen.storage;

/**
 * Onglets disponibles dans le Storage Terminal.
 * Chaque onglet contrôle la visibilité des slots et le contenu affiché.
 */
public enum StorageTab {
    STORAGE("gui.apica.tab.storage"),
    TASKS("gui.apica.tab.tasks"),
    CONTROLLER("gui.apica.tab.controller");

    private final String translationKey;

    StorageTab(String translationKey) {
        this.translationKey = translationKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }
}
