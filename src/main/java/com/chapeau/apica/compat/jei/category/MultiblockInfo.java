/**
 * ============================================================
 * [MultiblockInfo.java]
 * Description: Wrapper pour afficher un multibloc dans JEI
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | MultiblockPattern   | Donnees structure    | Elements et positions          |
 * | BlockMatcher        | Acces blocs display  | Recuperation des blocs         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - MultiblockCategory (affichage JEI)
 * - ApicaJeiPlugin (enregistrement recipes)
 *
 * ============================================================
 */
package com.chapeau.apica.compat.jei.category;

import com.chapeau.apica.core.multiblock.BlockMatcher;
import com.chapeau.apica.core.multiblock.MultiblockPattern;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Represente les informations d'un multibloc pour l'affichage JEI.
 */
public record MultiblockInfo(
        String id,
        String name,
        ItemStack controllerStack,
        List<ItemStack> requiredBlocks,
        MultiblockPattern pattern
) {

    /**
     * Cree un MultiblockInfo a partir d'un pattern et d'un bloc controleur.
     */
    public static MultiblockInfo create(String id, String translationKey, Block controller, MultiblockPattern pattern) {
        ItemStack controllerStack = new ItemStack(controller.asItem());

        // Collecter tous les blocs requis (sans doublons pour l'affichage)
        List<ItemStack> blocks = new ArrayList<>();
        blocks.add(controllerStack.copy());

        for (MultiblockPattern.PatternElement element : pattern.getElements()) {
            if (BlockMatcher.isAirMatcher(element.matcher())) {
                continue;
            }
            Block block = BlockMatcher.getDisplayBlock(element.matcher());
            if (block == null) continue;

            ItemStack stack = new ItemStack(block.asItem());
            if (stack.isEmpty()) continue;

            // Verifier si ce bloc est deja dans la liste
            boolean found = false;
            for (ItemStack existing : blocks) {
                if (ItemStack.isSameItem(existing, stack)) {
                    existing.grow(1);
                    found = true;
                    break;
                }
            }
            if (!found) {
                blocks.add(stack);
            }
        }

        return new MultiblockInfo(id, translationKey, controllerStack, blocks, pattern);
    }
}
