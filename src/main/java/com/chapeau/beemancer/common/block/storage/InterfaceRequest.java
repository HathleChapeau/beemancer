/**
 * ============================================================
 * [InterfaceRequest.java]
 * Description: Demande publiee par une interface/terminal vers le controller
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BlockPos            | Position source      | Interface/terminal emetteur    |
 * | ItemStack           | Template item        | Type d'item demande            |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - RequestManager.java (gestion centralisee des demandes)
 * - ImportInterfaceBlockEntity.java (publication demandes import)
 * - ExportInterfaceBlockEntity.java (publication demandes export)
 * - StorageTerminalBlockEntity.java (publication demandes joueur)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Represente une demande publiee par un bloc du reseau (interface ou terminal).
 *
 * - IMPORT: "j'ai besoin de N items de type X" (interface import, terminal requestItem)
 * - EXPORT: "j'ai N items de type X a exporter" (interface export, terminal deposit)
 *
 * Le controller recoit ces demandes et decide du routage (quel coffre, quelle abeille).
 * L'interface ne cherche plus de coffre et ne cree plus de DeliveryTask.
 */
public class InterfaceRequest {

    public enum RequestType {
        IMPORT,
        EXPORT
    }

    public enum RequestStatus {
        PENDING,
        ASSIGNED,
        BLOCKED,
        CANCELLED
    }

    private final UUID requestId;
    private final BlockPos sourcePos;
    private final BlockPos requesterPos;
    private final RequestType type;
    private final ItemStack template;
    private int count;
    private RequestStatus status;
    private String blockedReason;
    @Nullable
    private UUID assignedTaskId;
    private final TaskOrigin origin;
    private final boolean preloaded;

    /**
     * Origine de la demande, pour affichage et priorisation.
     */
    public enum TaskOrigin {
        REQUEST,
        AUTOMATION
    }

    public InterfaceRequest(BlockPos sourcePos, BlockPos requesterPos, RequestType type,
                            ItemStack template, int count, TaskOrigin origin) {
        this(sourcePos, requesterPos, type, template, count, origin, false);
    }

    public InterfaceRequest(BlockPos sourcePos, BlockPos requesterPos, RequestType type,
                            ItemStack template, int count, TaskOrigin origin, boolean preloaded) {
        this.requestId = UUID.randomUUID();
        this.sourcePos = sourcePos;
        this.requesterPos = requesterPos;
        this.type = type;
        this.template = template.copyWithCount(1);
        this.count = count;
        this.status = RequestStatus.PENDING;
        this.blockedReason = "";
        this.assignedTaskId = null;
        this.origin = origin;
        this.preloaded = preloaded;
    }

    /**
     * Constructeur NBT interne.
     */
    private InterfaceRequest(UUID requestId, BlockPos sourcePos, BlockPos requesterPos,
                             RequestType type, ItemStack template, int count,
                             RequestStatus status, String blockedReason,
                             @Nullable UUID assignedTaskId, TaskOrigin origin,
                             boolean preloaded) {
        this.requestId = requestId;
        this.sourcePos = sourcePos;
        this.requesterPos = requesterPos;
        this.type = type;
        this.template = template;
        this.count = count;
        this.status = status;
        this.blockedReason = blockedReason;
        this.assignedTaskId = assignedTaskId;
        this.origin = origin;
        this.preloaded = preloaded;
    }

    // === Getters ===

    public UUID getRequestId() { return requestId; }
    public BlockPos getSourcePos() { return sourcePos; }
    public BlockPos getRequesterPos() { return requesterPos; }
    public RequestType getType() { return type; }
    public ItemStack getTemplate() { return template.copy(); }
    public int getCount() { return count; }
    public RequestStatus getStatus() { return status; }
    public String getBlockedReason() { return blockedReason; }
    @Nullable public UUID getAssignedTaskId() { return assignedTaskId; }
    public TaskOrigin getOrigin() { return origin; }
    public boolean isPreloaded() { return preloaded; }

    // === Setters ===

    public void setCount(int count) { this.count = count; }
    public void setStatus(RequestStatus status) { this.status = status; }
    public void setBlockedReason(String reason) { this.blockedReason = reason; }
    public void setAssignedTaskId(@Nullable UUID taskId) { this.assignedTaskId = taskId; }

    // === NBT ===

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("RequestId", requestId);
        tag.putLong("SourcePos", sourcePos.asLong());
        tag.putLong("RequesterPos", requesterPos.asLong());
        tag.putString("Type", type.name());
        tag.put("Template", template.saveOptional(registries));
        tag.putInt("Count", count);
        tag.putString("Status", status.name());
        tag.putString("BlockedReason", blockedReason);
        tag.putString("Origin", origin.name());
        tag.putBoolean("Preloaded", preloaded);
        if (assignedTaskId != null) {
            tag.putUUID("AssignedTaskId", assignedTaskId);
        }
        return tag;
    }

    public static InterfaceRequest load(CompoundTag tag, HolderLookup.Provider registries) {
        UUID requestId = tag.getUUID("RequestId");
        BlockPos sourcePos = BlockPos.of(tag.getLong("SourcePos"));
        BlockPos requesterPos = tag.contains("RequesterPos")
            ? BlockPos.of(tag.getLong("RequesterPos")) : sourcePos;
        RequestType type = RequestType.valueOf(tag.getString("Type"));
        ItemStack template = ItemStack.parseOptional(registries, tag.getCompound("Template"));
        int count = tag.getInt("Count");
        RequestStatus status = RequestStatus.valueOf(tag.getString("Status"));
        String blockedReason = tag.getString("BlockedReason");
        TaskOrigin origin = tag.contains("Origin")
            ? TaskOrigin.valueOf(tag.getString("Origin"))
            : TaskOrigin.AUTOMATION;
        UUID assignedTaskId = tag.contains("AssignedTaskId")
            ? tag.getUUID("AssignedTaskId") : null;
        boolean preloaded = tag.getBoolean("Preloaded");
        return new InterfaceRequest(requestId, sourcePos, requesterPos, type, template,
            count, status, blockedReason, assignedTaskId, origin, preloaded);
    }
}
