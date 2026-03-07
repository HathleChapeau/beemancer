/**
 * ============================================================
 * [ApicaRenderTypes.java]
 * Description: Custom RenderTypes pour le rendu Apica (fluide derriere verre translucent)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | RenderStateShard        | Acces protected      | Shader, texture, transparency  |
 * | RenderType              | Creation composite   | FLUID_TRANSLUCENT              |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoneyTankRenderer.java
 * - HoneyReservoirRenderer.java
 * - MultiblockTankRenderer.java
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.util;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/**
 * Custom render types pour Apica.
 * Etend RenderStateShard pour acceder aux constantes protegees (shader, texture, etc.).
 */
public final class ApicaRenderTypes extends RenderStateShard {

    private ApicaRenderTypes() {
        super("apica_dummy", () -> {}, () -> {});
    }

    /**
     * RenderType translucent sans depth test ni depth write.
     * Utilise pour le rendu de fluide a l'interieur de conteneurs translucents (verre).
     * Le depth test est desactive (GL_ALWAYS) pour que le fluide ne soit pas masque
     * par la vitre qui a deja ecrit dans le depth buffer.
     * Le depth write est desactive pour ne pas affecter le rendu subsequent.
     */
    public static final RenderType FLUID_TRANSLUCENT = RenderType.create(
        "apica_fluid_translucent",
        DefaultVertexFormat.BLOCK,
        VertexFormat.Mode.QUADS,
        256 * 1024,
        false,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_TRANSLUCENT_SHADER)
            .setTextureState(BLOCK_SHEET_MIPPED)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(LIGHTMAP)
            .setDepthTestState(NO_DEPTH_TEST)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(false)
    );
}
