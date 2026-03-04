/**
 * ============================================================
 * [DubstepRadioScreen.java]
 * Description: Ecran principal du DAW Dubstep Radio — deux modes : liste instruments / piano-roll
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance               | Raison                | Utilisation                    |
 * |--------------------------|----------------------|--------------------------------|
 * | DubstepRadioMenu         | Menu associe         | ContainerData sync auto        |
 * | SequenceData             | Donnees locales      | Copie client de la sequence    |
 * | SequencePlaybackEngine   | Moteur audio         | Play/stop/update chaque frame  |
 * | TransportBarWidget       | Barre transport      | Play/stop, BPM, volume, back   |
 * | InstrumentColumnWidget   | Liste instruments    | M/S/X/Edit, add track          |
 * | TrackEditorWidget        | Piano-roll           | Edition notes par track        |
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
import com.chapeau.apica.client.gui.widget.TrackEditorWidget;
import com.chapeau.apica.client.gui.widget.TransportBarWidget;
import com.chapeau.apica.common.data.DubstepInstrument;
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
 * Ecran DAW avec deux modes et taille dynamique :
 * - Mode principal (editingTrack == -1) : liste des instruments (hauteur reduite)
 * - Mode editeur (editingTrack >= 0) : piano-roll pour un track (hauteur etendue)
 */
