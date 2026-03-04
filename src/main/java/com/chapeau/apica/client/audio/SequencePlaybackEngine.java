/**
 * ============================================================
 * [SequencePlaybackEngine.java]
 * Description: Moteur de lecture audio client-side avec timing frame-rate independant du tick
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | SequenceData        | Donnees musicales    | Lecture des notes actives      |
 * | DubstepInstrument   | Sons instruments     | playLocalSound par instrument  |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - DubstepRadioScreen (appel update() chaque frame dans render())
 *
 * ============================================================
 */
package com.chapeau.apica.client.audio;

import com.chapeau.apica.common.data.SequenceData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

/**
 * Moteur de playback singleton client-side.
 * Utilise System.currentTimeMillis() pour un timing precis independant du tick rate (50ms).
 * Appele depuis DubstepRadioScreen.render() a chaque frame (~8-16ms de resolution).
 */
@OnlyIn(Dist.CLIENT)
public class SequencePlaybackEngine {

    private static SequenceData data;
    private static BlockPos sourcePos;
    private static boolean playing;
    private static long startTimeMs;
    private static int currentStep = -1;
    private static long lastUpdateMs;

    public static void start(SequenceData sequenceData, BlockPos pos) {
        data = sequenceData.copy();
        sourcePos = pos;
        playing = true;
        currentStep = -1;
        startTimeMs = System.currentTimeMillis();
        lastUpdateMs = startTimeMs;
    }

    public static void stop() {
        playing = false;
        currentStep = 0;
        data = null;
        sourcePos = null;
    }

    public static void updateData(SequenceData newData) {
        if (playing && newData != null) {
            data = newData.copy();
        }
    }

    public static boolean isPlaying() {
        return playing;
    }

    public static int getCurrentStep() {
        return playing ? Math.max(0, currentStep) : -1;
    }

    /**
     * Appele chaque frame depuis le render du screen.
     * Calcule les steps ecoules et joue les notes correspondantes.
     */
    public static void update() {
        if (!playing || data == null || sourcePos == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused() || mc.level == null) return;

        long now = System.currentTimeMillis();
        if (now <= lastUpdateMs) return;
        lastUpdateMs = now;

        long elapsed = now - startTimeMs;
        int stepCount = data.getStepCount();
        double msPerStep = 60000.0 / data.getBpm() / 4.0;

        // Calculer le step cible
        long cycleMs = (long) (msPerStep * stepCount);
        if (cycleMs <= 0) return;
        long elapsedInCycle = elapsed % cycleMs;
        int targetStep = (int) (elapsedInCycle / msPerStep);
        if (targetStep >= stepCount) targetStep = stepCount - 1;

        // Jouer les notes des steps manques
        while (currentStep != targetStep) {
            currentStep = (currentStep + 1) % stepCount;
            playStep(currentStep, mc);
        }
    }

    private static void playStep(int step, Minecraft mc) {
        if (data == null || mc.level == null || sourcePos == null) return;

        List<SequenceData.NoteEvent> notes = data.getActiveNotes(step);
        Vec3 pos = Vec3.atCenterOf(sourcePos);

        for (SequenceData.NoteEvent note : notes) {
            mc.level.playLocalSound(
                    pos.x, pos.y, pos.z,
                    note.instrument().getSound(),
                    SoundSource.RECORDS,
                    note.volume() * 3.0f,
                    note.instrument().getPitch(note.pitch()),
                    false
            );
        }
    }

    /**
     * Joue une note unique immediatement (feedback au click dans la grille).
     */
    public static void playPreview(SequenceData.NoteEvent note, BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || pos == null) return;

        Vec3 center = Vec3.atCenterOf(pos);
        mc.level.playLocalSound(
                center.x, center.y, center.z,
                note.instrument().getSound(),
                SoundSource.RECORDS,
                note.volume() * 3.0f,
                note.instrument().getPitch(note.pitch()),
                false
        );
    }

    public static void cleanup() {
        stop();
    }
}
