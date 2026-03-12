/**
 * ============================================================
 * [MouseButtonTracker.java]
 * Description: Track mouse button state, reset reload on mouse UP
 * ============================================================
 */
package com.chapeau.apica.client.input;

import com.chapeau.apica.common.item.magazine.IMagazineHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public final class MouseButtonTracker {

    private static boolean wasDown = false;
    private static boolean isDown = false;
    private static boolean consumed = false;  // true après un reload, reset au mouse UP

    private MouseButtonTracker() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return;

        long window = mc.getWindow().getWindow();
        wasDown = isDown;
        isDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        // Mouse UP: reset consumed et reload state
        if (wasDown && !isDown) {
            consumed = false;
            LocalPlayer player = mc.player;
            if (player != null) {
                ItemStack mainHand = player.getMainHandItem();
                ItemStack offHand = player.getOffhandItem();

                if (mainHand.getItem() instanceof IMagazineHolder holder) {
                    holder.resetReload(player);
                }
                if (offHand.getItem() instanceof IMagazineHolder holder) {
                    holder.resetReload(player);
                }
            }
        }
    }

    /** True si click DOWN et pas encore consommé. */
    public static boolean isMouseDown() {
        if (consumed) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return false;
        long window = mc.getWindow().getWindow();
        boolean currentlyDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        return currentlyDown && !wasDown;
    }

    /** Marquer le click comme consommé (après reload). */
    public static void consume() {
        consumed = true;
    }

    /** True si bouton actuellement enfoncé. */
    public static boolean isMouseHeld() {
        return isDown;
    }
}
