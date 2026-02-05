/**
 * ============================================================
 * [HiveBeeLifecycleManager.java]
 * Description: Gestion du cycle de vie des abeilles dans la ruche magique
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance               | Raison                  | Utilisation                    |
 * |--------------------------|------------------------|--------------------------------|
 * | MagicHiveBlockEntity     | Parent BlockEntity     | Back-reference                 |
 * | MagicBeeEntity           | Entité abeille         | Spawn/capture/interaction      |
 * | MagicBeeItem             | Item abeille           | Lecture/écriture gènes         |
 * | BeeBehaviorManager       | Config comportement    | Cooldowns, loot, régénération  |
 * | BreedingManager          | Logique reproduction   | Offspring species et gènes     |
 * | BeeLarvaItem             | Item larve             | Création larve offspring        |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - MagicHiveBlockEntity.java (délégation tick, release, entry, breeding)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.hive;

import com.chapeau.beemancer.common.entity.bee.MagicBeeEntity;
import com.chapeau.beemancer.common.item.bee.BeeLarvaItem;
import com.chapeau.beemancer.common.item.bee.MagicBeeItem;
import com.chapeau.beemancer.core.bee.BeeSpeciesManager;
import com.chapeau.beemancer.core.behavior.BeeBehaviorConfig;
import com.chapeau.beemancer.core.behavior.BeeBehaviorManager;
import com.chapeau.beemancer.core.breeding.BreedingManager;
import com.chapeau.beemancer.core.gene.BeeGeneData;
import com.chapeau.beemancer.core.gene.Gene;
import com.chapeau.beemancer.core.gene.GeneCategory;
import com.chapeau.beemancer.core.registry.BeemancerEntities;
import com.chapeau.beemancer.core.registry.BeemancerItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Gère le cycle de vie des abeilles : sortie/entrée de ruche, pollinisation,
 * loot, reproduction automatique et tick des slots.
 * Ne possède aucun champ — opère sur les données du parent via back-reference.
 */
public class HiveBeeLifecycleManager {
    private final MagicHiveBlockEntity parent;

    private static final double BREEDING_CHANCE_ON_ENTRY = 0.15; // 15% quand une abeille rentre

    public HiveBeeLifecycleManager(MagicHiveBlockEntity parent) {
        this.parent = parent;
    }

    // === Static Helper ===

    /**
     * Résout l'ID d'espèce depuis les données génétiques.
     * Utilisé par le parent et le lifecycle manager.
     */
    static String getSpeciesId(BeeGeneData geneData) {
        Gene species = geneData.getGene(GeneCategory.SPECIES);
        return species != null ? species.getId() : "meadow";
    }

    // === Bee Release/Entry ===

    public void releaseBee(int slot) {
        if (slot < 0 || slot >= MagicHiveBlockEntity.BEE_SLOTS) return;

        HiveBeeSlot[] beeSlots = parent.getBeeSlots();
        if (!beeSlots[slot].isInside()) return;
        if (!(parent.getLevel() instanceof ServerLevel serverLevel)) return;

        NonNullList<ItemStack> items = parent.getItems();
        ItemStack beeItem = items.get(slot);
        if (beeItem.isEmpty() || !beeItem.is(BeemancerItems.MAGIC_BEE.get())) return;
        if (!parent.hasFlowersForSlot(slot)) return;

        MagicBeeEntity bee = BeemancerEntities.MAGIC_BEE.get().create(parent.getLevel());
        if (bee == null) return;

        BlockPos spawnPos = parent.getBlockPos().above();
        bee.moveTo(spawnPos.getX() + 0.5, spawnPos.getY() + 1, spawnPos.getZ() + 0.5, 0, 0);
        bee.setDeltaMovement(Vec3.ZERO);

        BeeGeneData geneData = MagicBeeItem.getGeneData(beeItem);
        bee.getGeneData().copyFrom(geneData);
        for (Gene gene : geneData.getAllGenes()) bee.setGene(gene);

        bee.setStoredHealth(beeSlots[slot].getCurrentHealth());
        bee.setAssignedHive(parent.getBlockPos(), slot);
        bee.setPollinated(false);
        bee.setEnraged(false);
        bee.setReturning(false);

        serverLevel.addFreshEntity(bee);

        beeSlots[slot].setState(HiveBeeSlot.State.OUTSIDE);
        beeSlots[slot].setBeeUUID(bee.getUUID());
        parent.setChanged();
    }

    public boolean canBeeEnter(MagicBeeEntity bee) {
        if (!bee.hasAssignedHive() || !parent.getBlockPos().equals(bee.getAssignedHivePos())) return false;
        int slot = bee.getAssignedSlot();
        if (slot < 0 || slot >= MagicHiveBlockEntity.BEE_SLOTS) return false;

        HiveBeeSlot beeSlot = parent.getBeeSlots()[slot];
        if (!beeSlot.isOutside()) return false;

        UUID slotUUID = beeSlot.getBeeUUID();
        return slotUUID != null && slotUUID.equals(bee.getUUID());
    }

