/**
 * ============================================================
 * [MagicBeeEntity.java]
 * Description: Entité abeille magique avec système de gènes et comportements
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BeeGeneData         | Stockage genes       | Gestion des genes de l'abeille |
 * | GeneRegistry        | Registre genes       | Lookup des genes               |
 * | BeeBehaviorConfig   | Configuration        | Parametres de comportement     |
 * | BeeBehaviorManager  | Gestionnaire config  | Recuperation config par espece |
 * | MagicHiveBlockEntity| Ruche                | Notification et interaction    |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - MagicBeeItem.java: Création/capture d'abeilles
 * - MagicHiveBlockEntity.java: Gestion des abeilles
 * - Goals: AI de l'abeille
 *
 * ============================================================
 */
package com.chapeau.apica.common.entity.bee;

import com.chapeau.apica.common.block.hive.IHiveBeeHost;
import com.chapeau.apica.common.entity.bee.goal.BeeRevengeGoal;
import com.chapeau.apica.common.entity.bee.goal.ForagingBehaviorGoal;
import com.chapeau.apica.common.entity.bee.goal.ReturnToHiveWhenLowHealthGoal;
import com.chapeau.apica.common.entity.bee.goal.WildBeePatrolGoal;
import com.chapeau.apica.core.behavior.BeeBehaviorConfig;
import com.chapeau.apica.core.behavior.BeeBehaviorManager;
import com.chapeau.apica.core.gene.BeeGeneData;
import com.chapeau.apica.core.gene.Gene;
import com.chapeau.apica.core.gene.GeneCategory;
import com.chapeau.apica.core.gene.GeneRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.chapeau.apica.common.entity.bee.pathfinding.NoOpNavigation;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class MagicBeeEntity extends Bee {

    // --- Entity Data Accessors ---
    private static final EntityDataAccessor<String> DATA_SPECIES = SynchedEntityData.defineId(
            MagicBeeEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_ENVIRONMENT = SynchedEntityData.defineId(
            MagicBeeEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_FLOWER = SynchedEntityData.defineId(
            MagicBeeEntity.class, EntityDataSerializers.STRING);

    // Nouveaux EntityData pour le comportement
    private static final EntityDataAccessor<Boolean> DATA_POLLINATED = SynchedEntityData.defineId(
            MagicBeeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ENRAGED = SynchedEntityData.defineId(
            MagicBeeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_RETURNING = SynchedEntityData.defineId(
            MagicBeeEntity.class, EntityDataSerializers.BOOLEAN);

    // Debug destination (synchronisé pour affichage client)
    private static final EntityDataAccessor<BlockPos> DATA_DEBUG_DESTINATION = SynchedEntityData.defineId(
            MagicBeeEntity.class, EntityDataSerializers.BLOCK_POS);

    // Debug path (synchronisé pour affichage client - chemin Theta* complet)
    private static final EntityDataAccessor<CompoundTag> DATA_DEBUG_PATH = SynchedEntityData.defineId(
            MagicBeeEntity.class, EntityDataSerializers.COMPOUND_TAG);

    // Debug vanilla path (chemin FlyingPathNavigation pour comparaison)
    private static final EntityDataAccessor<CompoundTag> DATA_DEBUG_VANILLA_PATH = SynchedEntityData.defineId(
            MagicBeeEntity.class, EntityDataSerializers.COMPOUND_TAG);

    // --- Gene Data ---
    private final BeeGeneData geneData = new BeeGeneData();

    // --- Wild Bee Nest ---
    @Nullable
    private BlockPos homeNestPos = null;

    // --- Hive Assignment ---
    @Nullable
    private BlockPos assignedHivePos = null;
    private int assignedSlot = -1;

    // --- State Flags ---
    private boolean notifiedHiveOfRemoval = false;

    // --- Health tracking for items ---
    private float storedHealth = -1; // -1 = use max health

    // --- Cached Config ---
    private BeeBehaviorConfig cachedConfig = null;
    private String cachedConfigSpecies = null;

    // --- Enraged Timer ---
    public static final int DEFAULT_ENRAGED_DURATION = 200; // 10 secondes
    private int enragedTimer = 0;

    // --- Hive Ping Timer (transient, not saved) ---
    private int hivePingTimer = 0;

    // --- Orphan Timer (transient): bee with no hive and no nest is discarded after 2400 ticks ---
    private static final int ORPHAN_ENTITY_TIMEOUT = 2400; // 120 secondes
    private int orphanTicks = 0;

    public MagicBeeEntity(EntityType<? extends Bee> entityType, Level level) {
        super(entityType, level);
        // Forcer le contrôle de vol et désactiver la gravité
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        // NoOp: les goals utilisent setDeltaMovement() + Theta* custom,
        //return new NoOpNavigation(this, level);
        return new FlyingPathNavigation(this, level);
    }

    @Override
    public boolean onClimbable() {
        // Les abeilles ne grimpent pas
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
        // Pas de dégâts de chute pour les abeilles volantes
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        // Pas de dégâts de chute
        return false;
    }

    @Override
    public void travel(Vec3 travelVector) {
        // Override travel pour forcer le vol
        if (this.isControlledByLocalInstance()) {
            if (this.isInWater()) {
                this.moveRelative(0.02F, travelVector);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.8));
            } else if (this.isInLava()) {
                this.moveRelative(0.02F, travelVector);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
            } else {
                // Vol normal - pas de friction au sol
                float friction = 0.91F;
                this.moveRelative(this.getSpeed(), travelVector);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(friction));
            }
        }
        this.calculateEntityAnimation(false);
    }

    @Override
    public boolean isFlying() {
        // Toujours considéré en vol
        return true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SPECIES, "meadow");
        builder.define(DATA_ENVIRONMENT, "normal");
        builder.define(DATA_FLOWER, "flowers");
        builder.define(DATA_POLLINATED, false);
        builder.define(DATA_ENRAGED, false);
        builder.define(DATA_RETURNING, false);
        builder.define(DATA_DEBUG_DESTINATION, BlockPos.ZERO);
        builder.define(DATA_DEBUG_PATH, new CompoundTag());
        builder.define(DATA_DEBUG_VANILLA_PATH, new CompoundTag());
    }

    @Override
    protected void registerGoals() {
        // Appeler super pour initialiser les champs internes de Bee (beePollinateGoal, etc.)
        // requis par BeeLookControl, puis vider les goal selectors et reenregistrer les notres.
        super.registerGoals();
        this.goalSelector.removeAllGoals(goal -> true);
        this.targetSelector.removeAllGoals(goal -> true);

        this.goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));

        // Priorité 1: Fuite si vie < 30%
        this.goalSelector.addGoal(1, new ReturnToHiveWhenLowHealthGoal(this));

        // Priorité 2: Vengeance si attaqué
        this.targetSelector.addGoal(2, new BeeRevengeGoal(this));

        // Priorité 3: Comportement de butinage (abeilles assignées à une ruche)
        this.goalSelector.addGoal(3, new ForagingBehaviorGoal(this));

        // Priorité 3: Patrouille sauvage (abeilles de nids naturels)
        this.goalSelector.addGoal(3, new WildBeePatrolGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Bee.createAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    // --- Gene System ---

    public BeeGeneData getGeneData() {
        return geneData;
    }

    public void setGene(Gene gene) {
        if (geneData.setGene(gene)) {
            syncGeneToData(gene);
        }
    }

    private void syncGeneToData(Gene gene) {
        GeneCategory cat = gene.getCategory();
        if (cat == GeneCategory.SPECIES) {
            this.entityData.set(DATA_SPECIES, gene.getId());
        } else if (cat == GeneCategory.ENVIRONMENT) {
            this.entityData.set(DATA_ENVIRONMENT, gene.getId());
        } else if (cat == GeneCategory.FLOWER) {
            this.entityData.set(DATA_FLOWER, gene.getId());
        }
    }

    private void loadGenesFromData() {
        Gene species = GeneRegistry.getGene(GeneCategory.SPECIES, entityData.get(DATA_SPECIES));
        Gene environment = GeneRegistry.getGene(GeneCategory.ENVIRONMENT, entityData.get(DATA_ENVIRONMENT));
        Gene flower = GeneRegistry.getGene(GeneCategory.FLOWER, entityData.get(DATA_FLOWER));

        if (species != null) geneData.setGene(species);
        if (environment != null) geneData.setGene(environment);
        if (flower != null) geneData.setGene(flower);
    }

    /**
     * Récupère l'ID de l'espèce actuelle.
     */
    public String getSpeciesId() {
        return entityData.get(DATA_SPECIES);
    }

    // --- Behavior Config ---

    /**
     * Récupère la configuration de comportement pour cette espèce (avec cache).
     */
    public BeeBehaviorConfig getBehaviorConfig() {
        String currentSpecies = getSpeciesId();
        if (cachedConfig == null || !currentSpecies.equals(cachedConfigSpecies)) {
            cachedConfig = BeeBehaviorManager.getConfig(currentSpecies);
            cachedConfigSpecies = currentSpecies;
        }
        return cachedConfig;
    }

    /**
     * Invalide le cache de configuration.
     */
    public void invalidateConfigCache() {
        cachedConfig = null;
        cachedConfigSpecies = null;
    }

    // --- Pollination State ---

    public boolean isPollinated() {
        return entityData.get(DATA_POLLINATED);
    }

    public void setPollinated(boolean pollinated) {
        entityData.set(DATA_POLLINATED, pollinated);
    }

    // --- Combat State (Timer-based) ---

    /**
     * Vérifie si l'abeille est enragée (timer > 0).
     */
    public boolean isEnraged() {
        return enragedTimer > 0;
    }

    /**
     * Active l'état enragé pour la durée par défaut.
     */
    public void setEnraged(boolean enraged) {
        if (enraged) {
            enragedTimer = DEFAULT_ENRAGED_DURATION;
            entityData.set(DATA_ENRAGED, true);
        } else {
            enragedTimer = 0;
            entityData.set(DATA_ENRAGED, false);
        }
    }

    /**
     * Active l'état enragé pour une durée spécifique.
     */
    public void setEnragedFor(int ticks) {
        enragedTimer = ticks;
        entityData.set(DATA_ENRAGED, ticks > 0);
    }

    /**
     * Retourne le temps restant d'enragement.
     */
    public int getEnragedTimer() {
        return enragedTimer;
    }

    /**
     * Décrémente le timer enragé et met à jour l'état.
     */
    private void tickEnragedTimer() {
        if (enragedTimer > 0) {
            enragedTimer--;
            if (enragedTimer <= 0) {
                entityData.set(DATA_ENRAGED, false);
            }
        }
    }

    /**
     * Ping la ruche assignee toutes les 40 ticks (2 secondes) pour valider le UUID.
     * Si la ruche repond que ce bee est un doublon, se detruit silencieusement.
     */
    private void tickHivePing() {
        if (!hasAssignedHive()) return;

        hivePingTimer++;
        if (hivePingTimer < 40) return;
        hivePingTimer = 0;

        if (assignedHivePos == null) return;
        if (!(level().getBlockEntity(assignedHivePos) instanceof IHiveBeeHost hive)) return;

        boolean shouldSurvive = hive.handleBeePing(this);
        if (!shouldSurvive) {
            notifiedHiveOfRemoval = true;
            discard();
        }
    }

    /**
     * Si l'abeille n'a ni ruche assignee ni nid d'origine, elle est orpheline.
     * Apres 2400 ticks (120s), elle se supprime pour eviter les entites perdues.
     */
    private void tickOrphanCleanup() {
        if (!hasAssignedHive() && !hasHomeNest()) {
            orphanTicks++;
            if (orphanTicks >= ORPHAN_ENTITY_TIMEOUT) {
                discard();
            }
        } else {
            orphanTicks = 0;
        }
    }

    // --- Returning State ---

    public boolean isReturning() {
        return entityData.get(DATA_RETURNING);
    }

    public void setReturning(boolean returning) {
        entityData.set(DATA_RETURNING, returning);
    }

    /**
     * Pourcentage de vie actuel (0.0 à 1.0).
     */
    public float getHealthPercentage() {
        return getHealth() / getMaxHealth();
    }

    /**
     * Vérifie si l'abeille doit fuir (vie - 30%).
     */
    public boolean shouldFlee() {
        return getHealthPercentage() < 0.3f;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);

        if (hurt && !level().isClientSide()) {
            // Devenir enragée si attaquée par une entité vivante
            Entity attacker = source.getEntity();
            if (attacker instanceof LivingEntity) {
                setEnraged(true);
            }
        }

        return hurt;
    }

    /**
     * Calcule la vitesse effective de vol.
     */
    public double getEffectiveFlyingSpeed() {
        BeeBehaviorConfig config = getBehaviorConfig();
        double baseSpeed = config.getFlyingSpeed();

        if (isEnraged()) {
            baseSpeed = config.getEnragedFlyingSpeed();
        }

        return baseSpeed;
    }

    @Override
    public void tick() {
        super.tick();

        // Toujours forcer le vol (au cas où)
        if (!this.isNoGravity()) {
            this.setNoGravity(true);
        }

        // Empêcher de rester au sol - si sur le sol, remonter légèrement
        if (this.onGround() && !level().isClientSide()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0, 0.05, 0));
        }

        if (!level().isClientSide()) {
            // Tick le timer enragé chaque tick
            tickEnragedTimer();
            // Ping la ruche pour validation UUID
            tickHivePing();
            // Orphan entity cleanup: bee without hive or nest is discarded after timeout
            tickOrphanCleanup();
        }

        // Apply gene behaviors
        for (Gene gene : geneData.getAllGenes()) {
            gene.applyBehavior(this);
        }
    }

    // --- Stored Health (for item capture) ---

    public float getStoredHealth() {
        return storedHealth >= 0 ? storedHealth : getHealth();
    }

    public void setStoredHealth(float health) {
        this.storedHealth = health;
        if (health > 0 && health <= getMaxHealth()) {
            setHealth(health);
        }
    }

    // --- Removal / Death ---

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide() && hasAssignedHive() && !notifiedHiveOfRemoval) {
            notifiedHiveOfRemoval = true;
            notifyHiveOfDeath();
        }
        super.remove(reason);
    }

    private void notifyHiveOfDeath() {
        if (assignedHivePos == null) return;

        if (level().getBlockEntity(assignedHivePos) instanceof IHiveBeeHost hive) {
            hive.onBeeKilled(getUUID());
        }
    }

    public void markAsEnteredHive() {
        notifiedHiveOfRemoval = true;
    }

    // --- Hive Assignment ---

    public boolean hasAssignedHive() {
        return assignedHivePos != null && assignedSlot >= 0;
    }

    @Nullable
    public BlockPos getAssignedHivePos() {
        return assignedHivePos;
    }

    public int getAssignedSlot() {
        return assignedSlot;
    }

    public void setAssignedHive(@Nullable BlockPos hivePos, int slot) {
        this.assignedHivePos = hivePos;
        this.assignedSlot = slot;
    }

    public void clearAssignedHive() {
        this.assignedHivePos = null;
        this.assignedSlot = -1;
    }

    // --- Wild Bee Nest ---

    @Nullable
    public BlockPos getHomeNestPos() {
        return homeNestPos;
    }

    public void setHomeNestPos(@Nullable BlockPos pos) {
        this.homeNestPos = pos;
    }

    public boolean hasHomeNest() {
        return homeNestPos != null;
    }

    // --- Debug Destination (pour affichage visuel) ---

    /**
     * Définit la destination de debug (synchronisée au client).
     * Appelé par les goals pour indiquer où l'abeille se dirige.
     * Calcule aussi le chemin vanilla pour comparaison debug.
     */
    public void setDebugDestination(@Nullable BlockPos pos) {
        entityData.set(DATA_DEBUG_DESTINATION, pos != null ? pos : BlockPos.ZERO);

        // Calculer le chemin vanilla pour debug
        if (pos != null && !level().isClientSide()) {
            computeAndSetVanillaDebugPath(pos);
        } else if (pos == null) {
            setDebugVanillaPath(null);
        }
    }

    /**
     * Récupère la destination de debug actuelle.
     * Retourne null si aucune destination (BlockPos.ZERO).
     */
    @Nullable
    public BlockPos getDebugDestination() {
        BlockPos pos = entityData.get(DATA_DEBUG_DESTINATION);
        return pos.equals(BlockPos.ZERO) ? null : pos;
    }

    /**
     * Vérifie si l'abeille a une destination de debug.
     */
    public boolean hasDebugDestination() {
        return !entityData.get(DATA_DEBUG_DESTINATION).equals(BlockPos.ZERO);
    }

    /**
     * Efface la destination de debug.
     */
    public void clearDebugDestination() {
        entityData.set(DATA_DEBUG_DESTINATION, BlockPos.ZERO);
    }

    // --- Debug Path (chemin Theta* complet) ---

    /**
     * Définit le chemin de debug (synchronisé au client).
     * Encode les positions comme long[] dans un CompoundTag.
     */
    public void setDebugPath(@Nullable List<BlockPos> path) {
        CompoundTag tag = new CompoundTag();
        if (path != null && !path.isEmpty()) {
            long[] longs = new long[path.size()];
            for (int i = 0; i < path.size(); i++) {
                longs[i] = path.get(i).asLong();
            }
            tag.put("Path", new LongArrayTag(longs));
        }
        entityData.set(DATA_DEBUG_PATH, tag);
    }

    /**
     * Récupère le chemin de debug.
     */
    public List<BlockPos> getDebugPath() {
        CompoundTag tag = entityData.get(DATA_DEBUG_PATH);
        if (tag.contains("Path")) {
            long[] longs = tag.getLongArray("Path");
            List<BlockPos> path = new ArrayList<>(longs.length);
            for (long l : longs) {
                path.add(BlockPos.of(l));
            }
            return path;
        }
        return List.of();
    }

    // --- Debug Vanilla Path (FlyingPathNavigation pour comparaison) ---

    /**
     * Définit le chemin vanilla de debug (synchronisé au client).
     */
    public void setDebugVanillaPath(@Nullable List<BlockPos> path) {
        CompoundTag tag = new CompoundTag();
        if (path != null && !path.isEmpty()) {
            long[] longs = new long[path.size()];
            for (int i = 0; i < path.size(); i++) {
                longs[i] = path.get(i).asLong();
            }
            tag.put("Path", new LongArrayTag(longs));
        }
        entityData.set(DATA_DEBUG_VANILLA_PATH, tag);
    }

    /**
     * Récupère le chemin vanilla de debug.
     */
    public List<BlockPos> getDebugVanillaPath() {
        CompoundTag tag = entityData.get(DATA_DEBUG_VANILLA_PATH);
        if (tag.contains("Path")) {
            long[] longs = tag.getLongArray("Path");
            List<BlockPos> path = new ArrayList<>(longs.length);
            for (long l : longs) {
                path.add(BlockPos.of(l));
            }
            return path;
        }
        return List.of();
    }

    // --- Lazy Vanilla Navigation (pour calcul de debug path) ---
    private FlyingPathNavigation debugVanillaNavigation = null;

    /**
     * Calcule et stocke le chemin vanilla vers une destination.
     * Utilise FlyingPathNavigation pour comparaison avec Theta*.
     */
    public void computeAndSetVanillaDebugPath(BlockPos destination) {
        if (level().isClientSide()) return;

        // Lazy init du FlyingPathNavigation
        if (debugVanillaNavigation == null) {
            debugVanillaNavigation = new FlyingPathNavigation(this, level());
        }

        Path path = debugVanillaNavigation.createPath(destination, 0);
        if (path != null && path.getNodeCount() > 0) {
            List<BlockPos> positions = new ArrayList<>();
            for (int i = 0; i < path.getNodeCount(); i++) {
                Node node = path.getNode(i);
                positions.add(new BlockPos(node.x, node.y, node.z));
            }
            setDebugVanillaPath(positions);
        } else {
            setDebugVanillaPath(null);
        }
    }

    // --- NBT ---

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("GeneData", geneData.save());

        if (assignedHivePos != null) {
            tag.putInt("HiveX", assignedHivePos.getX());
            tag.putInt("HiveY", assignedHivePos.getY());
            tag.putInt("HiveZ", assignedHivePos.getZ());
            tag.putInt("HiveSlot", assignedSlot);
        }

        if (homeNestPos != null) {
            tag.putInt("NestX", homeNestPos.getX());
            tag.putInt("NestY", homeNestPos.getY());
            tag.putInt("NestZ", homeNestPos.getZ());
        }

        // Sauvegarder l'état de comportement
        tag.putBoolean("Pollinated", isPollinated());
        tag.putInt("EnragedTimer", enragedTimer);
        tag.putFloat("StoredHealth", getHealth());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("GeneData")) {
            geneData.load(tag.getCompound("GeneData"));
            for (Gene gene : geneData.getAllGenes()) {
                syncGeneToData(gene);
            }
        }

        if (tag.contains("HiveX")) {
            assignedHivePos = new BlockPos(tag.getInt("HiveX"), tag.getInt("HiveY"), tag.getInt("HiveZ"));
            assignedSlot = tag.getInt("HiveSlot");
        }

        if (tag.contains("NestX")) {
            homeNestPos = new BlockPos(tag.getInt("NestX"), tag.getInt("NestY"), tag.getInt("NestZ"));
        }

        if (tag.contains("Pollinated")) {
            setPollinated(tag.getBoolean("Pollinated"));
        }
        if (tag.contains("EnragedTimer")) {
            enragedTimer = tag.getInt("EnragedTimer");
            entityData.set(DATA_ENRAGED, enragedTimer > 0);
        } else if (tag.contains("Enraged")) {
            // Backwards compatibility with old saves
            if (tag.getBoolean("Enraged")) {
                enragedTimer = DEFAULT_ENRAGED_DURATION;
                entityData.set(DATA_ENRAGED, true);
            }
        }
        if (tag.contains("StoredHealth")) {
            storedHealth = tag.getFloat("StoredHealth");
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key == DATA_SPECIES || key == DATA_ENVIRONMENT || key == DATA_FLOWER) {
            loadGenesFromData();
        }
    }

}