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
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Api grandit quand nourri au honey bottle, rétrécit avec un comb.
 * L'animation de croissance se fait en 3 phases:
 * - Délai (5 min) → Interpolation (10 min) → Taille finale + collision check
 */
public class ApiBlockEntity extends BlockEntity {

    // --- Constantes ---
    public static final float BASE_SCALE = 0.8f;
    public static final float SCALE_VALUE = 0.15f;
    public static final int COOLDOWN_TICKS = 24000;
    public static final int GROWTH_DELAY_TICKS = 6000;
    public static final int GROWTH_DURATION_TICKS = 12000;
    public static final int GROWTH_TOTAL_TICKS = GROWTH_DELAY_TICKS + GROWTH_DURATION_TICKS;
    public static final float MAX_HARDNESS = 3.0f;

    // --- Face System ---
    private static final int BLINK_DURATION_TICKS = 6; // 0.3 sec
    private static final int BLINK_MIN_DELAY_TICKS = 60; // 3 sec
    private static final int BLINK_MAX_DELAY_TICKS = 140; // 7 sec
    private static final double CLOSE_DISTANCE = 6.0;
    private static final double VERY_CLOSE_DISTANCE = 2.0;

    // Tags pour items aimes/detestes par Api
    private static final TagKey<Item> API_LIKE_TAG =
        TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("apica", "api_like"));
    private static final TagKey<Item> API_DISLIKE_TAG =
        TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("apica", "api_dislike"));

    // --- State ---
    private int apiLevel = 0;
    private long lastFeedTick = -COOLDOWN_TICKS;
    private boolean growing = false;
    private boolean shrinking = false;
    private boolean animationComplete = true;
    @Nullable
    private UUID ownerUUID = null;
    @Nullable
    private Component customName = null;
    private ApiAnimationState animState = ApiAnimationState.IDLE;
    private long animStartTick = 0;

    // --- Blink State ---
    private long nextBlinkTick = 0;
    private boolean isBlinking = false;
    private long blinkStartTick = 0;

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

    public long getRemainingCooldown(long gameTime) {
        return Math.max(0, COOLDOWN_TICKS - (gameTime - lastFeedTick));
    }

    public void setOwner(Player player) {
        this.ownerUUID = player.getUUID();
        setChanged();
    }

    public boolean isOwner(Player player) {
        return ownerUUID == null || ownerUUID.equals(player.getUUID());
    }

    public void setCustomName(@Nullable Component name) {
        this.customName = name;
        setChanged();
        syncToClient();
    }

    @Nullable
    public Component getCustomName() {
        return customName;
    }

    public ApiAnimationState getAnimState() {
        return animState;
    }

    public long getAnimStartTick() {
        return animStartTick;
    }

    public void setAnimState(ApiAnimationState state) {
        this.animState = state;
        this.animStartTick = level != null ? level.getGameTime() : 0;
        setChanged();
        syncToClient();
    }

    public void cycleAnimState() {
        ApiAnimationState[] states = ApiAnimationState.values();
        int next = (animState.ordinal() + 1) % states.length;
        setAnimState(states[next]);
    }

    // ==================== Face System ====================

    public boolean isBlinking() {
        return isBlinking;
    }

    /**
     * Retourne la texture de face pour l'etat IDLE (normal).
     */
    public String getIdleFace() {
        if (level == null) return "idle";

        Player nearestPlayer = findNearestPlayer();
        if (nearestPlayer == null) return "idle";

        double distance = nearestPlayer.distanceToSqr(Vec3.atCenterOf(worldPosition));
        ItemStack heldItem = nearestPlayer.getMainHandItem();

        // Verifier cooldown happy termine
        boolean happyCooldownDone = !isOnCooldown(level.getGameTime());

        if (happyCooldownDone && isLikedItem(heldItem)) {
            if (distance < VERY_CLOSE_DISTANCE * VERY_CLOSE_DISTANCE) {
                return "idle_very_hungry";
            } else if (distance < CLOSE_DISTANCE * CLOSE_DISTANCE) {
                return "idle_hungry";
            }
        }

        if (isDislikedItem(heldItem)) {
            if (distance < VERY_CLOSE_DISTANCE * VERY_CLOSE_DISTANCE) {
                return "idle_very_suspicious";
            } else if (distance < CLOSE_DISTANCE * CLOSE_DISTANCE) {
                return "idle_suspicious";
            }
        }

        return "idle";
    }

    /**
     * Retourne la texture de face pour l'etat IDLE (blink).
     */
    public String getIdleFaceBlink() {
        return getIdleFace() + "_blink";
    }

    /**
     * Retourne la texture de face actuelle selon l'etat d'animation.
     */
    public String getCurrentFace() {
        if (level == null) return "idle";

        long gameTime = level.getGameTime();
        float animTime = gameTime - animStartTick;

        return switch (animState) {
            case IDLE -> isBlinking ? getIdleFaceBlink() : getIdleFace();
            case HITSTOP -> {
                // Effort jusqu'a 60 ticks, puis exhausted
                if (animTime < 60f) {
                    yield "effort";
                } else {
                    yield "exhausted";
                }
            }
            case HAPPY -> "happy";
            case SLEEP -> {
                // Alterne sleeping_1 et sleeping_2 toutes les 1 sec (20 ticks)
                int cycle = (int) (animTime / 20) % 2;
                yield cycle == 0 ? "sleeping_1" : "sleeping_2";
            }
        };
    }

    /**
     * Verifie si l'item est aime par Api (tag apica:api_like).
     */
    private boolean isLikedItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(API_LIKE_TAG);
    }

    /**
     * Verifie si l'item est deteste par Api (tag apica:api_dislike).
     */
    private boolean isDislikedItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(API_DISLIKE_TAG);
    }

    /**
     * Trouve le joueur le plus proche.
     */
    @Nullable
    private Player findNearestPlayer() {
        if (level == null) return null;
        Vec3 center = Vec3.atCenterOf(worldPosition);
        return level.getNearestPlayer(center.x, center.y, center.z, CLOSE_DISTANCE + 1, false);
    }

    /**
     * Schedule le prochain blink avec un delai aleatoire.
     */
    private void scheduleNextBlink(long gameTime) {
        int delay = BLINK_MIN_DELAY_TICKS + level.random.nextInt(BLINK_MAX_DELAY_TICKS - BLINK_MIN_DELAY_TICKS);
        nextBlinkTick = gameTime + delay;
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
        return BASE_SCALE + (SCALE_VALUE * apiLevel);
    }

    /**
     * Scale pour le VoxelShape: retourne la taille précédente pendant l'animation,
     * et la taille complétée une fois l'animation terminée.
     */
    public float getCollisionScale() {
        if (animationComplete) {
            return getCompletedScale();
        }
        return getPreviousScale();
    }

    // ==================== Server Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, ApiBlockEntity be) {
        long gameTime = level.getGameTime();

        // Blink logic (seulement en IDLE)
        if (be.animState == ApiAnimationState.IDLE) {
            be.tickBlink(gameTime);
        } else {
            be.isBlinking = false;
        }

        // Growth animation
        if (!be.animationComplete) {
            long elapsed = gameTime - be.lastFeedTick;

            if (elapsed >= GROWTH_TOTAL_TICKS) {
                be.animationComplete = true;
                be.growing = false;
                be.shrinking = false;
                be.setChanged();
                be.syncToClient();

                level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);

                if (be.apiLevel > 0) {
                    be.checkAdjacentCollisions(level, pos);
                }
            }
        }
    }

    private void tickBlink(long gameTime) {
        if (isBlinking) {
            // Fin du blink?
            if (gameTime >= blinkStartTick + BLINK_DURATION_TICKS) {
                isBlinking = false;
                scheduleNextBlink(gameTime);
                syncToClient();
            }
        } else {
            // Temps de blinker?
            if (nextBlinkTick == 0) {
                scheduleNextBlink(gameTime);
            } else if (gameTime >= nextBlinkTick) {
                isBlinking = true;
                blinkStartTick = gameTime;
                syncToClient();
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
        // Drop honeycomb nommé "Api"
        Block.popResource(level, pos, createNamedDrop());

        // Particules d'explosion
        if (level instanceof ServerLevel serverLevel) {
            Vec3 center = Vec3.atCenterOf(pos);
            ParticleHelper.sphere(serverLevel, center, 0.8, ParticleHelper.EffectType.HONEY, 40);
        }

        // Casser le bloc
        level.destroyBlock(pos, false);
    }

    // ==================== Helpers ====================

    /**
     * Crée un honeycomb nommé avec le nom custom ou "Api" par défaut.
     */
    public ItemStack createNamedDrop() {
        ItemStack stack = new ItemStack(net.minecraft.world.item.Items.HONEYCOMB);
        Component name = customName != null ? customName : Component.literal("Api");
        stack.set(DataComponents.CUSTOM_NAME, name);
        return stack;
    }

    /**
     * Scale de la taille avant la dernière modification.
     */
    private float getPreviousScale() {
        if (growing) {
            return BASE_SCALE + (SCALE_VALUE * (apiLevel - 1));
        } else if (shrinking) {
            return BASE_SCALE + (SCALE_VALUE * (apiLevel + 1));
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
        if (ownerUUID != null) {
            tag.putUUID("Owner", ownerUUID);
        }
        if (customName != null) {
            tag.putString("CustomName", Component.Serializer.toJson(customName, registries));
        }
        tag.putString("AnimState", animState.name());
        tag.putLong("AnimStartTick", animStartTick);
        tag.putLong("NextBlinkTick", nextBlinkTick);
        tag.putBoolean("IsBlinking", isBlinking);
        tag.putLong("BlinkStartTick", blinkStartTick);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        apiLevel = tag.getInt("ApiLevel");
        lastFeedTick = tag.getLong("LastFeedTick");
        growing = tag.getBoolean("Growing");
        shrinking = tag.getBoolean("Shrinking");
        animationComplete = tag.getBoolean("AnimComplete");
        if (tag.hasUUID("Owner")) {
            ownerUUID = tag.getUUID("Owner");
        }
        if (tag.contains("CustomName", 8)) {
            customName = parseCustomNameSafe(tag.getString("CustomName"), registries);
        }
        if (tag.contains("AnimState", 8)) {
            try {
                animState = ApiAnimationState.valueOf(tag.getString("AnimState"));
            } catch (IllegalArgumentException e) {
                animState = ApiAnimationState.IDLE;
            }
        }
        animStartTick = tag.getLong("AnimStartTick");
        nextBlinkTick = tag.getLong("NextBlinkTick");
        isBlinking = tag.getBoolean("IsBlinking");
        blinkStartTick = tag.getLong("BlinkStartTick");
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
