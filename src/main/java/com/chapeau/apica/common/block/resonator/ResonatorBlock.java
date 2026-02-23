/**
 * ============================================================
 * [ResonatorBlock.java]
 * Description: Bloc resonateur avec GUI d'onde interactive et support abeille
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | ResonatorBlockEntity    | Stockage parametres  | Persistance freq/knobs/bee     |
 * | BaseEntityBlock         | Support BlockEntity  | newBlockEntity()               |
 * | ApicaItems              | MagicBee check       | Filtre item abeille            |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ApicaBlocks (enregistrement)
 * - ApicaItems (BlockItem)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.resonator;

import com.chapeau.apica.client.particle.ParticleEmitter;
import com.chapeau.apica.common.codex.CodexPlayerData;
import com.chapeau.apica.common.item.bee.MagicBeeItem;
import com.chapeau.apica.common.item.essence.EssenceItem;
import com.chapeau.apica.core.bee.BeeSpeciesManager;
import com.chapeau.apica.core.registry.ApicaAttachments;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import com.chapeau.apica.core.registry.ApicaItems;
import com.chapeau.apica.core.registry.ApicaParticles;
import com.chapeau.apica.core.util.BeeInjectionHelper;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class ResonatorBlock extends BaseEntityBlock {

    public static final MapCodec<ResonatorBlock> CODEC = simpleCodec(ResonatorBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
        Block.box(0, 0, 0, 16, 6, 16),    // Plateforme de base
        Block.box(3, 6, 3, 13, 7, 13)     // Anneau central
    );

    public ResonatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ResonatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ApicaBlockEntities.RESONATOR.get(),
                ResonatorBlockEntity::serverTick);
    }

    // Clic droit avec item en main (abeille)
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                               BlockPos pos, Player player, InteractionHand hand,
                                               BlockHitResult hitResult) {
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;

        if (!stack.is(ApicaItems.MAGIC_BEE.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ResonatorBlockEntity resonator)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (resonator.hasBee()) {
            // Swap: retirer l'ancienne abeille et placer la nouvelle
            ItemStack oldBee = resonator.removeBee();
            resonator.placeBee(stack);
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            // Donner l'ancienne abeille au joueur
            if (!player.getInventory().add(oldBee)) {
                player.drop(oldBee, false);
            }
        } else {
            // Placer l'abeille
            resonator.placeBee(stack);
            if (!player.isCreative()) {
                stack.shrink(1);
            }
        }

        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
        return ItemInteractionResult.CONSUME;
    }

    // Clic droit sans item en main
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ResonatorBlockEntity resonator) {
                // Shift+clic droit: reprendre l'abeille (interdit pendant analyse)
                if (player.isShiftKeyDown() && resonator.hasBee() && !resonator.isAnalysisInProgress()) {
                    ItemStack removed = resonator.removeBee();
                    if (!removed.isEmpty()) {
                        if (!player.getInventory().add(removed)) {
                            player.drop(removed, false);
                        }
                        level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM,
                                SoundSource.BLOCKS, 1.0f, 1.0f);
                        return InteractionResult.CONSUME;
                    }
                }
                // Ouvrir le menu (analyse ou normal)
                if (player instanceof ServerPlayer serverPlayer) {
                    boolean analysisMode = false;
                    if (resonator.hasBee()) {
                        String speciesId = MagicBeeItem.getSpeciesId(resonator.getStoredBee());
                        if (speciesId != null) {
                            CodexPlayerData codex = serverPlayer.getData(ApicaAttachments.CODEX_DATA);
                            if (hasUnknownKnowledge(codex, speciesId, resonator.getStoredBee())) {
                                analysisMode = true;
                                if (!resonator.isAnalysisInProgress()) {
                                    resonator.startAnalysis(serverPlayer.getUUID());
                                }
                            }
                        }
                    }
                    final boolean sendAnalysisMode = analysisMode;
                    serverPlayer.openMenu(resonator, buf -> {
                        buf.writeBlockPos(pos);
                        ItemStack.OPTIONAL_STREAM_CODEC.encode(
                                (RegistryFriendlyByteBuf) buf, resonator.getStoredBee());
                        buf.writeBoolean(sendAnalysisMode);
                    });
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * Verifie si l'espece ou un des 5 traits de l'abeille est inconnu du joueur.
     */
    private static boolean hasUnknownKnowledge(CodexPlayerData codex, String speciesId, ItemStack bee) {
        if (!codex.isSpeciesKnown(speciesId)) return true;

        BeeSpeciesManager.BeeSpeciesData data = BeeSpeciesManager.getSpecies(speciesId);
        if (data == null) return false;

        int drop = data.dropLevel + BeeInjectionHelper.getBonusLevel(bee, EssenceItem.EssenceType.DROP);
        if (!codex.isTraitKnown("drop:" + drop)) return true;

        int speed = data.flyingSpeedLevel + BeeInjectionHelper.getBonusLevel(bee, EssenceItem.EssenceType.SPEED);
        if (!codex.isTraitKnown("speed:" + speed)) return true;

        int foraging = data.foragingDurationLevel + BeeInjectionHelper.getBonusLevel(bee, EssenceItem.EssenceType.FORAGING);
        if (!codex.isTraitKnown("foraging:" + foraging)) return true;

        int tolerance = data.toleranceLevel + BeeInjectionHelper.getBonusLevel(bee, EssenceItem.EssenceType.TOLERANCE);
        if (!codex.isTraitKnown("tolerance:" + tolerance)) return true;

        int activity = BeeInjectionHelper.getActivityLevel(data.dayNight) + 1
                + BeeInjectionHelper.getBonusLevel(bee, EssenceItem.EssenceType.DIURNAL);
        if (!codex.isTraitKnown("activity:" + activity)) return true;

        return false;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ResonatorBlockEntity resonator)) return;
        if (!resonator.isAnalysisInProgress()) return;

        // Particules rune montant du bas vers le haut (comme le storage relay)
        new ParticleEmitter(ApicaParticles.RUNE.get())
            .at(pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5)
            .speed(0, 0.015, 0)
            .spread(0.25, 0.1, 0.25)
            .speedVariance(0.01, 0.005, 0.01)
            .count(3)
            .lifetime(15)
            .gravity(-0.002f)
            .scale(0.06f)
            .fadeOut()
            .fullBright()
            .spawn(level);
    }

    // Drop l'abeille quand le bloc est casse
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ResonatorBlockEntity resonator && resonator.hasBee()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        resonator.getStoredBee());
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
