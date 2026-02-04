/**
 * ============================================================
 * [ControlledHiveBlock.java]
 * Description: Bloc ruche contrôlée pour le multibloc Storage Controller
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Block               | Base Minecraft       | Bloc simple                    |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - BeemancerBlocks.java (enregistrement)
 * - MultiblockPatterns.java (pattern storage_controller)
 * - StorageControllerBlockEntity.java (spawn/retour abeilles)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.storage;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import com.chapeau.beemancer.core.multiblock.MultiblockProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Bloc ruche passive pour le multibloc Storage Controller.
 * Placé en haut de la structure (0, +1, 0), sert de point
 * d'entrée/sortie pour les delivery bees.
 * Aucun comportement propre — le controller gère tout.
 */
public class ControlledHiveBlock extends Block {

    public static final EnumProperty<MultiblockProperty> MULTIBLOCK = MultiblockProperty.create("storage");
    public static final IntegerProperty FORMED_ROTATION = IntegerProperty.create("formed_rotation", 0, 3);

    public ControlledHiveBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(MULTIBLOCK, MultiblockProperty.NONE)
            .setValue(FORMED_ROTATION, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MULTIBLOCK, FORMED_ROTATION);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