    public void addBee(MagicBeeEntity bee) {
        int slot = bee.getAssignedSlot();
        if (slot < 0 || slot >= MagicHiveBlockEntity.BEE_SLOTS) return;

        bee.markAsEnteredHive();

        if (bee.isPollinated()) {
            depositPollinationLoot(bee);
        }

        parent.returnAssignedFlower(slot);

        NonNullList<ItemStack> items = parent.getItems();
        ItemStack beeItem = MagicBeeItem.captureFromEntity(bee);
        items.set(slot, beeItem);

        HiveBeeSlot beeSlot = parent.getBeeSlots()[slot];
        beeSlot.setState(HiveBeeSlot.State.INSIDE);
        beeSlot.setBeeUUID(null);
        beeSlot.setCurrentHealth(bee.getHealth());
        beeSlot.setMaxHealth(bee.getMaxHealth());
        beeSlot.setNeedsHealing(bee.getHealth() < bee.getMaxHealth());
        beeSlot.setCooldown(bee.getBehaviorConfig().getRandomRestCooldown(parent.getLevel().getRandom()));

        bee.setPollinated(false);
        bee.setEnraged(false);
        bee.setReturning(false);

        if (!parent.isAntibreedingMode() && parent.getLevel() != null) {
            tryAutoBreeding(slot, parent.getLevel().getRandom());
        }

        parent.triggerFlowerScan();
        parent.setChanged();
    }

    public void onBeeKilled(UUID beeUUID) {
        HiveBeeSlot[] beeSlots = parent.getBeeSlots();
        NonNullList<ItemStack> items = parent.getItems();

        for (int i = 0; i < MagicHiveBlockEntity.BEE_SLOTS; i++) {
            if (beeUUID.equals(beeSlots[i].getBeeUUID()) && beeSlots[i].isOutside()) {
                parent.returnAssignedFlower(i);
                items.set(i, ItemStack.EMPTY);
                beeSlots[i].clear();
                parent.setChanged();
                return;
            }
        }
    }

    // === UUID Sync Security ===

    /**
     * Gere le ping periodique d'une abeille exterieure.
     * Verifie que le UUID du bee correspond au slot de la ruche.
     *
     * @return true si le bee est valide et doit survivre, false si c'est un doublon a detruire
     */
    public boolean handleBeePing(MagicBeeEntity bee) {
        int slot = bee.getAssignedSlot();
        if (slot < 0 || slot >= MagicHiveBlockEntity.BEE_SLOTS) return false;

        HiveBeeSlot beeSlot = parent.getBeeSlots()[slot];
        UUID beeUUID = bee.getUUID();
        UUID slotUUID = beeSlot.getBeeUUID();

        if (beeSlot.isOutside()) {
            if (slotUUID != null && slotUUID.equals(beeUUID)) {
                return true;
            }
            verifyOutsideBees();
            return false;
        }

        if (beeSlot.isInside()) {
            beeSlot.setState(HiveBeeSlot.State.OUTSIDE);
            beeSlot.setBeeUUID(beeUUID);
            parent.setChanged();
            return true;
        }

        return false;
    }

    /**
     * Verifie toutes les abeilles OUTSIDE de la ruche.
     * Pour chaque slot OUTSIDE, si l'entite n'est plus trouvable dans le monde,
     * remet le slot en INSIDE avec un cooldown de recuperation.
     */
    void verifyOutsideBees() {
        if (!(parent.getLevel() instanceof ServerLevel serverLevel)) return;

        HiveBeeSlot[] beeSlots = parent.getBeeSlots();
        NonNullList<ItemStack> items = parent.getItems();

        for (int i = 0; i < MagicHiveBlockEntity.BEE_SLOTS; i++) {
            if (!beeSlots[i].isOutside()) continue;

            UUID uuid = beeSlots[i].getBeeUUID();
            if (uuid == null) {
                resetSlotToInside(i, beeSlots[i], items.get(i));
                continue;
            }

            Entity entity = serverLevel.getEntity(uuid);
            if (entity == null || entity.isRemoved() || !(entity instanceof MagicBeeEntity)) {
                resetSlotToInside(i, beeSlots[i], items.get(i));
            }
        }
    }

    /**
     * Remet un slot OUTSIDE en INSIDE avec un cooldown de recuperation.
     * Utilise si l'entite associee n'est plus trouvable.
     */
    private void resetSlotToInside(int slot, HiveBeeSlot beeSlot, ItemStack beeItem) {
        parent.returnAssignedFlower(slot);
        beeSlot.setState(HiveBeeSlot.State.INSIDE);
        beeSlot.setBeeUUID(null);

        if (!beeItem.isEmpty()) {
            BeeGeneData geneData = MagicBeeItem.getGeneData(beeItem);
            BeeBehaviorConfig config = BeeBehaviorManager.getConfig(getSpeciesId(geneData));
            beeSlot.setCooldown(config.getRandomRestCooldown(
                    parent.getLevel() != null ? parent.getLevel().getRandom() : RandomSource.create()));
        } else {
            beeSlot.setCooldown(200);
        }

        parent.setChanged();
    }

    // === Loot & Output ===

