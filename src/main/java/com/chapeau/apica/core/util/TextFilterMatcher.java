/**
 * ============================================================
 * [TextFilterMatcher.java]
 * Description: Utilitaire de matching texte pour filtres d'items (syntaxe JEI-like)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ItemStack           | Item a tester        | Nom, namespace, tags           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ItemFilterData.java (filtre pipe)
 * - InterfaceFilter.java (filtre import/export)
 * - StorageTerminalScreen.java (recherche terminal)
 *
 * ============================================================
 */
package com.chapeau.apica.core.util;

import net.minecraft.world.item.ItemStack;

/**
 * Matching texte unifie pour tous les systemes de filtrage d'items.
 * Syntaxe supportee :
 * - Texte libre : substring case-insensitive sur le nom d'affichage
 * - @mod : substring case-insensitive sur le namespace du mod
 * - #tag : match exact sur le path du tag ou le full tag location
 */
public final class TextFilterMatcher {

    private TextFilterMatcher() {}

    /**
     * Teste si un ItemStack correspond au filtre texte.
     *
     * @param stack  l'item a tester
     * @param filter le filtre (ex: "stone", "@apica", "#planks", "#c:logs")
     * @return true si l'item correspond au filtre
     */
    public static boolean matches(ItemStack stack, String filter) {
        if (stack.isEmpty() || filter == null || filter.isEmpty()) return false;

        if (filter.startsWith("#")) {
            return matchesTag(stack, filter.substring(1));
        } else if (filter.startsWith("@")) {
            return matchesMod(stack, filter.substring(1));
        } else {
            return stack.getHoverName().getString().toLowerCase().contains(filter.toLowerCase());
        }
    }

    /**
     * Teste si le namespace du mod de l'item contient la query (substring, case-insensitive).
     * Ex: "@api" matche "apica", "@mine" matche "minecraft".
     */
    private static boolean matchesMod(ItemStack stack, String query) {
        if (query.isEmpty()) return true;
        String ns = stack.getItem().builtInRegistryHolder().key().location().getNamespace();
        return ns.contains(query.toLowerCase());
    }

    /**
     * Teste si l'item possede un tag dont le path ou le full location correspond.
     * Ex: "#planks" matche le tag "minecraft:planks" (par le path).
     * Ex: "#c:logs" matche le tag "c:logs" (par le full location).
     */
    private static boolean matchesTag(ItemStack stack, String tagName) {
        if (tagName.isEmpty()) return false;
        for (var tag : stack.getTags().toList()) {
            String fullPath = tag.location().toString();
            String path = tag.location().getPath();
            if (path.equals(tagName) || fullPath.equals(tagName)) {
                return true;
            }
        }
        return false;
    }
}
