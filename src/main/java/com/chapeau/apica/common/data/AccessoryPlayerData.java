/**
 * ============================================================
 * [AccessoryPlayerData.java]
 * Description: Donnees des 2 slots accessoire d'un joueur
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ItemStack           | Stockage items       | 2 slots accessoire             |
 * | Codec               | Serialisation        | Persistance attachment         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaAttachments.java (registration attachment)
 * - AccessoryEquipPacket.java (modification slots)
 * - AccessorySyncPacket.java (sync client)
 * - Apica.java (death drop + login sync)
 *
 * ============================================================
 */
package com.chapeau.apica.common.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

/**
 * Stocke 2 ItemStack representant les accessoires equipes du joueur.
 * Persiste via AttachmentType (PAS copyOnDeath — les accessoires drop au sol).
 */
public class AccessoryPlayerData {

    public static final int SLOT_COUNT = 2;

    public static final Codec<AccessoryPlayerData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("slot0", ItemStack.EMPTY).forGetter(d -> d.slots[0]),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("slot1", ItemStack.EMPTY).forGetter(d -> d.slots[1])
        ).apply(instance, (s0, s1) -> {
            AccessoryPlayerData data = new AccessoryPlayerData();
            data.slots[0] = s0;
            data.slots[1] = s1;
            return data;
        })
    );

    private final ItemStack[] slots = new ItemStack[]{ ItemStack.EMPTY, ItemStack.EMPTY };

    public AccessoryPlayerData() {
    }

    public ItemStack getAccessory(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) return ItemStack.EMPTY;
        return slots[slot];
    }

    public void setAccessory(int slot, ItemStack stack) {
        if (slot >= 0 && slot < SLOT_COUNT) {
            slots[slot] = stack;
        }
    }

    public ItemStack removeAccessory(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) return ItemStack.EMPTY;
        ItemStack old = slots[slot];
        slots[slot] = ItemStack.EMPTY;
        return old;
    }

    public boolean isEmpty() {
        for (ItemStack stack : slots) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }
}