public class DubstepRadioScreen extends AbstractContainerScreen<DubstepRadioMenu>
        implements DubstepRadioSyncPacket.DubstepRadioSyncReceiver {

    private static final int GUI_W = 260;
    private static final int MAIN_H = 150;
    private static final int EDITOR_H = 220;
    private static final int BAR_H = 18;
    private static final int COL_BG = 0xCC1A1A2E;
    private static final int COL_BORDER = 0xFF555555;

    private SequenceData localData = new SequenceData();
    private int editingTrack = -1;

    private TransportBarWidget mainTransport;
    private InstrumentColumnWidget instrumentColumn;
    private TransportBarWidget editorTransport;
    private TrackEditorWidget trackEditor;

    public DubstepRadioScreen(DubstepRadioMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = GUI_W;
        this.imageHeight = MAIN_H;
        this.inventoryLabelY = MAIN_H + 1;
    }

    @Override
    protected void init() {
        super.init();
        updateLayout();
    }

    private void updateLayout() {
        int currentH = (editingTrack == -1) ? MAIN_H : EDITOR_H;
        this.imageHeight = currentH;
        this.leftPos = (this.width - GUI_W) / 2;
        this.topPos = (this.height - currentH) / 2;
        this.inventoryLabelY = currentH + 1;

        int gx = this.leftPos;
        int gy = this.topPos;

        TransportBarWidget.Listener transportListener = new TransportBarWidget.Listener() {
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
                sendTransportValue(DubstepRadioTransportPacket.SET_BPM, newBpm);
            }
            @Override
            public void onVolumeChange(int pct) {
                localData.setMasterVolume(pct / 100.0f);
            }
        };

        mainTransport = new TransportBarWidget(gx, gy, GUI_W, transportListener, null);
        editorTransport = new TransportBarWidget(gx, gy, GUI_W, transportListener,
                () -> { editingTrack = -1; updateLayout(); });

        instrumentColumn = new InstrumentColumnWidget(gx, gy + BAR_H, GUI_W, currentH - BAR_H,
                new InstrumentColumnWidget.Listener() {
            @Override
            public void onAddTrack(DubstepInstrument instrument) {
                localData.addTrack(instrument);
                sendTrackAction(localData.getTrackCount() - 1, DubstepRadioTrackPacket.ADD,
                        instrument.ordinal());
            }
            @Override
            public void onDeleteTrack(int trackIndex) {
                if (editingTrack == trackIndex) editingTrack = -1;
                else if (editingTrack > trackIndex) editingTrack--;
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
            public void onEditTrack(int trackIndex) {
                editingTrack = trackIndex;
                updateLayout();
            }
        });

        trackEditor = new TrackEditorWidget(gx, gy + BAR_H, GUI_W, currentH - BAR_H,
                new TrackEditorWidget.Listener() {
            @Override
            public void onPitchToggle(int stepIndex, int pitch, boolean activate) {
                if (editingTrack < 0) return;
                TrackData track = localData.getTrack(editingTrack);
                if (track == null) return;
                track.setPitchActive(stepIndex, pitch, activate);
                PacketDistributor.sendToServer(new DubstepRadioEditPacket(
                        menu.getBlockPos(), editingTrack, stepIndex, pitch, activate));
            }
            @Override
            public void onPitchPreview(int pitch) {
                if (editingTrack < 0) return;
                TrackData track = localData.getTrack(editingTrack);
                if (track == null) return;
                float vol = track.getVolume() * localData.getMasterVolume();
                SequencePlaybackEngine.playPreview(
                        new SequenceData.NoteEvent(track.getInstrument(), pitch, vol),
                        menu.getBlockPos());
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
        SequencePlaybackEngine.update();

        localData.setBpm(menu.getBpm());
        boolean isPlaying = menu.isPlaying();

        if (isPlaying && !SequencePlaybackEngine.isPlaying()) {
            SequencePlaybackEngine.start(localData, menu.getBlockPos());
        } else if (!isPlaying && SequencePlaybackEngine.isPlaying()) {
            SequencePlaybackEngine.stop();
        }

        if (SequencePlaybackEngine.isPlaying()) {
            SequencePlaybackEngine.updateData(localData);
        }

        if (editingTrack >= localData.getTrackCount()) {
            editingTrack = -1;
            updateLayout();
        }

        int currentH = (editingTrack == -1) ? MAIN_H : EDITOR_H;

        // Background
        gfx.fill(leftPos - 2, topPos - 2, leftPos + GUI_W + 2, topPos + currentH + 2, COL_BORDER);
        gfx.fill(leftPos, topPos, leftPos + GUI_W, topPos + currentH, COL_BG);

        int volumePct = menu.getMasterVolume();

        if (editingTrack == -1) {
            mainTransport.update(localData.getBpm(), isPlaying, volumePct);
            mainTransport.render(gfx);
            instrumentColumn.render(gfx, localData);
        } else {
            editorTransport.update(localData.getBpm(), isPlaying, volumePct);
            editorTransport.render(gfx);

            TrackData track = localData.getTrack(editingTrack);
            if (track != null) {
                trackEditor.setPlayheadStep(SequencePlaybackEngine.getCurrentStep());
                trackEditor.render(gfx, track, localData.getStepCount());
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (editingTrack == -1) {
            if (mainTransport.mouseClicked(mouseX, mouseY, button)) return true;
            if (instrumentColumn.isDropdownOpen() &&
                    instrumentColumn.mouseClicked(mouseX, mouseY, button, localData)) return true;
            if (instrumentColumn.mouseClicked(mouseX, mouseY, button, localData)) return true;
        } else {
            if (editorTransport.mouseClicked(mouseX, mouseY, button)) return true;
            TrackData track = localData.getTrack(editingTrack);
            if (track != null) {
                if (trackEditor.mouseClicked(mouseX, mouseY, button, track,
                        localData.getStepCount())) return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        if (editingTrack == -1) {
            if (mainTransport.mouseDragged(mouseX, mouseY)) return true;
        } else {
            if (editorTransport.mouseDragged(mouseX, mouseY)) return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        mainTransport.mouseReleased();
        editorTransport.mouseReleased();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (editingTrack == -1) {
            if (instrumentColumn.mouseScrolled(mouseX, mouseY, scrollY)) return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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
                menu.getBlockPos(), action, localData.getBpm(), 0));
    }

    private void sendTransportValue(int action, int bpm) {
        PacketDistributor.sendToServer(new DubstepRadioTransportPacket(
                menu.getBlockPos(), action, bpm, 0));
    }

    private void sendTrackAction(int trackIndex, int action, int value) {
        PacketDistributor.sendToServer(new DubstepRadioTrackPacket(
                menu.getBlockPos(), trackIndex, action, value));
    }
}
