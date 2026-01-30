/**
 * ============================================================
 * [DeliveryTask.java]
 * Description: Tâche de livraison pour le système de delivery bees
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance     | Raison                | Utilisation                    |
 * |----------------|----------------------|--------------------------------|
 * | BlockPos       | Positions cibles     | Coffre et terminal             |
 * | ItemStack      | Template item        | Item à transporter             |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - StorageControllerBlockEntity.java (queue de livraison)
 * - DeliveryBeeEntity.java (exécution de la tâche)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Représente une tâche de livraison dans le réseau de stockage.
 * Chaque tâche décrit un transfert d'items entre un coffre et un terminal,
 * exécuté par une DeliveryBee.
 */
public class DeliveryTask {

    public enum DeliveryType {
        EXTRACT,
        DEPOSIT
    }

    public enum DeliveryState {
        QUEUED,
        FLYING,
        WAITING,
        RETURNING,
        COMPLETED,
        FAILED
    }

    private final UUID taskId;
    private final ItemStack template;
    private final int count;
    private final BlockPos targetChest;
    private final BlockPos terminalPos;
    private final DeliveryType type;
    private DeliveryState state;

    public DeliveryTask(ItemStack template, int count, BlockPos targetChest,
                        BlockPos terminalPos, DeliveryType type) {
        this.taskId = UUID.randomUUID();
        this.template = template.copy();
        this.count = count;
        this.targetChest = targetChest;
        this.terminalPos = terminalPos;
        this.type = type;
        this.state = DeliveryState.QUEUED;
    }

    private DeliveryTask(UUID taskId, ItemStack template, int count, BlockPos targetChest,
                         BlockPos terminalPos, DeliveryType type, DeliveryState state) {
        this.taskId = taskId;
        this.template = template;
        this.count = count;
        this.targetChest = targetChest;
        this.terminalPos = terminalPos;
        this.type = type;
        this.state = state;
    }

    public UUID getTaskId() { return taskId; }
    public ItemStack getTemplate() { return template.copy(); }
    public int getCount() { return count; }
    public BlockPos getTargetChest() { return targetChest; }
    public BlockPos getTerminalPos() { return terminalPos; }
    public DeliveryType getType() { return type; }
    public DeliveryState getState() { return state; }

    public void setState(DeliveryState state) {
        this.state = state;
    }

    public boolean isActive() {
        return state == DeliveryState.FLYING || state == DeliveryState.WAITING
            || state == DeliveryState.RETURNING;
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("TaskId", taskId);
        tag.put("Template", template.saveOptional(registries));
        tag.putInt("Count", count);
        tag.putLong("TargetChest", targetChest.asLong());
        tag.putLong("TerminalPos", terminalPos.asLong());
        tag.putString("Type", type.name());
        tag.putString("State", state.name());
        return tag;
    }

    public static DeliveryTask load(CompoundTag tag, HolderLookup.Provider registries) {
        UUID id = tag.getUUID("TaskId");
        ItemStack template = ItemStack.parseOptional(registries, tag.getCompound("Template"));
        int count = tag.getInt("Count");
        BlockPos chest = BlockPos.of(tag.getLong("TargetChest"));
        BlockPos terminal = BlockPos.of(tag.getLong("TerminalPos"));
        DeliveryType type = DeliveryType.valueOf(tag.getString("Type"));
        DeliveryState state = DeliveryState.valueOf(tag.getString("State"));
        return new DeliveryTask(id, template, count, chest, terminal, type, state);
    }
}
