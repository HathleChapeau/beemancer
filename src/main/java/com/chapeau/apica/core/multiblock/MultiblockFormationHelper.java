/**
 * ============================================================
 * [MultiblockFormationHelper.java]
 * Description: Utilitaires communs pour la formation/destruction des multiblocs
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | MultiblockPattern   | Pattern structure    | Itération des éléments         |
 * | MultiblockProperty  | Blockstate value     | Set FORMED/NONE                |
 * | BlockMatcher        | Air detection        | Skip air matchers              |
 * | HoneyReservoirBE    | Réservoirs           | Link/unlink controllerPos      |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - Tous les contrôleurs de multiblocs (formation/destruction)
 *
 * ============================================================
 */
package com.chapeau.apica.core.multiblock;

import com.chapeau.apica.common.block.altar.HoneyReservoirBlock;
import com.chapeau.apica.common.blockentity.altar.HoneyReservoirBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Méthodes utilitaires partagées par tous les contrôleurs de multiblocs.
 * Élimine la duplication de code entre les différentes implémentations.
 */
public class MultiblockFormationHelper {

    /**
     * Met à jour la propriété MULTIBLOCK sur tous les blocs de la structure.
     * Itère le pattern, applique la rotation, et set la valeur sur chaque bloc.
     *
     * @param level Le monde
     * @param controllerPos Position du contrôleur
     * @param pattern Le pattern du multibloc
     * @param formedValue La valeur MultiblockProperty quand formé (ex: CENTRIFUGE, ALTAR)
     * @param rotation Rotation horizontale (0-3)
     */
    public static void setFormedOnStructureBlocks(Level level, BlockPos controllerPos,
                                                   MultiblockPattern pattern, MultiblockProperty formedValue,
                                                   int rotation) {
        setFormedOnStructureBlocks(level, controllerPos, pattern, offset -> formedValue, rotation);
    }

    /**
     * Met à jour la propriété MULTIBLOCK sur tous les blocs de la structure,
     * avec une fonction de mapping pour les cas où la valeur dépend de la position
     * (ex: Alembic avec ALEMBIC_0 et ALEMBIC_1).
     *
     * @param level Le monde
     * @param controllerPos Position du contrôleur
     * @param pattern Le pattern du multibloc
     * @param valueMapper Fonction qui retourne la MultiblockProperty pour un offset donné
     * @param rotation Rotation horizontale (0-3)
     */
    public static void setFormedOnStructureBlocks(Level level, BlockPos controllerPos,
                                                   MultiblockPattern pattern,
                                                   Function<Vec3i, MultiblockProperty> valueMapper,
                                                   int rotation) {
        for (MultiblockPattern.PatternElement element : pattern.getElements()) {
            if (BlockMatcher.isAirMatcher(element.matcher())) continue;

            Vec3i rotatedOffset = MultiblockPattern.rotateY(element.offset(), rotation);
            BlockPos blockPos = controllerPos.offset(rotatedOffset);
            if (!level.hasChunkAt(blockPos)) continue;
            BlockState state = level.getBlockState(blockPos);

            MultiblockProperty value = valueMapper.apply(element.offset());
            setMultiblockProperty(level, blockPos, state, value);
        }
    }

    /**
     * Retire la propriété MULTIBLOCK (set NONE) sur tous les blocs de la structure.
     *
     * @param level Le monde
     * @param controllerPos Position du contrôleur
     * @param pattern Le pattern du multibloc
     * @param rotation Rotation horizontale (0-3)
     */
    public static void clearFormedOnStructureBlocks(Level level, BlockPos controllerPos,
                                                     MultiblockPattern pattern, int rotation) {
        setFormedOnStructureBlocks(level, controllerPos, pattern, MultiblockProperty.NONE, rotation);
    }

    /**
     * Invalide les capabilities de TOUS les blocs du multibloc (contrôleur + structure).
     * Force NeoForge à re-query les lambdas de capabilities pour chaque bloc.
     *
     * @param level Le monde
     * @param controllerPos Position du contrôleur
     * @param pattern Le pattern du multibloc
     * @param rotation Rotation horizontale (0-3)
     */
    public static void invalidateAllCapabilities(Level level, BlockPos controllerPos,
                                                  MultiblockPattern pattern, int rotation) {
        // Invalider le contrôleur
        level.invalidateCapabilities(controllerPos);
        // Invalider tous les blocs structurels
        for (MultiblockPattern.PatternElement element : pattern.getElements()) {
            Vec3i rotatedOffset = MultiblockPattern.rotateY(element.offset(), rotation);
            BlockPos blockPos = controllerPos.offset(rotatedOffset);
            level.invalidateCapabilities(blockPos);
        }
    }

