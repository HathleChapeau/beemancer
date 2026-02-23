/**
 * ============================================================
 * [ResonatorBlockEntity.java]
 * Description: Stocke les 4 parametres d'onde, une abeille optionnelle et l'etat d'analyse
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | ApicaBlockEntities      | Type enregistre      | Construction                   |
 * | ResonatorMenu           | Menu associe         | createMenu()                   |
 * | ContainerData           | Sync serveur→client  | 7 valeurs (4 onde + hasBee + analysis) |
 * | BeeSpeciesManager       | Donnees espece       | Tier pour duree analyse        |
 * | BeeInjectionHelper      | Bonus traits         | Niveaux exacts des traits      |
 * | CodexPlayerData         | Knowledge joueur     | Deblocage traits/espece        |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ResonatorBlock (newBlockEntity, openMenu, bee placement, ticker)
 * - ResonatorMenu (server constructor)
 * - ResonatorFinishPacket (completeAnalysis)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.resonator;

import com.chapeau.apica.common.codex.CodexPlayerData;
import com.chapeau.apica.common.item.bee.MagicBeeItem;
import com.chapeau.apica.common.item.essence.EssenceItem;
import com.chapeau.apica.common.menu.ResonatorMenu;
import com.chapeau.apica.core.bee.BeeSpeciesManager;
import com.chapeau.apica.core.network.packets.CodexSyncPacket;
import com.chapeau.apica.core.registry.ApicaAttachments;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import com.chapeau.apica.core.util.BeeInjectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.UUID;

public class ResonatorBlockEntity extends BlockEntity implements MenuProvider {

    /** Frequency in Hz (1-80), stored as raw int. */
    private int frequency = 20;
    /** Amplitude 0-100 (displayed as 0.0-1.0). */
    private int amplitude = 70;
    /** Phase 0-360 (degrees). */
    private int phase = 0;
    /** Harmonics 0-100 (displayed as 0.0-1.0). */
    private int harmonics = 0;

    /** Abeille stockee sur le resonateur (visuel + target waveform). */
    private ItemStack storedBee = ItemStack.EMPTY;

    // ========== ANALYSIS STATE ==========

    private boolean analysisInProgress = false;
    private int analysisProgress = 0;
    private int analysisDuration = 0;
    @Nullable
    private UUID analyzingPlayerUUID = null;
    /** Trait key being analyzed (null = full species analysis, non-null = single trait analysis). */
    @Nullable
    private String analyzingTraitKey = null;

    public final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> frequency;
                case 1 -> amplitude;
                case 2 -> phase;
                case 3 -> harmonics;
                case 4 -> storedBee.isEmpty() ? 0 : 1;
                case 5 -> analysisProgress;
                case 6 -> analysisDuration;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> frequency = value;
                case 1 -> amplitude = value;
                case 2 -> phase = value;
                case 3 -> harmonics = value;
            }
            setChanged();
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    public ResonatorBlockEntity(BlockPos pos, BlockState state) {
        super(ApicaBlockEntities.RESONATOR.get(), pos, state);
    }

    // ========== SERVER TICK ==========

    public static void serverTick(Level level, BlockPos pos, BlockState state, ResonatorBlockEntity be) {
        if (!be.analysisInProgress) return;
        if (be.storedBee.isEmpty()) {
            be.cancelAnalysis();
            return;
        }
        if (be.analysisProgress < be.analysisDuration) {
            be.analysisProgress++;
            be.setChanged();
        }
    }

    // ========== ANALYSIS ==========

    public boolean isAnalysisInProgress() {
        return analysisInProgress;
    }

    /**
     * Demarre l'analyse d'une abeille inconnue.
     * Duree = 10 secondes * tier de l'espece (en ticks).
     */
    public void startAnalysis(UUID playerUUID) {
        String speciesId = MagicBeeItem.getSpeciesId(storedBee);
        BeeSpeciesManager.BeeSpeciesData data = speciesId != null
                ? BeeSpeciesManager.getSpecies(speciesId) : null;
        int tierNum = data != null ? parseTierNumber(data.tier) : 1;
        this.analysisDuration = tierNum * 10 * 20;
        this.analysisProgress = 0;
        this.analysisInProgress = true;
        this.analyzingPlayerUUID = playerUUID;
        setChanged();
    }

    /**
     * Demarre l'analyse d'un trait specifique (match waveform).
     * Duree fixe 10 secondes (200 ticks).
     */
    public void startTraitAnalysis(UUID playerUUID, String traitKey) {
        this.analysisDuration = 200;
        this.analysisProgress = 0;
        this.analysisInProgress = true;
        this.analyzingPlayerUUID = playerUUID;
        this.analyzingTraitKey = traitKey;
        setChanged();
    }

    @Nullable
    public String getAnalyzingTraitKey() {
        return analyzingTraitKey;
    }

    /**
     * Complete l'analyse d'un trait: apprend seulement le trait specifique.
     * - "species:xxx" → learnFrequency(xxx)
     * - "compat:xxx" → learnFrequency(xxx)
     * - sinon (stat trait) → learnTrait(traitKey)
     */
    public void completeTraitAnalysis(ServerPlayer player) {
        if (!analysisInProgress || analysisProgress < analysisDuration) return;
        if (analyzingTraitKey == null) {
            cancelAnalysis();
            return;
        }

        CodexPlayerData codex = player.getData(ApicaAttachments.CODEX_DATA);

        if (analyzingTraitKey.startsWith("species:")) {
            String speciesId = analyzingTraitKey.substring("species:".length());
            codex.learnFrequency(speciesId);
        } else if (analyzingTraitKey.startsWith("compat:")) {
            String compatId = analyzingTraitKey.substring("compat:".length());
            codex.learnFrequency(compatId);
        } else {
            codex.learnTrait(analyzingTraitKey);
        }

        player.setData(ApicaAttachments.CODEX_DATA, codex);
        PacketDistributor.sendToPlayer(player, new CodexSyncPacket(codex));

        cancelAnalysis();
    }

    /**
     * Complete l'analyse: debloque l'espece et les niveaux exacts des 5 traits.
     */
    public void completeAnalysis(ServerPlayer player) {
        if (!analysisInProgress || analysisProgress < analysisDuration) return;

        String speciesId = MagicBeeItem.getSpeciesId(storedBee);
        if (speciesId == null) {
            cancelAnalysis();
            return;
        }

        CodexPlayerData codex = player.getData(ApicaAttachments.CODEX_DATA);
        codex.learnSpecies(speciesId);

        BeeSpeciesManager.BeeSpeciesData data = BeeSpeciesManager.getSpecies(speciesId);
        if (data != null) {
            int dropTotal = data.dropLevel + BeeInjectionHelper.getBonusLevel(storedBee, EssenceItem.EssenceType.DROP);
            codex.learnTrait("drop:" + dropTotal);

            int speedTotal = data.flyingSpeedLevel + BeeInjectionHelper.getBonusLevel(storedBee, EssenceItem.EssenceType.SPEED);
            codex.learnTrait("speed:" + speedTotal);

            int foragingTotal = data.foragingDurationLevel + BeeInjectionHelper.getBonusLevel(storedBee, EssenceItem.EssenceType.FORAGING);
            codex.learnTrait("foraging:" + foragingTotal);

            int toleranceTotal = data.toleranceLevel + BeeInjectionHelper.getBonusLevel(storedBee, EssenceItem.EssenceType.TOLERANCE);
            codex.learnTrait("tolerance:" + toleranceTotal);

            int activityBase = BeeInjectionHelper.getActivityLevel(data.dayNight) + 1;
            int activityBonus = BeeInjectionHelper.getBonusLevel(storedBee, EssenceItem.EssenceType.DIURNAL);
            codex.learnTrait("activity:" + (activityBase + activityBonus));
        }

        player.setData(ApicaAttachments.CODEX_DATA, codex);
        PacketDistributor.sendToPlayer(player, new CodexSyncPacket(codex));

        cancelAnalysis();
    }

    public void cancelAnalysis() {
        analysisInProgress = false;
        analysisProgress = 0;
        analysisDuration = 0;
        analyzingPlayerUUID = null;
        analyzingTraitKey = null;
        setChanged();
    }

    private static int parseTierNumber(String tier) {
        return switch (tier) {
            case "I" -> 1;
            case "II" -> 2;
            case "III" -> 3;
            case "IV" -> 4;
            case "V" -> 5;
            default -> 1;
        };
    }

    // ========== BEE STORAGE ==========

    public boolean hasBee() {
        return !storedBee.isEmpty();
    }

    public ItemStack getStoredBee() {
        return storedBee;
    }

    /**
     * Place une abeille sur le resonateur.
     * @param stack l'item abeille (sera copie avec count=1)
     * @return true si l'abeille a ete placee
     */
    public boolean placeBee(ItemStack stack) {
        if (stack.isEmpty()) return false;
        storedBee = stack.copyWithCount(1);
        setChanged();
        syncToClient();
        return true;
    }

    /**
     * Retire l'abeille du resonateur.
     * @return l'abeille retiree ou ItemStack.EMPTY
     */
    public ItemStack removeBee() {
        if (storedBee.isEmpty()) return ItemStack.EMPTY;
        ItemStack removed = storedBee.copy();
        storedBee = ItemStack.EMPTY;
        cancelAnalysis();
        setChanged();
        syncToClient();
        return removed;
    }

    // ========== MENU ==========

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.apica.resonator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ResonatorMenu(containerId, playerInventory, containerData, worldPosition, storedBee, false);
    }

    // ========== NBT ==========

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Frequency", frequency);
        tag.putInt("Amplitude", amplitude);
        tag.putInt("Phase", phase);
        tag.putInt("Harmonics", harmonics);
        if (!storedBee.isEmpty()) {
            tag.put("StoredBee", storedBee.save(registries, new CompoundTag()));
        }
        if (analysisInProgress) {
            tag.putBoolean("AnalysisInProgress", true);
            tag.putInt("AnalysisProgress", analysisProgress);
            tag.putInt("AnalysisDuration", analysisDuration);
            if (analyzingPlayerUUID != null) {
                tag.putUUID("AnalyzingPlayer", analyzingPlayerUUID);
            }
            if (analyzingTraitKey != null) {
                tag.putString("AnalyzingTraitKey", analyzingTraitKey);
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        frequency = tag.getInt("Frequency");
        amplitude = tag.getInt("Amplitude");
        phase = tag.getInt("Phase");
        harmonics = tag.getInt("Harmonics");
        if (tag.contains("StoredBee")) {
            storedBee = ItemStack.parse(registries, tag.getCompound("StoredBee"))
                    .orElse(ItemStack.EMPTY);
        } else {
            storedBee = ItemStack.EMPTY;
        }
        analysisInProgress = tag.getBoolean("AnalysisInProgress");
        analysisProgress = tag.getInt("AnalysisProgress");
        analysisDuration = tag.getInt("AnalysisDuration");
        if (tag.hasUUID("AnalyzingPlayer")) {
            analyzingPlayerUUID = tag.getUUID("AnalyzingPlayer");
        }
        if (tag.contains("AnalyzingTraitKey")) {
            analyzingTraitKey = tag.getString("AnalyzingTraitKey");
        } else {
            analyzingTraitKey = null;
        }
    }

    // ========== CLIENT SYNC ==========

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("Frequency", frequency);
        tag.putInt("Amplitude", amplitude);
        tag.putInt("Phase", phase);
        tag.putInt("Harmonics", harmonics);
        if (!storedBee.isEmpty()) {
            tag.put("StoredBee", storedBee.save(registries, new CompoundTag()));
        }
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

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt,
                             HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            loadAdditional(tag, registries);
        }
    }

    // ========== SETTERS ==========

    public void setFrequency(int freq) { this.frequency = freq; setChanged(); }
    public void setAmplitude(int amp) { this.amplitude = amp; setChanged(); }
    public void setPhase(int ph) { this.phase = ph; setChanged(); }
    public void setHarmonics(int harm) { this.harmonics = harm; setChanged(); }
}
