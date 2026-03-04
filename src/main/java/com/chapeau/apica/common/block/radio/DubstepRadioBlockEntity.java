/**
 * ============================================================
 * [DubstepRadioBlockEntity.java]
 * Description: BlockEntity du Dubstep Radio — stocke la sequence, gere le transport et les particules
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance               | Raison                | Utilisation                    |
 * |--------------------------|----------------------|--------------------------------|
 * | SequenceData             | Donnees musicales    | Stockage/serialisation         |
 * | DubstepRadioMenu         | Menu GUI             | createMenu()                   |
 * | ApicaBlockEntities       | Type enregistre      | Construction                   |
 * | ContainerData            | Sync S->C            | BPM, pageCount, transport      |
 * | ParticleHelper           | Particules au beat   | Effets visuels pendant play    |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - DubstepRadioBlock (newBlockEntity, openMenu, ticker)
 * - DubstepRadioMenu (server constructor)
 * - DubstepRadioEditPacket (modifications cellules)
 * - DubstepRadioTransportPacket (play/stop)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.radio;

import com.chapeau.apica.common.data.SequenceData;
import com.chapeau.apica.common.menu.DubstepRadioMenu;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import com.chapeau.apica.core.util.ParticleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class DubstepRadioBlockEntity extends BlockEntity implements MenuProvider {

    private final SequenceData sequenceData = new SequenceData();
    private boolean playing = false;
    private int currentStep = 0;
    private float tickAccumulator = 0.0f;

    public final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> sequenceData.getBpm();
                case 1 -> sequenceData.getPageCount();
                case 2 -> playing ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> sequenceData.setBpm(value);
                case 1 -> sequenceData.setPageCount(value);
                case 2 -> playing = value == 1;
            }
            setChanged();
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public DubstepRadioBlockEntity(BlockPos pos, BlockState state) {
        super(ApicaBlockEntities.DUBSTEP_RADIO.get(), pos, state);
    }

    // === Server Tick ===

    public static void serverTick(Level level, BlockPos pos, BlockState state, DubstepRadioBlockEntity be) {
        if (!be.playing) return;

        float ticksPerStep = 1200.0f / be.sequenceData.getBpm() / 4.0f;
        be.tickAccumulator += 1.0f;

        if (be.tickAccumulator >= ticksPerStep) {
            be.tickAccumulator -= ticksPerStep;
            int prevStep = be.currentStep;
            be.currentStep = (be.currentStep + 1) % be.sequenceData.getStepCount();

            // Particules sur les beats forts (tous les 4 steps)
            if (be.currentStep % 4 == 0 && level instanceof ServerLevel serverLevel) {
                Vec3 center = Vec3.atCenterOf(pos).add(0, 0.8, 0);
                ParticleHelper.burst(serverLevel, center, ParticleHelper.EffectType.MAGIC, 4);
            }

            be.setChanged();
        }

        // Sync blockstate PLAYING
        boolean stateValue = state.getValue(DubstepRadioBlock.PLAYING);
        if (stateValue != be.playing) {
            level.setBlock(pos, state.setValue(DubstepRadioBlock.PLAYING, be.playing), 3);
        }
    }

    // === Transport ===

    public void play() {
        this.playing = true;
        this.currentStep = 0;
        this.tickAccumulator = 0.0f;
        setChanged();
        syncBlockstate();
    }

    public void stop() {
        this.playing = false;
        this.currentStep = 0;
        this.tickAccumulator = 0.0f;
        setChanged();
        syncBlockstate();
    }

    public boolean isPlaying() {
        return playing;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    // === Data Access ===

    public SequenceData getSequenceData() {
        return sequenceData;
    }

    // === Menu ===

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.apica.dubstep_radio");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new DubstepRadioMenu(containerId, playerInventory, containerData, worldPosition);
    }

    // === NBT ===

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Sequence", sequenceData.save());
        tag.putBoolean("Playing", playing);
        tag.putInt("CurrentStep", currentStep);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Sequence")) {
            sequenceData.load(tag.getCompound("Sequence"));
        }
        playing = tag.getBoolean("Playing");
        currentStep = tag.getInt("CurrentStep");
    }

    // === Client Sync ===

    private void syncBlockstate() {
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            boolean stateValue = state.getValue(DubstepRadioBlock.PLAYING);
            if (stateValue != playing) {
                level.setBlock(worldPosition, state.setValue(DubstepRadioBlock.PLAYING, playing), 3);
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("Sequence", sequenceData.save());
        tag.putBoolean("Playing", playing);
        tag.putInt("CurrentStep", currentStep);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }
}