    /**
     * Lie ou délie les réservoirs au contrôleur pour la délégation de capabilities.
     * Les offsets sont appliqués avec rotation.
     *
     * @param level Le monde
     * @param controllerPos Position du contrôleur
     * @param reservoirOffsets Offsets relatifs des réservoirs (non rotatés)
     * @param rotation Rotation horizontale (0-3)
     * @param link true pour lier, false pour délier
     */
    public static void linkReservoirs(Level level, BlockPos controllerPos,
                                       BlockPos[] reservoirOffsets, int rotation, boolean link) {
        for (BlockPos offset : reservoirOffsets) {
            Vec3i rotatedOffset = MultiblockPattern.rotateY(offset, rotation);
            BlockPos reservoirPos = controllerPos.offset(rotatedOffset);
            if (level.getBlockEntity(reservoirPos) instanceof HoneyReservoirBlockEntity reservoir) {
                reservoir.setControllerPosQuiet(link ? controllerPos : null);
            }
        }
    }

    /**
     * Définit la propriété FACING sur les réservoirs du multibloc.
     * Utilisé pour que les modèles des réservoirs soient correctement rotatés.
     *
     * @param level Le monde
     * @param controllerPos Position du contrôleur
     * @param reservoirOffsets Offsets relatifs des réservoirs (non rotatés)
     * @param rotation Rotation horizontale (0-3)
     * @param facing La direction FACING à appliquer
     */
    public static void setFacingOnReservoirs(Level level, BlockPos controllerPos,
                                              BlockPos[] reservoirOffsets, int rotation, Direction facing) {
        for (BlockPos offset : reservoirOffsets) {
            Vec3i rotatedOffset = MultiblockPattern.rotateY(offset, rotation);
            BlockPos reservoirPos = controllerPos.offset(rotatedOffset);
            BlockState state = level.getBlockState(reservoirPos);
            if (state.hasProperty(HoneyReservoirBlock.FACING) && state.getValue(HoneyReservoirBlock.FACING) != facing) {
                level.setBlock(reservoirPos, state.setValue(HoneyReservoirBlock.FACING, facing), 3);
            }
        }
    }

    /**
     * Définit la propriété FACING sur tous les blocs de la structure qui la supportent.
     * Itère le pattern, applique la rotation, et set FACING sur chaque bloc applicable.
     *
     * @param level Le monde
     * @param controllerPos Position du contrôleur
     * @param pattern Le pattern du multibloc
     * @param rotation Rotation horizontale (0-3)
     * @param facing La direction FACING à appliquer
     */
    @SuppressWarnings("unchecked")
    public static void setFacingOnStructureBlocks(Level level, BlockPos controllerPos,
                                                   MultiblockPattern pattern, int rotation, Direction facing) {
        for (MultiblockPattern.PatternElement element : pattern.getElements()) {
            if (BlockMatcher.isAirMatcher(element.matcher())) continue;

            Vec3i rotatedOffset = MultiblockPattern.rotateY(element.offset(), rotation);
            BlockPos blockPos = controllerPos.offset(rotatedOffset);
            if (!level.hasChunkAt(blockPos)) continue;
            BlockState state = level.getBlockState(blockPos);

            // Cherche une propriété "facing" de type Direction horizontale
            for (var prop : state.getProperties()) {
                if (prop.getName().equals("facing") && prop.getValueClass() == Direction.class) {
                    var dirProp = (net.minecraft.world.level.block.state.properties.DirectionProperty) prop;
                    if (dirProp.getPossibleValues().contains(facing) && state.getValue(dirProp) != facing) {
                        level.setBlock(blockPos, state.setValue(dirProp, facing), 3);
                    }
                    break;
                }
            }
        }
    }

    /**
     * Lie ou délie plusieurs groupes de réservoirs au contrôleur.
     *
     * @param level Le monde
     * @param controllerPos Position du contrôleur
     * @param offsetGroups Plusieurs tableaux d'offsets de réservoirs
     * @param rotation Rotation horizontale (0-3)
     * @param link true pour lier, false pour délier
     */
    public static void linkReservoirs(Level level, BlockPos controllerPos,
                                       BlockPos[][] offsetGroups, int rotation, boolean link) {
        for (BlockPos[] offsets : offsetGroups) {
            linkReservoirs(level, controllerPos, offsets, rotation, link);
        }
    }

    /**
     * Set la propriété "multiblock" sur un blockstate de manière générique.
     * Cherche la propriété par nom et applique la valeur si possible.
     */
    @SuppressWarnings("unchecked")
    private static void setMultiblockProperty(Level level, BlockPos pos, BlockState state,
                                               MultiblockProperty value) {
        for (var prop : state.getProperties()) {
            if (prop.getName().equals("multiblock") && prop instanceof EnumProperty<?> enumProp) {
                EnumProperty<MultiblockProperty> mbProp = (EnumProperty<MultiblockProperty>) enumProp;
                if (mbProp.getPossibleValues().contains(value) && state.getValue(mbProp) != value) {
                    level.setBlock(pos, state.setValue(mbProp, value), 3);
                }
                break;
            }
        }
    }
}
