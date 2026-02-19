/**
 * ============================================================
 * [InteractionMarkerEntity.java]
 * Description: Entité invisible réutilisable servant de point d'interaction cliquable
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | InteractionMarkerTypes  | Registre de types    | Validation et handler          |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - InteractionMarkerManager.java: Spawn/despawn
 * - AssemblyTableBlockEntity.java: Via le manager
 * - Tout système nécessitant un point d'interaction en monde
 *
 * ============================================================
 */
package com.chapeau.apica.core.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Entité invisible, sans IA, sans gravité, sans persistance.
 * Sert de point d'interaction cliquable dans le monde, ancré à un bloc.
 * Le comportement est entièrement délégué au MarkerType enregistré dans InteractionMarkerTypes.
 */
public class InteractionMarkerEntity extends Mob {

    private static final EntityDataAccessor<BlockPos> DATA_ANCHOR_POS =
            SynchedEntityData.defineId(InteractionMarkerEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<String> DATA_MARKER_TYPE =
            SynchedEntityData.defineId(InteractionMarkerEntity.class, EntityDataSerializers.STRING);

    /** Intervalle de vérification de validité (en ticks). */
    private static final int VALIDITY_CHECK_INTERVAL = 20;
    private int validityCheckTimer = 0;

    public InteractionMarkerEntity(EntityType<? extends InteractionMarkerEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setInvisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ANCHOR_POS, BlockPos.ZERO);
        builder.define(DATA_MARKER_TYPE, "");
    }

    // --- Getters / Setters ---

    public BlockPos getAnchorPos() {
        return this.entityData.get(DATA_ANCHOR_POS);
    }

    public void setAnchorPos(BlockPos pos) {
        this.entityData.set(DATA_ANCHOR_POS, pos);
    }

    public String getMarkerType() {
        return this.entityData.get(DATA_MARKER_TYPE);
    }

    public void setMarkerType(String type) {
        this.entityData.set(DATA_MARKER_TYPE, type);
    }

    // --- Comportement ---

    @Override
    protected void registerGoals() {
        // Pas d'IA
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean skipAttackInteraction(net.minecraft.world.entity.Entity attacker) {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            validityCheckTimer++;
            if (validityCheckTimer >= VALIDITY_CHECK_INTERVAL) {
                validityCheckTimer = 0;
                if (!isStillValid()) {
                    this.discard();
                }
            }
        }
    }

    private boolean isStillValid() {
        InteractionMarkerTypes.MarkerType type = InteractionMarkerTypes.get(getMarkerType());
        if (type == null) return false;
        return type.validityCheck().test(this.level(), getAnchorPos());
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionMarkerTypes.MarkerType type = InteractionMarkerTypes.get(getMarkerType());
        if (type != null) {
            return type.handler().handle(this, player, hand);
        }
        return InteractionResult.PASS;
    }

    // --- Pas de sauvegarde (noSave sur le EntityType) ---

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("AnchorX", getAnchorPos().getX());
        tag.putInt("AnchorY", getAnchorPos().getY());
        tag.putInt("AnchorZ", getAnchorPos().getZ());
        tag.putString("MarkerType", getMarkerType());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("AnchorX")) {
            setAnchorPos(new BlockPos(tag.getInt("AnchorX"), tag.getInt("AnchorY"), tag.getInt("AnchorZ")));
        }
        if (tag.contains("MarkerType")) {
            setMarkerType(tag.getString("MarkerType"));
        }
    }
}
