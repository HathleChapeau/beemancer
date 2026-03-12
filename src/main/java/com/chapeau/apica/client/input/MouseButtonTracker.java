/**
 * ============================================================
 * [MouseButtonTracker.java]
 * Description: Track mouse button state pour reload magazine
 * ============================================================
 */
package com.chapeau.apica.client.input;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public final class MouseButtonTracker {

    private static boolean wasDown = false;
    private static boolean isDown = false;
    private static boolean blocked = false;

    private MouseButtonTracker() {}

    public static void tick() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        wasDown = isDown;
        isDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        // Debloquer au relachement
        if (!isDown) {
            blocked = false;
        }
    }

    /** True si click DOWN ce tick (pas maintenu) ET pas bloque. */
    public static boolean canReload() {
        return isDown && !wasDown && !blocked;
    }

    /** True si bloque (reload deja fait, attendre relachement). */
    public static boolean isBlocked() {
        return blocked;
    }

    /** Bloquer jusqu'au relachement. */
    public static void block() {
        blocked = true;
    }
}
