/**
 * ============================================================
 * [BeeBehaviorConfig.java]
 * Description: Configuration des parametres de comportement par espece
 * ============================================================
 */
package com.chapeau.beemancer.core.behavior;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration complete du comportement d'une espece d'abeille.
 */
public class BeeBehaviorConfig {

    // Parametres communs
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

    // Parametres butineuse
    private int foragingDuration = 100; // ticks (5 secondes)

    // Loot de pollinisation
    private final List<LootEntry> pollinationLoot = new ArrayList<>();

    // --- Getters ---

    public BeeBehaviorType getBehaviorType() {
        return BeeBehaviorType.FORAGER;
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

    public List<LootEntry> getPollinationLoot() {
        return pollinationLoot;
    }

    // --- Setters (pour le chargement JSON) ---

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

    public void addLootEntry(LootEntry entry) {
        if (entry.isValid()) {
            this.pollinationLoot.add(entry);
        }
    }

    // --- Methodes utilitaires ---

    /**
     * Genere un cooldown aleatoire entre min et max.
     */
    public int getRandomRestCooldown(RandomSource random) {
        if (restCooldownMax <= restCooldownMin) {
            return restCooldownMin;
        }
        return restCooldownMin + random.nextInt(restCooldownMax - restCooldownMin + 1);
    }

    /**
     * Genere le loot de pollinisation base sur les probabilites.
     * Supporte les items directs (ex: "minecraft:honeycomb") et les tags (ex: "#minecraft:small_flowers").
     */
    public List<ItemStack> rollPollinationLoot(RandomSource random) {
        List<ItemStack> result = new ArrayList<>();

        for (LootEntry entry : pollinationLoot) {
            // Roll pour la chance
            if (random.nextInt(100) < entry.chance()) {
                ItemStack rolled = resolveItemFromEntry(entry, random);
                if (!rolled.isEmpty()) {
                    result.add(rolled);
                }
            }
        }

        return result;
    }

    /**
     * Resout un ItemStack depuis une LootEntry.
     * Si l'itemId commence par "#", pioche un item aleatoire parmi le tag.
     */
    private ItemStack resolveItemFromEntry(LootEntry entry, RandomSource random) {
        String id = entry.itemId();

        if (id.startsWith("#")) {
            // Tag-based loot: choisir un item aleatoire parmi le tag
            String tagPath = id.substring(1); // Retirer le "#"
            ResourceLocation tagId = ResourceLocation.tryParse(tagPath);
            if (tagId == null) return ItemStack.EMPTY;

            TagKey<Item> tag = TagKey.create(BuiltInRegistries.ITEM.key(), tagId);
            List<Item> tagItems = new ArrayList<>();
            BuiltInRegistries.ITEM.getTag(tag).ifPresent(holders -> {
                holders.forEach(holder -> tagItems.add(holder.value()));
            });

            if (tagItems.isEmpty()) return ItemStack.EMPTY;

            Item chosen = tagItems.get(random.nextInt(tagItems.size()));
            int qty = entry.minQty();
            if (entry.maxQty() > entry.minQty()) {
                qty += random.nextInt(entry.maxQty() - entry.minQty() + 1);
            }
            return qty > 0 ? new ItemStack(chosen, qty) : ItemStack.EMPTY;
        }

        // Item direct
        ResourceLocation itemId = ResourceLocation.tryParse(id);
        if (itemId != null) {
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item != null) {
                int qty = entry.minQty();
                if (entry.maxQty() > entry.minQty()) {
                    qty += random.nextInt(entry.maxQty() - entry.minQty() + 1);
                }
                if (qty > 0) {
                    return new ItemStack(item, qty);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Cree une copie avec les valeurs par defaut fusionnees.
     */
    public static BeeBehaviorConfig createWithDefaults(BeeBehaviorConfig defaults) {
        BeeBehaviorConfig config = new BeeBehaviorConfig();
        if (defaults != null) {
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
        }
        return config;
    }
}
