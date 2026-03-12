/**
 * ============================================================
 * [MouseButtonTracker.java]
 * Description: Detecte la transition "mouse down" du clic droit
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | GLFW                | Etat souris          | glfwGetMouseButton()           |
 * | Minecraft           | Fenetre              | Recuperation handle GLFW       |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - IMagazineHolder.java (detection click down pour reload)
 * - ClientSetup.java (enregistrement tick event)
 *
 * ============================================================
 */
package com.chapeau.apica.client.input;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

/**
 * Trackeur d'etat du bouton droit de la souris.
 * Permet de detecter la transition DOWN (appui) vs HELD (maintenu).
 * Mis a jour chaque tick client via ClientTickEvent.
 */
@OnlyIn(Dist.CLIENT)
public final class MouseButtonTracker {

    private static boolean wasRightMouseDown = false;
    private static boolean isRightMouseDown = false;
    private static boolean rightMouseJustPressed = false;

    private MouseButtonTracker() {}

    /**
     * A appeler chaque tick client pour mettre a jour l'etat.
     */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return;

        long window = mc.getWindow().getWindow();
        wasRightMouseDown = isRightMouseDown;
        isRightMouseDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        rightMouseJustPressed = isRightMouseDown && !wasRightMouseDown;
    }

    /**
     * Retourne true si le bouton droit vient JUSTE d'etre enfonce ce tick.
     * False si maintenu ou relache.
     */
    public static boolean isRightMouseJustPressed() {
        return rightMouseJustPressed;
    }

    /**
     * Retourne true si le bouton droit est actuellement enfonce.
     */
    public static boolean isRightMouseDown() {
        return isRightMouseDown;
    }

    /**
     * Retourne true si le bouton droit etait enfonce au tick precedent.
     */
    public static boolean wasRightMouseDown() {
        return wasRightMouseDown;
    }
}
