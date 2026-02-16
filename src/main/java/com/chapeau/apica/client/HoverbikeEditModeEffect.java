/**
 * ============================================================
 * [HoverbikeEditModeEffect.java]
 * Description: Copie la silhouette entity_outline vers le masque du shader edit mode
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikeEntity     | Etat edit mode       | Verification shader actif      |
 * | PostChain           | Shader pipeline      | Acces au target "mask"         |
 * | LevelRenderer       | entity_outline       | Source de la silhouette         |
 * | OutlineBufferSource | Flush outline batch   | Force GPU write avant blit     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java: Enregistrement event handler
 *
 * ============================================================
 */
package com.chapeau.apica.client;

import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.lwjgl.opengl.GL30;

/**
 * Copie le buffer entity_outline (silhouette des entites glow) vers le target "mask"
 * du shader edit mode. Appele a AFTER_ENTITIES, avant que le PostChain entity_outline
 * ne transforme la silhouette brute en contour.
 *
 * IMPORTANT: A AFTER_ENTITIES, les draw calls entity_outline sont encore dans le buffer
 * de OutlineBufferSource (non flushed vers le GPU). On doit appeler endOutlineBatch()
 * pour forcer l'ecriture dans entityTarget AVANT de copier vers le masque.
 * Le fragment shader utilise ensuite ce masque pour preserver les pixels de la moto.
 */
@OnlyIn(Dist.CLIENT)
public class HoverbikeEditModeEffect {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (!HoverbikeEntity.isEditShaderActive()) return;

        Minecraft mc = Minecraft.getInstance();

        // Forcer le flush des draw calls outline vers le GPU.
        // A AFTER_ENTITIES, les entites sont rendues mais les outline draw calls
        // sont encore batches dans OutlineBufferSource. Sans ce flush,
        // entityTarget() est vide et le masque ne contient aucune donnee.
        OutlineBufferSource outlineBS = mc.renderBuffers().outlineBufferSource();
        outlineBS.endOutlineBatch();

        // Source : entity_outline (silhouette brute des entites glow)
        RenderTarget entityOutline = mc.levelRenderer.entityTarget();
        if (entityOutline == null) return;

        // Destination : target "mask" dans notre PostChain
        PostChain chain = mc.gameRenderer.currentEffect();
        if (chain == null) return;

        RenderTarget mask = chain.getTempTarget("mask");
        if (mask == null) return;

        // Blit : copie les pixels de entity_outline vers mask
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, entityOutline.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, mask.frameBufferId);
        GL30.glBlitFramebuffer(
                0, 0, entityOutline.width, entityOutline.height,
                0, 0, mask.width, mask.height,
                GL30.GL_COLOR_BUFFER_BIT, GL30.GL_NEAREST
        );

        // Restaurer le framebuffer principal
        mc.getMainRenderTarget().bindWrite(true);
    }
}
