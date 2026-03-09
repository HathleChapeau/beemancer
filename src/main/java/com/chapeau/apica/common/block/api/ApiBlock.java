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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

    private static final TagKey<net.minecraft.world.item.Item> COMBS_TAG =
        TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("apica", "combs"));

    // Shape de base: 10x10x10 centré (from 3,0,3 to 13,10,13)
    private static final VoxelShape BASE_SHAPE = Block.box(3, 0, 3, 13, 10, 13);

    public ApiBlock(Properties properties) {
        super(properties);
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

        float scale = apiBE.getCompletedScale();
        float halfWidth = 5.0f * scale;
        float height = 10.0f * scale;

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

        long gameTime = level.getGameTime();

        // Cooldown check
        if (apiBE.isOnCooldown(gameTime)) {
            return ItemInteractionResult.CONSUME;
        }

        // Honey Bottle → feed (+1 level)
        if (stack.is(Items.HONEY_BOTTLE)) {
            if (!level.isClientSide()) {
                apiBE.feed(gameTime);

                // Consommer la bouteille et rendre un glass bottle
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                    ItemStack glassBottle = new ItemStack(Items.GLASS_BOTTLE);
                    if (!player.getInventory().add(glassBottle)) {
                        player.drop(glassBottle, false);
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

        // Comb → shrink (-1 level)
        if (stack.is(COMBS_TAG) && apiBE.getApiLevel() > 0) {
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
}
