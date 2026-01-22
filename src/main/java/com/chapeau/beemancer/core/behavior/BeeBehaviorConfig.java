/**
 * ============================================================
 * [BeeBehaviorConfig.java]
 * Description: Configuration des paramètres de comportement par espèce
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BeeBehaviorType     | Type de comportement | Définit FORAGER/HARVESTER      |
 * | LootEntry           | Entrées de loot      | Liste des loots pollinisation  |
 * | SpeedThreshold      | Seuils de vitesse    | Liste pour récolteuses         |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - BeeBehaviorManager.java: Chargement depuis JSON
 * - MagicBeeEntity.java: Récupération des paramètres
 * - MagicHiveBlockEntity.java: Cooldowns et loot
 * - HarvestingBehaviorGoal.java: Calcul vitesse effective
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.behavior;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Configuration complète du comportement d'une espèce d'abeille.
 */
public class BeeBehaviorConfig {
    
    // Type de comportement
    private BeeBehaviorType behaviorType = BeeBehaviorType.FORAGER;
    
    // Paramètres communs
    private double flyingSpeed = 0.6;
    private double enragedFlyingSpeed = 0.9;
    private double health = 10.0;
    private double attackDamage = 2.0;
    private int regenerationRate = 1; // points de vie par seconde
    private boolean aggressiveToPlayers = false;
    private boolean aggressiveToPassiveMobs = false;
    private boolean aggressiveToHostileMobs = false;
    private int restCooldownMin = 200; // ticks (10 secondes)
    private int restCooldownMax = 600; // ticks (30 secondes)
    private int areaOfEffect = 16; // rayon en blocs
    
    // Paramètres butineuse
    private int foragingDuration = 100; // ticks (5 secondes)
    
    // Paramètres récolteuse
    private int harvestingDuration = 200; // ticks (10 secondes)
    private int inventorySize = 27; // grande capacité par défaut
    private int returnThreshold = 64; // nombre d'items avant retour
    private final List<SpeedThreshold> speedThresholds = new ArrayList<>();
    
    // Loot de pollinisation
    private final List<LootEntry> pollinationLoot = new ArrayList<>();
    
    // --- Getters ---
    
    public BeeBehaviorType getBehaviorType() {
        return behaviorType;
    }
    
    public double getFlyingSpeed() {
        return flyingSpeed;
    }
    
    public double getEnragedFlyingSpeed() {
        return enragedFlyingSpeed;
    }
    
    public double getHealth() {
        return health;
    }
    
    public double getAttackDamage() {
        return attackDamage;
    }
    
    public int getRegenerationRate() {
        return regenerationRate;
    }
    
    public boolean isAggressiveToPlayers() {
        return aggressiveToPlayers;
    }
    
    public boolean isAggressiveToPassiveMobs() {
        return aggressiveToPassiveMobs;
    }
    
    public boolean isAggressiveToHostileMobs() {
        return aggressiveToHostileMobs;
    }
    
    public int getRestCooldownMin() {
        return restCooldownMin;
    }
    
    public int getRestCooldownMax() {
        return restCooldownMax;
    }
    
    public int getAreaOfEffect() {
        return areaOfEffect;
    }
    
    public int getForagingDuration() {
        return foragingDuration;
    }
    
    public int getHarvestingDuration() {
        return harvestingDuration;
    }
    
    public int getInventorySize() {
        return inventorySize;
    }
    
    public int getReturnThreshold() {
        return returnThreshold;
    }
    
    public List<SpeedThreshold> getSpeedThresholds() {
        return speedThresholds;
    }
    
    public List<LootEntry> getPollinationLoot() {
        return pollinationLoot;
    }
    
    // --- Setters (pour le chargement JSON) ---
    
    public void setBehaviorType(BeeBehaviorType type) {
        this.behaviorType = type;
    }
    
    public void setFlyingSpeed(double speed) {
        this.flyingSpeed = speed;
    }
    
    public void setEnragedFlyingSpeed(double speed) {
        this.enragedFlyingSpeed = speed;
    }
    
    public void setHealth(double health) {
        this.health = health;
    }
    
    public void setAttackDamage(double damage) {
        this.attackDamage = damage;
    }
    
    public void setRegenerationRate(int rate) {
        this.regenerationRate = rate;
    }
    
    public void setAggressiveToPlayers(boolean aggressive) {
        this.aggressiveToPlayers = aggressive;
    }
    
    public void setAggressiveToPassiveMobs(boolean aggressive) {
        this.aggressiveToPassiveMobs = aggressive;
    }
    
