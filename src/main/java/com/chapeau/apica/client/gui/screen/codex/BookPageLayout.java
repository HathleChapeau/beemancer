/**
 * ============================================================
 * [BookPageLayout.java]
 * Description: Separation des sections en page gauche/droite du Codex Book
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexBookSection    | Sections a repartir  | Detection du page_break        |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CodexBookScreen (split gauche/droite du contenu)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.screen.codex;

import com.chapeau.apica.common.codex.book.CodexBookSection;

import java.util.ArrayList;
import java.util.List;

public class BookPageLayout {

    /**
     * Separe les sections en deux listes au premier PAGE_BREAK rencontre.
     * Tout ce qui est avant le page_break va a gauche, tout ce qui est apres va a droite.
     * S'il n'y a pas de page_break, tout va a gauche et la droite est vide.
     *
     * @param sections Les sections a repartir
     * @return Liste de 2 elements: [0] = page gauche, [1] = page droite
     */
    public static List<List<CodexBookSection>> splitAtPageBreak(List<CodexBookSection> sections) {
        List<CodexBookSection> left = new ArrayList<>();
        List<CodexBookSection> right = new ArrayList<>();

        boolean afterBreak = false;
        for (CodexBookSection section : sections) {
            if (section.getType() == CodexBookSection.SectionType.PAGE_BREAK) {
                afterBreak = true;
                continue;
            }
            if (afterBreak) {
                right.add(section);
            } else {
                left.add(section);
            }
        }

        return List.of(left, right);
    }
}
