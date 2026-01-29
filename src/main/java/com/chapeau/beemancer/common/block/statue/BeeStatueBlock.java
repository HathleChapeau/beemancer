/**
 * ============================================================
 * [BeeStatueBlock.java]
 * Description: Statue d'abeille affichant une espèce avec interactions
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation           |
 * |-------------------------|----------------------|-----------------------|
 * | BeeStatueBlockEntity    | Stockage espèce      | getSpeciesId()        |
 * | MagicBeeItem            | Création item abeille| createWithGenes()     |
 * | GeneRegistry            | Accès aux gènes      | getGene()             |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - BeemancerBlocks.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.statue;

import com.chapeau.beemancer.common.item.bee.MagicBeeItem;
import com.chapeau.beemancer.core.gene.BeeGeneData;
import com.chapeau.beemancer.core.gene.Gene;
import com.chapeau.beemancer.core.gene.GeneCategory;
import com.chapeau.beemancer.core.gene.GeneRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Bloc statue d'abeille.
 * - Shift+clic droit: change d'espèce
 * - Clic droit: donne une abeille de l'espèce avec valeurs par défaut
 */
public class BeeStatueBlock extends Block implements EntityBlock {

    // Forme: piédestal avec zone pour l'abeille au-dessus
    private static final VoxelShape PEDESTAL = Block.box(4, 0, 4, 12, 8, 12);
    private static final VoxelShape TOP = Block.box(3, 8, 3, 13, 10, 13);
    private static final VoxelShape SHAPE = Shapes.or(PEDESTAL, TOP);

    public BeeStatueBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BeeStatueBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof BeeStatueBlockEntity statueBE)) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            // Shift+clic droit: changer d'espèce
            if (!level.isClientSide()) {
                statueBE.cycleToNextSpecies();
                player.displayClientMessage(
                    Component.translatable("message.beemancer.bee_statue.species_changed",
                        Component.translatable("species.beemancer." + statueBE.getSpeciesId()),
                        statueBE.getCurrentSpeciesIndex(),
                        statueBE.getTotalSpeciesCount()),
                    true
                );
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        } else {
            // Clic droit simple: donner une abeille de l'espèce
            if (!level.isClientSide()) {
                String speciesId = statueBE.getSpeciesId();
                ItemStack beeStack = createBeeItemForSpecies(speciesId);

                if (!beeStack.isEmpty()) {
                    if (!player.getInventory().add(beeStack)) {
                        player.drop(beeStack, false);
                    }
                    player.displayClientMessage(
                        Component.translatable("message.beemancer.bee_statue.bee_given",
                            Component.translatable("species.beemancer." + speciesId)),
                        true
                    );
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
    }

    /**
     * Crée un item MagicBee avec les gènes par défaut pour une espèce.
     */
    private ItemStack createBeeItemForSpecies(String speciesId) {
        // Créer les données de gènes avec l'espèce
        BeeGeneData geneData = new BeeGeneData();

        // Récupérer le gène d'espèce
        Gene speciesGene = GeneRegistry.getGene(GeneCategory.SPECIES, speciesId);
        if (speciesGene != null) {
            geneData.setGene(speciesGene);
        }

        // Ajouter les gènes par défaut pour les autres catégories
        for (GeneCategory category : GeneRegistry.getAllCategories()) {
            if (category != GeneCategory.SPECIES && geneData.getGene(category) == null) {
                Gene defaultGene = GeneRegistry.getDefaultGene(category);
                if (defaultGene != null) {
                    geneData.setGene(defaultGene);
                }
            }
        }

        return MagicBeeItem.createWithGenes(geneData);
    }
}
