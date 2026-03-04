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
 * | PlayMode            | Mode de lecture      | Play/Loop/Page variants        |
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
 * Supporte 4 modes de lecture : Play, Play Page, Loop, Page Loop.
 */
@OnlyIn(Dist.CLIENT)
public class SequencePlaybackEngine {

    private static SequenceData data;
    private static BlockPos sourcePos;
    private static boolean playing;
    private static long startTimeMs;
    private static int currentStep = -1;
    private static long lastUpdateMs;

    private static PlayMode playMode = PlayMode.LOOP;
    private static int rangeStart = 0;
    private static int rangeEnd = 16;
    private static boolean autoStopped = false;

    public static void start(SequenceData sequenceData, BlockPos pos, PlayMode mode, int currentPage) {
        data = sequenceData.copy();
        sourcePos = pos;
        playMode = mode;
        playing = true;
        autoStopped = false;

        int totalSteps = data.getStepCount();
        int stepsPerPage = SequenceData.STEPS_PER_PAGE;

        switch (mode) {
            case PLAY, LOOP -> {
                rangeStart = 0;
                rangeEnd = totalSteps;
            }
            case PLAY_PAGE, PAGE_LOOP -> {
                rangeStart = currentPage * stepsPerPage;
                rangeEnd = Math.min(rangeStart + stepsPerPage, totalSteps);
            }
        }

        currentStep = rangeStart - 1;
        startTimeMs = System.currentTimeMillis();
        lastUpdateMs = startTimeMs;
    }

    public static void stop() {
        playing = false;
        currentStep = 0;
        autoStopped = false;
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
     * Retourne true si le mode non-looping a atteint la fin.
     * Le screen doit verifier ceci et envoyer STOP au serveur.
     */
    public static boolean shouldAutoStop() {
        return autoStopped;
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
        int rangeLen = rangeEnd - rangeStart;
        if (rangeLen <= 0) return;

        double msPerStep = 60000.0 / data.getBpm() / 4.0;
        long cycleMs = (long) (msPerStep * rangeLen);
        if (cycleMs <= 0) return;

        boolean isLooping = (playMode == PlayMode.LOOP || playMode == PlayMode.PAGE_LOOP);

        if (isLooping) {
            long elapsedInCycle = elapsed % cycleMs;
            int targetStep = rangeStart + (int) (elapsedInCycle / msPerStep);
            if (targetStep >= rangeEnd) targetStep = rangeEnd - 1;

            while (currentStep != targetStep) {
                currentStep++;
                if (currentStep >= rangeEnd) currentStep = rangeStart;
                playStep(currentStep, mc);
            }
        } else {
            int targetStep = rangeStart + (int) (elapsed / msPerStep);
            if (targetStep >= rangeEnd) {
                // Play remaining steps before stopping
                while (currentStep < rangeEnd - 1) {
                    currentStep++;
                    playStep(currentStep, mc);
                }
                autoStopped = true;
                return;
            }

            while (currentStep < targetStep) {
                currentStep++;
                if (currentStep >= rangeEnd) break;
                playStep(currentStep, mc);
            }
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