    public void setAggressiveToHostileMobs(boolean aggressive) {
        this.aggressiveToHostileMobs = aggressive;
    }
    
    public void setRestCooldownMin(int min) {
        this.restCooldownMin = min;
    }
    
    public void setRestCooldownMax(int max) {
        this.restCooldownMax = max;
    }
    
    public void setAreaOfEffect(int radius) {
        this.areaOfEffect = radius;
    }
    
    public void setForagingDuration(int duration) {
        this.foragingDuration = duration;
    }
    
    public void setHarvestingDuration(int duration) {
        this.harvestingDuration = duration;
    }
    
    public void setInventorySize(int size) {
        this.inventorySize = size;
    }
    
    public void setReturnThreshold(int threshold) {
        this.returnThreshold = threshold;
    }
    
    public void addLootEntry(LootEntry entry) {
        if (entry.isValid()) {
            this.pollinationLoot.add(entry);
        }
    }
    
    public void addSpeedThreshold(SpeedThreshold threshold) {
        if (threshold.isValid()) {
            this.speedThresholds.add(threshold);
            // Trier par itemCount croissant pour faciliter la recherche
            this.speedThresholds.sort(Comparator.comparingInt(SpeedThreshold::itemCount));
        }
    }
    
    // --- Méthodes utilitaires ---
    
    /**
     * Génère un cooldown aléatoire entre min et max.
     */
    public int getRandomRestCooldown(RandomSource random) {
        if (restCooldownMax <= restCooldownMin) {
            return restCooldownMin;
        }
        return restCooldownMin + random.nextInt(restCooldownMax - restCooldownMin + 1);
    }
    
    /**
     * Calcule la vitesse effective basée sur le nombre d'items transportés.
     * Utilise le seuil le plus élevé que itemCount dépasse.
     * 
     * @param itemCount Nombre d'items dans l'inventaire
     * @return Multiplicateur de vitesse (1.0 si aucun seuil applicable)
     */
    public double getEffectiveSpeedMultiplier(int itemCount) {
        if (speedThresholds.isEmpty()) {
            return 1.0;
        }
        
        double multiplier = 1.0;
        for (SpeedThreshold threshold : speedThresholds) {
            if (itemCount >= threshold.itemCount()) {
                multiplier = threshold.speedMultiplier();
            } else {
                break; // Liste triée, on peut arrêter
            }
        }
        return multiplier;
    }
    
    /**
     * Génère le loot de pollinisation basé sur les probabilités.
     */
    public List<ItemStack> rollPollinationLoot(RandomSource random) {
        List<ItemStack> result = new ArrayList<>();
        
        for (LootEntry entry : pollinationLoot) {
            // Roll pour la chance
            if (random.nextInt(100) < entry.chance()) {
                // Résoudre l'item
                ResourceLocation itemId = ResourceLocation.tryParse(entry.itemId());
                if (itemId != null) {
                    Item item = BuiltInRegistries.ITEM.get(itemId);
                    if (item != null) {
                        // Quantité aléatoire entre min et max
                        int qty = entry.minQty();
                        if (entry.maxQty() > entry.minQty()) {
                            qty += random.nextInt(entry.maxQty() - entry.minQty() + 1);
                        }
                        if (qty > 0) {
                            result.add(new ItemStack(item, qty));
                        }
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * Crée une copie avec les valeurs par défaut fusionnées.
     */
    public static BeeBehaviorConfig createWithDefaults(BeeBehaviorConfig defaults) {
        BeeBehaviorConfig config = new BeeBehaviorConfig();
        if (defaults != null) {
            config.behaviorType = defaults.behaviorType;
            config.flyingSpeed = defaults.flyingSpeed;
            config.enragedFlyingSpeed = defaults.enragedFlyingSpeed;
            config.health = defaults.health;
            config.attackDamage = defaults.attackDamage;
            config.regenerationRate = defaults.regenerationRate;
            config.aggressiveToPlayers = defaults.aggressiveToPlayers;
            config.aggressiveToPassiveMobs = defaults.aggressiveToPassiveMobs;
            config.aggressiveToHostileMobs = defaults.aggressiveToHostileMobs;
            config.restCooldownMin = defaults.restCooldownMin;
            config.restCooldownMax = defaults.restCooldownMax;
            config.areaOfEffect = defaults.areaOfEffect;
            config.foragingDuration = defaults.foragingDuration;
            config.harvestingDuration = defaults.harvestingDuration;
            config.inventorySize = defaults.inventorySize;
            config.returnThreshold = defaults.returnThreshold;
            config.speedThresholds.addAll(defaults.speedThresholds);
        }
        return config;
    }
}
