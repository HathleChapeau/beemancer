/**
 * ============================================================
 * [DubstepRadioScreen.java]
 * Description: Ecran principal du DAW Dubstep Radio — compose des sequences musicales
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance               | Raison                | Utilisation                    |
 * |--------------------------|----------------------|--------------------------------|
 * | DubstepRadioMenu         | Menu associe         | ContainerData sync auto        |
 * | SequenceData             | Donnees locales      | Copie client de la sequence    |
 * | SequencePlaybackEngine   | Moteur audio         | Play/stop/update chaque frame  |
 * | TransportBarWidget       | Barre transport      | Play/stop, BPM, swing, volume  |
 * | InstrumentColumnWidget   | Colonne instruments  | Liste tracks, mute/solo, add   |
 * | SequenceGridWidget       | Grille notes         | Toggle cellules, playhead      |
 * | DubstepRadioSyncPacket   | Sync S2C             | Reception donnees completes    |
 * | DubstepRadio*Packet      | Actions C2S          | Envoi modifications au serveur |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup (registerScreens)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.screen;

import com.chapeau.apica.client.audio.SequencePlaybackEngine;
import com.chapeau.apica.client.gui.widget.InstrumentColumnWidget;
import com.chapeau.apica.client.gui.widget.SequenceGridWidget;
import com.chapeau.apica.client.gui.widget.TransportBarWidget;
import com.chapeau.apica.common.data.DubstepInstrument;
import com.chapeau.apica.common.data.NoteCell;
import com.chapeau.apica.common.data.SequenceData;
import com.chapeau.apica.common.data.TrackData;
import com.chapeau.apica.common.menu.DubstepRadioMenu;
import com.chapeau.apica.core.network.packets.DubstepRadioEditPacket;
import com.chapeau.apica.core.network.packets.DubstepRadioSyncPacket;
import com.chapeau.apica.core.network.packets.DubstepRadioTrackPacket;
import com.chapeau.apica.core.network.packets.DubstepRadioTransportPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Ecran DAW : compose des sequences musicales avec les instruments Note Block.
 * Rendering vectoriel (pas de texture PNG), charte graphique style Resonator.
 */
