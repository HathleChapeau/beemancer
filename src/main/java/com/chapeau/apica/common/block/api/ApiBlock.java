/**
 * ============================================================
 * [ApiBlock.java]
 * Description: Bloc Api, créature vivante qui grandit et peut exploser
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ApiBlockEntity      | Stockage state       | Level, cooldown, scale         |
 * | ApicaTags           | Tag combs            | Détection items comb           |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ApicaBlocks.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.api;

import com.chapeau.apica.core.util.ParticleHelper;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.chapeau.apica.core.registry.ApicaBlockEntities;

import javax.annotation.Nullable;

/**
 * Bloc Api:
 * - Honey Bottle → +1 level, rend glass bottle
 * - Item tag apica:combs → -1 level
 * - VoxelShape dynamique basé sur le level complété du BE
 * - Rendu invisible (fait par ApiRenderer)
 */
public class ApiBlock extends BaseEntityBlock {

    public static final MapCodec<ApiBlock> CODEC = simpleCodec(ApiBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final TagKey<net.minecraft.world.item.Item> API_LIKE_TAG =
        TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("apica", "api_like"));
    private static final TagKey<net.minecraft.world.item.Item> API_DISLIKE_TAG =
        TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("apica", "api_dislike"));

    // Shape de base: ~10x9x10 centré (scale 0.8 du modèle tilté 45°)
    private static final VoxelShape BASE_SHAPE = Block.box(3, 0, 3, 13, 9, 13);

    public ApiBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    // ==================== BlockEntity ====================

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ApiBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ApicaBlockEntities.API.get(), ApiBlockEntity::serverTick);
    }

    // ==================== Placement ====================

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof ApiBlockEntity apiBE) {
            if (placer instanceof Player player) {
                apiBE.setOwner(player);
            }
            if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
                apiBE.setCustomName(stack.getHoverName());
            }
        }
    }

    // ==================== Drops ====================

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof ApiBlockEntity apiBE) {
                Block.popResource(level, pos, apiBE.createNamedDrop());
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // ==================== Render ====================

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    // ==================== VoxelShape dynamique ====================

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ApiBlockEntity apiBE)) {
            return BASE_SHAPE;
        }

        float scale = apiBE.getCollisionScale();
        float halfWidth = 6.5f * scale;
        float height = 11.0f * scale;

        // Centré en X/Z (8 - halfWidth, 8 + halfWidth), base à Y=0
        float minX = Math.max(0, 8.0f - halfWidth);
        float maxX = Math.min(16, 8.0f + halfWidth);
        float minZ = Math.max(0, 8.0f - halfWidth);
        float maxZ = Math.min(16, 8.0f + halfWidth);
        float maxY = Math.min(16, height);

        return Block.box(minX, 0, minZ, maxX, maxY, maxZ);
    }

    // ==================== Interactions ====================

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hitResult) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ApiBlockEntity apiBE)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Name tag → renommer Api
        if (stack.is(Items.NAME_TAG) && stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
            if (!level.isClientSide()) {
                apiBE.setCustomName(stack.getHoverName());
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return ItemInteractionResult.SUCCESS;
        }

        // Owner check: seul le propriétaire peut nourrir/shrink
        if (!apiBE.isOwner(player)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        long gameTime = level.getGameTime();

        // Cooldown check
        if (apiBE.isOnCooldown(gameTime)) {
            return ItemInteractionResult.CONSUME;
        }

        // Items aimes (tag api_like) → feed (+1 level)
        if (stack.is(API_LIKE_TAG)) {
            if (!level.isClientSide()) {
                apiBE.feed(gameTime);

                // Consommer l'item et rendre un glass bottle si crafting remainder
                if (!player.getAbilities().instabuild) {
                    ItemStack remainder = stack.getCraftingRemainingItem();
                    stack.shrink(1);
                    if (!remainder.isEmpty()) {
                        if (!player.getInventory().add(remainder)) {
                            player.drop(remainder, false);
                        }
                    }
                }

                // Particules coeurs
                Vec3 center = Vec3.atCenterOf(pos);
                if (level instanceof ServerLevel serverLevel) {
                    ParticleHelper.burst(serverLevel, center, ParticleHelper.EffectType.HEAL, 8);
                }

                SoundType honeycombSound = Blocks.HONEYCOMB_BLOCK.defaultBlockState().getSoundType();
                level.playSound(null, pos, honeycombSound.getPlaceSound(), SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return ItemInteractionResult.SUCCESS;
        }

        // Items detestes (tag api_dislike) → shrink (-1 level)
        if (stack.is(API_DISLIKE_TAG) && apiBE.getApiLevel() > 0) {
            if (!level.isClientSide()) {
                apiBE.shrink(gameTime);

                // Consommer le comb
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                // Particules échec
                Vec3 center = Vec3.atCenterOf(pos);
                if (level instanceof ServerLevel serverLevel) {
                    ParticleHelper.burst(serverLevel, center, ParticleHelper.EffectType.FAILURE, 6);
                }

                SoundType honeycombSound = Blocks.HONEYCOMB_BLOCK.defaultBlockState().getSoundType();
                level.playSound(null, pos, honeycombSound.getPlaceSound(), SoundSource.BLOCKS, 1.0f, 0.7f);
            }
            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof ApiBlockEntity apiBE) {
            if (!level.isClientSide()) {
                apiBE.cycleAnimState();
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
