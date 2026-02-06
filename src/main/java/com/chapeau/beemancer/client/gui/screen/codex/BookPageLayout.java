/**
 * ============================================================
 * [BookPageLayout.java]
 * Description: Calcul de la pagination des sections sur les pages du Codex Book
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexBookSection    | Sections à paginer   | Calcul des hauteurs            |
 * | Font                | Mesure du texte      | Hauteur dynamique des sections |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - CodexBookScreen (pagination du contenu)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.codex;

import com.chapeau.beemancer.common.codex.book.CodexBookSection;
import net.minecraft.client.gui.Font;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BookPageLayout {

    /**
     * Répartit les sections sur des pages individuelles selon la hauteur disponible.
     * Les sections sont empilées verticalement jusqu'à dépasser la hauteur de page,
     * puis débordent sur la page suivante.
     *
     * @param sections Les sections à paginer
     * @param font Le font pour calculer les hauteurs
     * @param pageWidth Largeur d'une page
     * @param pageHeight Hauteur disponible sur une page
     * @return Liste de pages, chaque page contenant ses sections
     */
    public static List<List<CodexBookSection>> paginate(List<CodexBookSection> sections,
                                                         Font font, int pageWidth, int pageHeight) {
        if (sections.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<CodexBookSection>> pages = new ArrayList<>();
        List<CodexBookSection> currentPage = new ArrayList<>();
        int currentHeight = 0;

        for (CodexBookSection section : sections) {
            int sectionHeight = section.getHeight(font, pageWidth);

            if (!currentPage.isEmpty() && currentHeight + sectionHeight > pageHeight) {
                pages.add(currentPage);
                currentPage = new ArrayList<>();
                currentHeight = 0;
            }

            currentPage.add(section);
            currentHeight += sectionHeight;
        }

        if (!currentPage.isEmpty()) {
            pages.add(currentPage);
        }

        return pages;
    }

    /**
     * Retourne le nombre total de paires de pages (spreads) nécessaires.
     * Un spread = 2 pages visibles côte à côte.
     * @param totalPages Le nombre total de pages individuelles
     * @return Le nombre de spreads
     */
    public static int getSpreadCount(int totalPages) {
        return Math.max(1, (totalPages + 1) / 2);
    }

    /**
     * Retourne les indices des pages gauche et droite pour un spread donné.
     * @param spreadIndex L'index du spread (0-based)
     * @return Tableau [leftPageIndex, rightPageIndex], -1 si la page n'existe pas
     */
    public static int[] getSpreadPages(int spreadIndex) {
        int left = spreadIndex * 2;
        int right = left + 1;
        return new int[]{left, right};
    }
}
