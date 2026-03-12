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

    private MouseButtonTracker() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return;

        long window = mc.getWindow().getWindow();
        wasDown = isDown;
        isDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        // Mouse UP: reset reload state
        if (wasDown && !isDown) {
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

    /** True si click DOWN (lecture directe GLFW, pas cached). */
    public static boolean isMouseDown() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return false;
        long window = mc.getWindow().getWindow();
        boolean currentlyDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        // DOWN = appuyé maintenant ET pas appuyé au dernier tick
        return currentlyDown && !wasDown;
    }

    /** True si bouton actuellement enfoncé. */
    public static boolean isMouseHeld() {
        return isDown;
    }
}