public class DubstepRadioScreen extends AbstractContainerScreen<DubstepRadioMenu>
        implements DubstepRadioSyncPacket.DubstepRadioSyncReceiver {

    private static final int GUI_W = 290;
    private static final int GUI_H = 170;
    private static final int COL_BG = 0xCC1A1A2E;
    private static final int COL_BORDER = 0xFF555555;
    private static final int COL_TITLE = 0xFF00FF88;

    private SequenceData localData = new SequenceData();
    private TransportBarWidget transportBar;
    private InstrumentColumnWidget instrumentColumn;
    private SequenceGridWidget sequenceGrid;

    public DubstepRadioScreen(DubstepRadioMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = GUI_W;
        this.imageHeight = GUI_H;
        this.inventoryLabelY = GUI_H + 1; // Pas d'inventaire visible
    }

    @Override
    protected void init() {
        super.init();

        int gx = this.leftPos;
        int gy = this.topPos;
        int colW = InstrumentColumnWidget.COL_W;
        int barH = 18;
        int gridX = gx + colW;
        int gridY = gy + barH;
        int gridW = GUI_W - colW;
        int gridH = GUI_H - barH;

        transportBar = new TransportBarWidget(gx, gy, GUI_W, new TransportBarWidget.Listener() {
            @Override
            public void onPlay() { sendTransport(DubstepRadioTransportPacket.PLAY); }
            @Override
            public void onStop() {
                sendTransport(DubstepRadioTransportPacket.STOP);
                SequencePlaybackEngine.stop();
            }
            @Override
            public void onBpmChange(int delta) {
                int newBpm = Math.max(40, Math.min(300, localData.getBpm() + delta));
                localData.setBpm(newBpm);
                sendTransportValue(DubstepRadioTransportPacket.SET_BPM, newBpm, 0);
            }
            @Override
            public void onSwingChange(int pct) {
                localData.setSwing(pct / 100.0f);
                sendTransportValue(DubstepRadioTransportPacket.SET_SWING, 0, pct);
            }
            @Override
            public void onVolumeChange(int pct) {
                localData.setMasterVolume(pct / 100.0f);
            }
        });

        instrumentColumn = new InstrumentColumnWidget(gx, gy + barH, gridH,
                new InstrumentColumnWidget.Listener() {
            @Override
            public void onAddTrack(DubstepInstrument instrument) {
                localData.addTrack(instrument);
                sendTrackAction(localData.getTrackCount() - 1, DubstepRadioTrackPacket.ADD,
                        instrument.ordinal());
            }
            @Override
            public void onRemoveTrack(int trackIndex) {
                localData.removeTrack(trackIndex);
                sendTrackAction(trackIndex, DubstepRadioTrackPacket.REMOVE, 0);
            }
            @Override
            public void onToggleMute(int trackIndex) {
                TrackData track = localData.getTrack(trackIndex);
                if (track != null) {
                    track.setMuted(!track.isMuted());
                    sendTrackAction(trackIndex, DubstepRadioTrackPacket.MUTE,
                            track.isMuted() ? 1 : 0);
                }
            }
            @Override
            public void onToggleSolo(int trackIndex) {
                TrackData track = localData.getTrack(trackIndex);
                if (track != null) {
                    track.setSolo(!track.isSolo());
                    sendTrackAction(trackIndex, DubstepRadioTrackPacket.SOLO,
                            track.isSolo() ? 1 : 0);
                }
            }
            @Override
            public void onSelectTrack(int trackIndex) {
                // Selection visuelle geree dans le widget
            }
        });

        sequenceGrid = new SequenceGridWidget(gridX, gridY, gridW, gridH,
                new SequenceGridWidget.Listener() {
            @Override
            public void onCellToggle(int trackIndex, int stepIndex) {
                TrackData track = localData.getTrack(trackIndex);
                if (track == null) return;
                track.toggleCell(stepIndex);
                NoteCell cell = track.getCell(stepIndex);
                PacketDistributor.sendToServer(new DubstepRadioEditPacket(
                        menu.getBlockPos(), trackIndex, stepIndex, cell.toCompact()));
            }
            @Override
            public void onCellPreview(int trackIndex, int stepIndex) {
                TrackData track = localData.getTrack(trackIndex);
                if (track == null) return;
                NoteCell cell = track.getCell(stepIndex);
                if (cell.active()) {
                    float vol = cell.velocity() / 100.0f * track.getVolume()
                            * localData.getMasterVolume();
                    SequencePlaybackEngine.playPreview(
                            new SequenceData.NoteEvent(track.getInstrument(), cell.pitch(), vol),
                            menu.getBlockPos());
                }
            }
        });
    }

    @Override
    public void onSequenceSync(BlockPos pos, SequenceData data) {
        if (pos.equals(menu.getBlockPos())) {
            this.localData = data;
        }
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        // Update playback engine chaque frame (timing frame-rate)
        SequencePlaybackEngine.update();

        // Sync ContainerData vers localData
        localData.setBpm(menu.getBpm());
        boolean isPlaying = menu.isPlaying();

        // Auto-start/stop engine selon etat serveur
        if (isPlaying && !SequencePlaybackEngine.isPlaying()) {
            SequencePlaybackEngine.start(localData, menu.getBlockPos());
        } else if (!isPlaying && SequencePlaybackEngine.isPlaying()) {
            SequencePlaybackEngine.stop();
        }

        if (SequencePlaybackEngine.isPlaying()) {
            SequencePlaybackEngine.updateData(localData);
        }

        // Fond semi-transparent
        gfx.fill(leftPos - 2, topPos - 2, leftPos + GUI_W + 2, topPos + GUI_H + 2, COL_BORDER);
        gfx.fill(leftPos, topPos, leftPos + GUI_W, topPos + GUI_H, COL_BG);

        // Widgets
        transportBar.update(localData.getBpm(), isPlaying, menu.getSwing(), menu.getMasterVolume());
        transportBar.render(gfx);

        instrumentColumn.render(gfx, localData);

        sequenceGrid.setPlayheadStep(SequencePlaybackEngine.getCurrentStep());
        sequenceGrid.render(gfx, localData);
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        // Pas de labels par defaut (pas d'inventaire joueur)
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (transportBar.mouseClicked(mouseX, mouseY, button)) return true;
        if (instrumentColumn.isDropdownOpen() &&
                instrumentColumn.mouseClicked(mouseX, mouseY, button, localData)) return true;
        if (instrumentColumn.mouseClicked(mouseX, mouseY, button, localData)) return true;
        if (sequenceGrid.mouseClicked(mouseX, mouseY, button, localData)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (transportBar.mouseDragged(mouseX, mouseY)) return true;
        if (sequenceGrid.mouseDragged(mouseX, mouseY, localData)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        transportBar.mouseReleased();
        sequenceGrid.mouseReleased();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (instrumentColumn.mouseScrolled(mouseX, mouseY, scrollY)) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Espace = play/stop
        if (keyCode == 32) {
            if (menu.isPlaying()) {
                sendTransport(DubstepRadioTransportPacket.STOP);
                SequencePlaybackEngine.stop();
            } else {
                sendTransport(DubstepRadioTransportPacket.PLAY);
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        super.removed();
        SequencePlaybackEngine.cleanup();
    }

    // === Packet helpers ===

    private void sendTransport(int action) {
        PacketDistributor.sendToServer(new DubstepRadioTransportPacket(
                menu.getBlockPos(), action, localData.getBpm(),
                (int) (localData.getSwing() * 100)));
    }

    private void sendTransportValue(int action, int bpm, int swing) {
        PacketDistributor.sendToServer(new DubstepRadioTransportPacket(
                menu.getBlockPos(), action, bpm, swing));
    }

    private void sendTrackAction(int trackIndex, int action, int value) {
        PacketDistributor.sendToServer(new DubstepRadioTrackPacket(
                menu.getBlockPos(), trackIndex, action, value));
    }
}
