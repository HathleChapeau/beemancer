/**
 * ============================================================
 * [IncubatorBlockEntity.java]
 * Description: BlockEntity incubateur avec timer d'incubation
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                | Raison                | Utilisation                    |
 * |---------------------------|----------------------|--------------------------------|
 * | ItemStackHandler          | Gestion inventaire   | Slot unique avec sync auto     |
 * | BeeLarvaItem              | Detection larve      | Lecture gene data              |
 * | MagicBeeItem              | Creation abeille     | Resultat incubation            |
 * | ParticleHelper            | Particules           | Effet visuel incubation        |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - IncubatorBlock.java (creation, ticker, drop)
 * - IncubatorMenu.java (menu joueur)
 * - IncubatorRenderer.java (rendu item flottant)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.incubator;

import com.chapeau.beemancer.common.item.bee.BeeLarvaItem;
import com.chapeau.beemancer.common.item.bee.MagicBeeItem;
import com.chapeau.beemancer.common.menu.IncubatorMenu;
import com.chapeau.beemancer.content.gene.species.DataDrivenSpeciesGene;
import com.chapeau.beemancer.core.gene.BeeGeneData;
import com.chapeau.beemancer.core.gene.Gene;
import com.chapeau.beemancer.core.gene.GeneCategory;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerItems;
import com.chapeau.beemancer.core.util.ParticleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class IncubatorBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_COUNT = 1;
    public static final int BASE_INCUBATION_TIME = 600; // 30 secondes (base tier I)

    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            incubationProgress = 0;
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    private int incubationProgress = 0;
    private int currentIncubationTime = BASE_INCUBATION_TIME;

    // ContainerData for GUI sync
    public final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> incubationProgress;
                case 1 -> currentIncubationTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) incubationProgress = value;
            if (index == 1) currentIncubationTime = value;
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public IncubatorBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.INCUBATOR.get(), pos, state);
    }

    // --- Accessors ---

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    // --- Tick ---

    public static void serverTick(Level level, BlockPos pos, BlockState state, IncubatorBlockEntity incubator) {
        ItemStack stack = incubator.itemHandler.getStackInSlot(0);

        // Only process larva items
        if (stack.isEmpty() || !stack.is(BeemancerItems.BEE_LARVA.get())) {
            if (incubator.incubationProgress > 0) {
                incubator.incubationProgress = 0;
                incubator.currentIncubationTime = BASE_INCUBATION_TIME;
                incubator.setChanged();
            }
            return;
        }

        // Recalculer le temps d'incubation si progress vient de demarrer
        if (incubator.incubationProgress == 0) {
            incubator.currentIncubationTime = getIncubationTimeForLarva(stack);
        }

        // Increment progress
        incubator.incubationProgress++;

        // Particules grises orbitant autour du bloc pendant l'incubation (2x moins que l'infuser)
        if (level instanceof ServerLevel serverLevel && level.getGameTime() % 4 == 0) {
            DustParticleOptions grayParticle = new DustParticleOptions(new Vector3f(0.6f, 0.6f, 0.6f), 0.5f);
            Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            ParticleHelper.orbitingRing(serverLevel, grayParticle, center, 0.35, 4, 0.08);
        }

        // Check if complete
        if (incubator.incubationProgress >= incubator.currentIncubationTime) {
            // Transform larva to bee
            BeeGeneData geneData = BeeLarvaItem.getGeneData(stack);
            ItemStack beeItem = MagicBeeItem.createWithGenes(geneData);

            incubator.itemHandler.setStackInSlot(0, beeItem);
            incubator.incubationProgress = 0;
            incubator.currentIncubationTime = BASE_INCUBATION_TIME;
            incubator.setChanged();
        }

        incubator.setChanged();
    }

    /**
     * Calcule le temps d'incubation pour une larve selon le tier de son espece.
     * Tier I = 30s, Tier II = 60s, ... Tier X = 300s.
     */
    private static int getIncubationTimeForLarva(ItemStack larvaStack) {
        BeeGeneData geneData = BeeLarvaItem.getGeneData(larvaStack);
        Gene speciesGene = geneData.getGene(GeneCategory.SPECIES);
        if (speciesGene instanceof DataDrivenSpeciesGene ddGene) {
            int tierValue = romanToInt(ddGene.getTier());
            return BASE_INCUBATION_TIME * tierValue;
        }
        return BASE_INCUBATION_TIME;
    }

    /**
     * Convertit un chiffre romain (I-X) en entier.
     */
    private static int romanToInt(String roman) {
        if (roman == null || roman.isEmpty()) return 1;
        int result = 0;
        for (int i = 0; i < roman.length(); i++) {
            int current = romanCharValue(roman.charAt(i));
            int next = (i + 1 < roman.length()) ? romanCharValue(roman.charAt(i + 1)) : 0;
            if (current < next) {
                result -= current;
            } else {
                result += current;
            }
        }
        return Math.max(1, result);
    }

    private static int romanCharValue(char c) {
        return switch (c) {
            case 'I' -> 1;
            case 'V' -> 5;
            case 'X' -> 10;
            case 'L' -> 50;
            case 'C' -> 100;
            default -> 0;
        };
    }

    // --- NBT ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", itemHandler.serializeNBT(registries));
        tag.putInt("Progress", incubationProgress);
        tag.putInt("IncubationTime", currentIncubationTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        incubationProgress = tag.getInt("Progress");
        currentIncubationTime = tag.getInt("IncubationTime");
        if (currentIncubationTime <= 0) currentIncubationTime = BASE_INCUBATION_TIME;
    }

    // --- Menu ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.beemancer.incubator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new IncubatorMenu(containerId, playerInventory, this, containerData);
    }

    // --- Synchronisation client (comme l'infuser) ---

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("Inventory", itemHandler.serializeNBT(registries));
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // --- Drop Contents ---

    public void dropContents() {
        if (level != null && !level.isClientSide()) {
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                        itemHandler.getStackInSlot(i));
            }
        }
    }
}
