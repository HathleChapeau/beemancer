/**
 * ============================================================
 * [HiveManager.java]
 * Description: Gestion des hives liees au reseau de stockage
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                      | Raison                | Utilisation                    |
 * |---------------------------------|------------------------|-----------------------|
 * | StorageControllerBlockEntity    | Parent controller     | Acces registre, level, essences |
 * | StorageHiveBlockEntity          | Hive liee             | Link/unlink, etat visuel       |
 * | StorageHiveBlock                | Bloc hive             | Multiplicateur honey           |
 * | StorageNetworkRegistry          | Registre central      | Enregistrement hives           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageControllerBlockEntity.java (delegation)
 * - StorageEvents.java (MAX_LINKED_HIVES)
 *
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.storage;

import com.chapeau.apica.common.block.storage.StorageHiveBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Gere les hives liees au controller du reseau de stockage.
 * Extrait de StorageControllerBlockEntity pour respecter le principe
 * de responsabilite unique (meme pattern que StorageChestManager).
 *
 * Responsabilites:
 * - Lier/delier les hives au controller
 * - Calculer le multiplicateur de consommation honey
 * - Calculer le nombre max de delivery bees
 * - Notifier les hives pour mise a jour visuelle
 * - Gerer l'overflow d'essences quand des hives sont retirees
 */
public class HiveManager {

    public static final int MAX_LINKED_HIVES = 4;
    public static final int BASE_ESSENCE_SLOTS = 4;
    public static final int MAX_ESSENCE_SLOTS = BASE_ESSENCE_SLOTS + MAX_LINKED_HIVES;
    public static final int BASE_DELIVERY_BEES = 2;

    private final StorageControllerBlockEntity parent;

    public HiveManager(StorageControllerBlockEntity parent) {
        this.parent = parent;
    }

    /**
     * Enregistre une hive dans le registre du reseau et notifie la hive.
     * Self-contained: la hive est automatiquement liee au controller.
     */
    public void linkHive(BlockPos hivePos) {
        parent.getNetworkRegistry().registerBlock(hivePos, parent.getBlockPos(),
                StorageNetworkRegistry.NetworkBlockType.HIVE);
        if (parent.getLevel() != null && parent.getLevel().hasChunkAt(hivePos)) {
            BlockEntity be = parent.getLevel().getBlockEntity(hivePos);
            if (be instanceof StorageHiveBlockEntity hive) {
                hive.linkToController(parent.getBlockPos());
            }
        }
        parent.setChanged();
        parent.syncToClient();
    }

    /**
     * Retire une hive du registre, notifie la hive et drop les essences overflow.
     */
    public void unlinkHive(BlockPos hivePos) {
        parent.getNetworkRegistry().unregisterBlock(hivePos);
        if (parent.getLevel() != null && parent.getLevel().hasChunkAt(hivePos)) {
            BlockEntity be = parent.getLevel().getBlockEntity(hivePos);
            if (be instanceof StorageHiveBlockEntity hive) {
                hive.unlinkController();
            }
        }
        dropOverflowEssences();
        parent.setChanged();
        parent.syncToClient();
    }

    /**
     * Retourne le nombre de hives liees au controller.
     */
    public int getLinkedHiveCount() {
        return parent.getNetworkRegistry().getHiveCount();
    }

    /**
     * Calcule le nombre max de delivery bees: base + somme des tiers des hives.
     */
    public int getMaxDeliveryBees() {
        if (parent.getLevel() == null) return BASE_DELIVERY_BEES;
        int bonus = 0;
        for (BlockPos hivePos : parent.getNetworkRegistry().getAllHives()) {
            if (!parent.getLevel().hasChunkAt(hivePos)) continue;
            BlockEntity be = parent.getLevel().getBlockEntity(hivePos);
            if (be instanceof StorageHiveBlockEntity hive) {
                bonus += hive.getTier();
            }
        }
        return BASE_DELIVERY_BEES + bonus;
    }

    /**
     * Calcule le multiplicateur de consommation de miel (produit des multiplicateurs de chaque hive).
     * Retourne 1.0f si aucune hive.
     */
    public float getHiveMultiplier() {
        if (parent.getLevel() == null) return 1.0f;
        float multiplier = 1.0f;
        for (BlockPos hivePos : parent.getNetworkRegistry().getAllHives()) {
            if (!parent.getLevel().hasChunkAt(hivePos)) continue;
            BlockEntity be = parent.getLevel().getBlockEntity(hivePos);
            if (be instanceof StorageHiveBlockEntity hive) {
                BlockState hiveState = parent.getLevel().getBlockState(hivePos);
                if (hiveState.getBlock() instanceof StorageHiveBlock hiveBlock) {
                    multiplier *= hiveBlock.getHoneyMultiplier();
                }
            }
        }
        return multiplier;
    }

    /**
     * Notifie toutes les hives liees pour mettre a jour leur etat visuel.
     * Appele quand le multibloc est forme ou detruit.
     */
    public void notifyLinkedHives() {
        if (parent.getLevel() == null || parent.getLevel().isClientSide()) return;
        for (BlockPos hivePos : parent.getNetworkRegistry().getAllHives()) {
            if (!parent.getLevel().hasChunkAt(hivePos)) continue;
            BlockEntity be = parent.getLevel().getBlockEntity(hivePos);
            if (be instanceof StorageHiveBlockEntity hive) {
                hive.updateVisualState();
            }
        }
    }

    /**
     * Drop les items essence dans les slots bonus qui depassent le nombre de hives actuel.
     * Les slots actifs: BASE_ESSENCE_SLOTS + getLinkedHiveCount().
     */
    public void dropOverflowEssences() {
        if (parent.getLevel() == null || parent.getLevel().isClientSide()) return;
        int activeSlots = BASE_ESSENCE_SLOTS + getLinkedHiveCount();
        for (int i = activeSlots; i < MAX_ESSENCE_SLOTS; i++) {
            ItemStack stack = parent.getEssenceSlots().getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(parent.getLevel(),
                        parent.getBlockPos().getX() + 0.5,
                        parent.getBlockPos().getY() + 1.0,
                        parent.getBlockPos().getZ() + 0.5, stack);
                parent.getEssenceSlots().setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    /**
     * Delie toutes les hives (appele lors du setRemoved du controller).
     */
    public void unlinkAllHives() {
        if (parent.getLevel() == null || parent.getLevel().isClientSide()) return;
        for (BlockPos hivePos : parent.getNetworkRegistry().getAllHives()) {
            if (!parent.getLevel().hasChunkAt(hivePos)) continue;
            BlockEntity be = parent.getLevel().getBlockEntity(hivePos);
            if (be instanceof StorageHiveBlockEntity hive) {
                hive.unlinkController();
            }
        }
    }
}