    void depositPollinationLoot(MagicBeeEntity bee) {
        if (parent.getLevel() == null) return;
        BeeBehaviorConfig config = bee.getBehaviorConfig();
        for (ItemStack stack : config.rollPollinationLoot(parent.getLevel().getRandom())) {
            insertIntoOutputSlots(stack);
        }
    }

    public void insertIntoOutputSlots(ItemStack stack) {
        if (stack.isEmpty()) return;

        NonNullList<ItemStack> items = parent.getItems();

        for (int i = MagicHiveBlockEntity.BEE_SLOTS; i < MagicHiveBlockEntity.TOTAL_SLOTS && !stack.isEmpty(); i++) {
            ItemStack existing = items.get(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)) {
                int toAdd = Math.min(existing.getMaxStackSize() - existing.getCount(), stack.getCount());
                if (toAdd > 0) {
                    existing.grow(toAdd);
                    stack.shrink(toAdd);
                }
            }
        }

        for (int i = MagicHiveBlockEntity.BEE_SLOTS; i < MagicHiveBlockEntity.TOTAL_SLOTS && !stack.isEmpty(); i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, stack.copy());
                stack.setCount(0);
            }
        }
        parent.setChanged();
    }

    // === Breeding ===

    /**
     * Tente le breeding quand une abeille rentre dans la ruche.
     * - 15% de chance si une autre abeille est INSIDE
     * - Sélection aléatoire du partenaire si plusieurs abeilles INSIDE
     */
    void tryAutoBreeding(int enteringSlot, RandomSource random) {
        if (random.nextDouble() >= BREEDING_CHANCE_ON_ENTRY) return;

        HiveBeeSlot[] beeSlots = parent.getBeeSlots();
        NonNullList<ItemStack> items = parent.getItems();

        // Collecter toutes les abeilles INSIDE (sauf celle qui rentre)
        List<Integer> insidePartners = new ArrayList<>();
        for (int i = 0; i < MagicHiveBlockEntity.BEE_SLOTS; i++) {
            if (i != enteringSlot && beeSlots[i].isInside() && !items.get(i).isEmpty()) {
                insidePartners.add(i);
            }
        }
        if (insidePartners.isEmpty()) return;

        // Sélection aléatoire du partenaire
        int partnerSlot = insidePartners.get(random.nextInt(insidePartners.size()));

        // Trouver un slot de sortie libre
        int outputSlot = -1;
        for (int i = MagicHiveBlockEntity.BEE_SLOTS; i < MagicHiveBlockEntity.TOTAL_SLOTS; i++) {
            if (items.get(i).isEmpty()) {
                outputSlot = i;
                break;
            }
        }
        if (outputSlot < 0) return;

        BeeGeneData parent1Data = MagicBeeItem.getGeneData(items.get(enteringSlot));
        BeeGeneData parent2Data = MagicBeeItem.getGeneData(items.get(partnerSlot));

        Gene species1 = parent1Data.getGene(GeneCategory.SPECIES);
        Gene species2 = parent2Data.getGene(GeneCategory.SPECIES);
        if (species1 == null || species2 == null) return;

        String offspringSpecies = BreedingManager.resolveOffspringSpecies(species1.getId(), species2.getId(), random);

        BeeGeneData offspringData = BreedingManager.createOffspringGeneData(parent1Data, parent2Data, offspringSpecies, random);
        items.set(outputSlot, BeeLarvaItem.createWithGenes(offspringData));
        parent.setChanged();
    }

    // === Tick ===

    void tickBeeSlot(int slot) {
        HiveBeeSlot beeSlot = parent.getBeeSlots()[slot];
        NonNullList<ItemStack> items = parent.getItems();
        if (!beeSlot.isInside() || items.get(slot).isEmpty()) return;

        BeeGeneData geneData = MagicBeeItem.getGeneData(items.get(slot));
        BeeBehaviorConfig config = BeeBehaviorManager.getConfig(getSpeciesId(geneData));

        if (beeSlot.needsHealing()) {
            beeSlot.heal(config.getRegenerationRate() / 20.0f);
            if (!beeSlot.needsHealing()) {
                MagicBeeItem.setStoredHealth(items.get(slot), beeSlot.getCurrentHealth());
            }
        }

        if (!beeSlot.needsHealing()) {
            beeSlot.decrementCooldown();
        }

        if (beeSlot.isCooldownComplete() && !beeSlot.needsHealing() && parent.canBeeForage(slot)) {
            releaseBee(slot);
        }
    }

    void checkReturningBees(Level level, BlockPos pos) {
        AABB searchBox = new AABB(pos).inflate(2);
        List<MagicBeeEntity> nearbyBees = level.getEntitiesOfClass(MagicBeeEntity.class, searchBox,
            bee -> bee.hasAssignedHive() && pos.equals(bee.getAssignedHivePos()));

        for (MagicBeeEntity bee : nearbyBees) {
            if (canBeeEnter(bee) && bee.isReturning() && bee.position().distanceTo(pos.getCenter()) < 1.5) {
                addBee(bee);
                bee.discard();
            }
        }
    }
}
