/**
 * ============================================================
 * [ApiBlockEntity.java]
 * Description: BlockEntity pour Api, créature-bloc vivante qui grandit
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ApicaBlockEntities  | Type BlockEntity     | Enregistrement                 |
 * | ParticleHelper      | Spawn particules     | Effets visuels                 |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ApiBlock.java (création BlockEntity, tick)
 * - ApiRenderer.java (getVisualScale)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.api;

import com.chapeau.apica.core.registry.ApicaBlockEntities;
import com.chapeau.apica.core.util.ParticleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Api grandit quand nourri au honey bottle, rétrécit avec un comb.
 * L'animation de croissance se fait en 3 phases:
 * - Délai (5 min) → Interpolation (10 min) → Taille finale + collision check
 */
public class ApiBlockEntity extends BlockEntity {

    // --- Constantes ---
    public static final float SCALE_VALUE = 0.15f;
    public static final int COOLDOWN_TICKS = 24000;
    public static final int GROWTH_DELAY_TICKS = 6000;
    public static final int GROWTH_DURATION_TICKS = 12000;
    public static final int GROWTH_TOTAL_TICKS = GROWTH_DELAY_TICKS + GROWTH_DURATION_TICKS;
    public static final float MAX_HARDNESS = 3.0f;

    // --- State ---
    private int apiLevel = 0;
    private long lastFeedTick = -COOLDOWN_TICKS;
    private boolean growing = false;
    private boolean shrinking = false;
    private boolean animationComplete = true;

    public ApiBlockEntity(BlockPos pos, BlockState state) {
        super(ApicaBlockEntities.API.get(), pos, state);
    }

    // ==================== Public Accessors ====================

    public int getApiLevel() {
        return apiLevel;
    }

    public boolean isOnCooldown(long gameTime) {
        return (gameTime - lastFeedTick) < COOLDOWN_TICKS;
    }

    /**
     * Nourrit Api: +1 level avec animation de croissance.
     */
    public void feed(long gameTime) {
        apiLevel++;
        lastFeedTick = gameTime;
        growing = true;
        shrinking = false;
        animationComplete = false;
        setChanged();
        syncToClient();
    }

    /**
     * Donne un comb à Api: -1 level avec animation de rétrécissement.
     */
    public void shrink(long gameTime) {
        if (apiLevel <= 0) return;
        apiLevel--;
        lastFeedTick = gameTime;
        growing = false;
        shrinking = true;
        animationComplete = false;
        setChanged();
        syncToClient();
    }

    /**
     * Retourne le scale visuel actuel pour le renderer (avec interpolation).
     */
    public float getVisualScale(float partialTick) {
        if (animationComplete || level == null) {
            return getCompletedScale();
        }

        long elapsed = level.getGameTime() - lastFeedTick;

        // Phase délai: pas de changement visuel
        if (elapsed < GROWTH_DELAY_TICKS) {
            return getPreviousScale();
        }

        // Phase interpolation
        float progress = (float)(elapsed - GROWTH_DELAY_TICKS) / GROWTH_DURATION_TICKS;
        progress = Math.min(1.0f, Math.max(0.0f, progress));

        float prevScale = getPreviousScale();
        float targetScale = getCompletedScale();

        return prevScale + (targetScale - prevScale) * progress;
    }

    /**
     * Scale de la taille complétée (level actuel).
     */
    public float getCompletedScale() {
        return 1.0f + (SCALE_VALUE * apiLevel);
    }

    // ==================== Server Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, ApiBlockEntity be) {
        if (be.animationComplete) return;

        long elapsed = level.getGameTime() - be.lastFeedTick;

        // Animation terminée
        if (elapsed >= GROWTH_TOTAL_TICKS) {
            be.animationComplete = true;
            be.growing = false;
            be.shrinking = false;
            be.setChanged();
            be.syncToClient();

            // Recalculer la collision shape via block update
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);

            // Vérifier les collisions avec les blocs adjacents
            if (be.apiLevel > 0) {
                be.checkAdjacentCollisions(level, pos);
            }
        }
    }

    // ==================== Collision Logic ====================

    /**
     * Vérifie si Api dépasse [0,16] pixels et touche les blocs adjacents.
     */
    private void checkAdjacentCollisions(Level level, BlockPos pos) {
        float sizePixels = 10.0f * getCompletedScale();
        float halfSize = sizePixels / 2.0f;

        // Api est centré dans le bloc (centre à 8,5,8)
        // Il dépasse si halfSize > 8 (soit sizePixels > 16)
        if (sizePixels <= 16.0f) return;

        // Vérifier chaque direction horizontale + up (pas down car il est posé au sol)
        Direction[] directionsToCheck = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP
        };

        for (Direction dir : directionsToCheck) {
            float extent;
            if (dir == Direction.UP) {
                // Verticalement: base à 0, hauteur = sizePixels
                extent = sizePixels;
            } else {
                // Horizontalement: centré à 8, extends de halfSize de chaque côté
                extent = 8.0f + halfSize;
            }

            // Dépasse du bloc si extent > 16
            if (extent > 16.0f) {
                BlockPos adjacent = pos.relative(dir);
                BlockState adjacentState = level.getBlockState(adjacent);

                if (adjacentState.isAir() || adjacentState.getBlock() == Blocks.WATER) continue;

                float hardness = adjacentState.getDestroySpeed(level, adjacent);

                if (hardness >= 0 && hardness < MAX_HARDNESS) {
                    // Bloc mou: casser et drop
                    level.destroyBlock(adjacent, true);
                } else {
                    // Bloc dur ou indestructible: Api explose
                    destroySelf(level, pos);
                    return;
                }
            }
        }
    }

    /**
     * Api se casse: drop un honeycomb + explosion particules.
     */
    private void destroySelf(Level level, BlockPos pos) {
        // Drop honeycomb
        Block.popResource(level, pos, new ItemStack(net.minecraft.world.item.Items.HONEYCOMB));

        // Particules d'explosion
        if (level instanceof ServerLevel serverLevel) {
            Vec3 center = Vec3.atCenterOf(pos);
            ParticleHelper.sphere(serverLevel, center, ParticleHelper.EffectType.HONEY, 40, 0.8);
        }

        // Casser le bloc
        level.destroyBlock(pos, false);
    }

    // ==================== Helpers ====================

    /**
     * Scale de la taille avant la dernière modification.
     */
    private float getPreviousScale() {
        if (growing) {
            return 1.0f + (SCALE_VALUE * (apiLevel - 1));
        } else if (shrinking) {
            return 1.0f + (SCALE_VALUE * (apiLevel + 1));
        }
        return getCompletedScale();
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("ApiLevel", apiLevel);
        tag.putLong("LastFeedTick", lastFeedTick);
        tag.putBoolean("Growing", growing);
        tag.putBoolean("Shrinking", shrinking);
        tag.putBoolean("AnimComplete", animationComplete);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        apiLevel = tag.getInt("ApiLevel");
        lastFeedTick = tag.getLong("LastFeedTick");
        growing = tag.getBoolean("Growing");
        shrinking = tag.getBoolean("Shrinking");
        animationComplete = tag.getBoolean("AnimComplete");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
