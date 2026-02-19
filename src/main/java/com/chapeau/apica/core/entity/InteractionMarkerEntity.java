/**
 * ============================================================
 * [InteractionMarkerEntity.java]
 * Description: Entite invisible reutilisable servant de point d'interaction cliquable
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | InteractionMarkerTypes  | Registre de types    | Validation et handler          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - InteractionMarkerManager.java: Spawn/despawn
 * - AssemblyTableBlockEntity.java: Via le manager (ancrage bloc)
 * - HoverbikeEntity.java: Via le manager (ancrage entite)
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Entite invisible, sans IA, sans gravite, sans persistance.
 * Sert de point d'interaction cliquable dans le monde.
 * Peut etre ancree a un bloc (BlockPos) ou a une entite (entity ID).
 * Le comportement est entierement delegue au MarkerType enregistre dans InteractionMarkerTypes.
 */
public class InteractionMarkerEntity extends Mob {

    private static final EntityDataAccessor<BlockPos> DATA_ANCHOR_POS =
            SynchedEntityData.defineId(InteractionMarkerEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<String> DATA_MARKER_TYPE =
            SynchedEntityData.defineId(InteractionMarkerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_ANCHOR_ENTITY_ID =
            SynchedEntityData.defineId(InteractionMarkerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PART_ORDINAL =
            SynchedEntityData.defineId(InteractionMarkerEntity.class, EntityDataSerializers.INT);

    /** -1 = ancrage bloc, sinon = entity ID */
    private static final int NO_ENTITY_ANCHOR = -1;

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
        builder.define(DATA_ANCHOR_ENTITY_ID, NO_ENTITY_ANCHOR);
        builder.define(DATA_PART_ORDINAL, -1);
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

    public int getAnchorEntityId() {
        return this.entityData.get(DATA_ANCHOR_ENTITY_ID);
    }

    public void setAnchorEntityId(int entityId) {
        this.entityData.set(DATA_ANCHOR_ENTITY_ID, entityId);
    }

    public boolean isEntityAnchored() {
        return getAnchorEntityId() != NO_ENTITY_ANCHOR;
    }

    public int getPartOrdinal() {
        return this.entityData.get(DATA_PART_ORDINAL);
    }

    public void setPartOrdinal(int ordinal) {
        this.entityData.set(DATA_PART_ORDINAL, ordinal);
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
    public boolean skipAttackInteraction(Entity attacker) {
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

        if (isEntityAnchored()) {
            Entity anchor = this.level().getEntity(getAnchorEntityId());
            return anchor != null && anchor.isAlive();
        }
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
        tag.putInt("AnchorEntityId", getAnchorEntityId());
        tag.putInt("PartOrdinal", getPartOrdinal());
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
        if (tag.contains("AnchorEntityId")) {
            setAnchorEntityId(tag.getInt("AnchorEntityId"));
        }
        if (tag.contains("PartOrdinal")) {
            setPartOrdinal(tag.getInt("PartOrdinal"));
        }
    }
}
