/**
 * ============================================================
 * [SequenceData.java]
 * Description: Donnees completes d'une sequence musicale multi-pages (tracks, BPM, pages)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | TrackData           | Tracks individuelles | Collection de tracks           |
 * | DubstepInstrument   | Instruments          | Ajout de nouvelles tracks      |
 * | CompoundTag/ListTag | Serialisation NBT    | Sauvegarde/chargement          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - WaveMixerBlockEntity.java (stockage persistent)
 * - WaveMixerScreen.java (copie locale client)
 * - SequencePlaybackEngine.java (lecture des notes)
 * - WaveMixerSyncPacket.java (transmission reseau)
 *
 * ============================================================
 */
package com.chapeau.apica.common.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * Modele de donnees complet d'une sequence musicale multi-pages.
 * Chaque page contient STEPS_PER_PAGE steps. Le total = pageCount * STEPS_PER_PAGE.
 */
public class SequenceData {

    public static final int MAX_TRACKS = 6;
    public static final int STEPS_PER_PAGE = 16;
    public static final int MAX_PAGES = 8;
    public static final int MAX_STEPS = STEPS_PER_PAGE * MAX_PAGES;
    public static final int MIN_BPM = 40;
    public static final int MAX_BPM = 300;

    private int pageCount = 1;
    private int bpm = 120;
    private float masterVolume = 0.8f;
    private final TrackData[] tracks = new TrackData[MAX_TRACKS];
    private int trackCount = 0;

    public SequenceData() {
    }

    // === Tracks ===

    public boolean addTrack(DubstepInstrument instrument) {
        if (trackCount >= MAX_TRACKS) return false;
        tracks[trackCount] = new TrackData(instrument, MAX_STEPS);
        trackCount++;
        return true;
    }

    public boolean removeTrack(int index) {
        if (index < 0 || index >= trackCount) return false;
        System.arraycopy(tracks, index + 1, tracks, index, trackCount - index - 1);
        trackCount--;
        tracks[trackCount] = null;
        return true;
    }

    public TrackData getTrack(int index) {
        if (index < 0 || index >= trackCount) return null;
        return tracks[index];
    }

    public int getTrackCount() {
        return trackCount;
    }

    // === Pages ===

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int count) {
        this.pageCount = Math.max(1, Math.min(MAX_PAGES, count));
    }

    public int getStepCount() {
        return pageCount * STEPS_PER_PAGE;
    }

    public boolean addPage() {
        if (pageCount >= MAX_PAGES) return false;
        pageCount++;
        return true;
    }

    public boolean removePage(int pageIndex) {
        if (pageCount <= 1 || pageIndex < 0 || pageIndex >= pageCount) return false;
        for (int i = 0; i < trackCount; i++) {
            tracks[i].removePage(pageIndex, STEPS_PER_PAGE);
        }
        pageCount--;
        return true;
    }

    // === Notes actives a un step ===

    public List<NoteEvent> getActiveNotes(int step) {
        List<NoteEvent> notes = new ArrayList<>();
        boolean hasSolo = hasSoloTrack();

        for (int i = 0; i < trackCount; i++) {
            TrackData track = tracks[i];
            if (track.isMuted()) continue;
            if (hasSolo && !track.isSolo()) continue;

            int mask = track.getPitchMask(step);
            if (mask == 0) continue;

            float vel = track.getVelocity(step) / 100.0f * track.getVolume() * masterVolume;
            for (int p = 0; p < TrackData.PITCH_COUNT; p++) {
                if ((mask & (1 << p)) != 0) {
                    notes.add(new NoteEvent(track.getInstrument(), p, vel));
                }
            }
        }
        return notes;
    }

    private boolean hasSoloTrack() {
        for (int i = 0; i < trackCount; i++) {
            if (tracks[i].isSolo()) return true;
        }
        return false;
    }

    // === Parametres ===

    public int getBpm() { return bpm; }
    public void setBpm(int bpm) { this.bpm = Math.max(MIN_BPM, Math.min(MAX_BPM, bpm)); }

    public float getMasterVolume() { return masterVolume; }
    public void setMasterVolume(float vol) { this.masterVolume = Math.max(0.0f, Math.min(1.0f, vol)); }

    // === NBT ===

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("BPM", bpm);
        tag.putInt("PageCount", pageCount);
        tag.putFloat("Volume", masterVolume);
        tag.putInt("TrackCount", trackCount);

        ListTag trackList = new ListTag();
        for (int i = 0; i < trackCount; i++) {
            trackList.add(tracks[i].save());
        }
        tag.put("Tracks", trackList);
        return tag;
    }

    public void load(CompoundTag tag) {
        bpm = tag.getInt("BPM");
        if (bpm < MIN_BPM || bpm > MAX_BPM) bpm = 120;
        pageCount = tag.getInt("PageCount");
        if (pageCount < 1 || pageCount > MAX_PAGES) pageCount = 1;
        masterVolume = tag.getFloat("Volume");
        if (masterVolume <= 0.0f) masterVolume = 0.8f;

        trackCount = 0;
        if (tag.contains("Tracks", Tag.TAG_LIST)) {
            ListTag trackList = tag.getList("Tracks", Tag.TAG_COMPOUND);
            int count = Math.min(trackList.size(), MAX_TRACKS);
            for (int i = 0; i < count; i++) {
                tracks[i] = TrackData.load(trackList.getCompound(i), MAX_STEPS);
                trackCount++;
            }
        }
    }

    // === Network ===

    public void writeToBuf(FriendlyByteBuf buf) {
        buf.writeNbt(save());
    }

    public static SequenceData readFromBuf(FriendlyByteBuf buf) {
        SequenceData data = new SequenceData();
        CompoundTag tag = buf.readNbt();
        if (tag != null) data.load(tag);
        return data;
    }

    public SequenceData copy() {
        SequenceData copy = new SequenceData();
        copy.load(this.save());
        return copy;
    }

    // === Note Event ===

    public record NoteEvent(DubstepInstrument instrument, int pitch, float volume) {
    }
}
