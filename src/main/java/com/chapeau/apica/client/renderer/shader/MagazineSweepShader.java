/**
 * ============================================================
 * [MagazineSweepShader.java]
 * Description: Gestionnaire du shader de bande animee pour les magazine holders
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ShaderInstance      | Instance shader      | Chargement et uniforms         |
 * | RenderType          | Type de rendu        | Creation RenderType custom     |
 * | DebugWandItem       | Valeurs debug        | Speed, angle, width            |
 * | MagazineData        | Donnees fluide       | Couleur basee sur fluide       |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java: Enregistrement shader
 * - MiningLaserItemRenderer.java: Rendu avec shader
 * - LeafBlowerItemRenderer.java: Rendu avec shader
 * - ChopperHiveItemRenderer.java: Rendu avec shader
 * - BuildingStaffItemRenderer.java: Rendu avec shader
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.shader;

import com.chapeau.apica.Apica;
import com.chapeau.apica.client.animation.AnimationTimer;
import com.chapeau.apica.common.item.debug.DebugWandItem;
import com.chapeau.apica.common.item.magazine.MagazineData;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Vector3f;

import java.io.IOException;

/**
 * Gestionnaire du shader magazine_sweep.
 * Applique une bande lumineuse animee sur les items magazine holders.
 * La couleur depend du fluide contenu (Miel=dore, Royal=creme, Nectar=violet).
 * Les parametres (speed, angle, width) sont controles via DebugWandItem.
 */
@OnlyIn(Dist.CLIENT)
public class MagazineSweepShader {

    private static ShaderInstance shaderInstance;

    // Couleurs fluides (RGB normalise 0-1)
    private static final Vector3f HONEY_COLOR = new Vector3f(0.91f, 0.64f, 0.09f);    // #E8A317
    private static final Vector3f ROYAL_COLOR = new Vector3f(1.0f, 0.97f, 0.86f);     // #FFF8DC
    private static final Vector3f NECTAR_COLOR = new Vector3f(0.69f, 0.31f, 1.0f);    // #B050FF
    private static final Vector3f EMPTY_COLOR = new Vector3f(0.53f, 0.53f, 0.53f);    // #888888

    // Fluid IDs
    private static final String HONEY_ID = "apica:honey";
    private static final String ROYAL_ID = "apica:royal_jelly";
    private static final String NECTAR_ID = "apica:nectar";

    // Defaults
    private static final float DEFAULT_SPEED = 1.0f;
    private static final float DEFAULT_ANGLE = 0.0f;
    private static final float DEFAULT_WIDTH = 0.03f;

    /**
     * Enregistre le shader lors de l'evenement RegisterShadersEvent.
     * Appele depuis ClientSetup.
     */
    public static void register(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "magazine_sweep"),
                        DefaultVertexFormat.NEW_ENTITY
                ),
                shader -> shaderInstance = shader
        );
        Apica.LOGGER.info("Registered magazine_sweep shader");
    }

    /**
     * Retourne true si le shader est pret a etre utilise.
     */
    public static boolean isAvailable() {
        return shaderInstance != null;
    }

    // Cache du RenderType par texture
    private static final java.util.Map<ResourceLocation, RenderType> RENDER_TYPE_CACHE = new java.util.HashMap<>();

    /**
     * Cree un RenderType utilisant le shader magazine_sweep.
     *
     * @param texture Texture de l'item
     * @param holderStack ItemStack du holder (pour determiner la couleur du fluide)
     * @return RenderType configure avec le shader et les uniforms
     */
    public static RenderType getRenderType(ResourceLocation texture, ItemStack holderStack) {
        if (shaderInstance == null) {
            // Fallback si shader pas charge
            return RenderType.entityTranslucentCull(texture);
        }

        // Mettre a jour les uniforms AVANT de retourner le RenderType
        updateUniforms(holderStack);

        // Utiliser un RenderType cache pour eviter de recreer a chaque frame
        return RENDER_TYPE_CACHE.computeIfAbsent(texture, tex -> RenderType.create(
                "apica:magazine_sweep",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                1536,
                true,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(() -> shaderInstance))
                        .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setLightmapState(RenderStateShard.LIGHTMAP)
                        .setOverlayState(RenderStateShard.OVERLAY)
                        .createCompositeState(true)
        ));
    }

    /**
     * Met a jour les uniforms du shader. Doit etre appele chaque frame APRES avoir obtenu le buffer.
     */
    public static void applyUniforms(ItemStack holderStack) {
        updateUniforms(holderStack);
    }

    /**
     * Met a jour les uniforms du shader avec les valeurs actuelles.
     */
    private static void updateUniforms(ItemStack holderStack) {
        if (shaderInstance == null) return;

        // GameTime doit etre mis a jour manuellement pour les shaders custom
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        float gameTime = AnimationTimer.getRenderTime(partialTick);
        setUniform("GameTime", gameTime);

        // Speed depuis DebugWandItem.value1 (default 1.0)
        float speed = DebugWandItem.value1 > 0.01f ? DebugWandItem.value1 : DEFAULT_SPEED;
        setUniform("SweepSpeed", speed);

        // Angle depuis DebugWandItem.value2 (default 0.0 = vertical)
        float angle = DebugWandItem.value2;
        setUniform("SweepAngle", angle);

        // Width depuis DebugWandItem.value3 (default 0.15)
        float width = DebugWandItem.value3 > 0.01f ? DebugWandItem.value3 : DEFAULT_WIDTH;
        setUniform("BandWidth", width);

        // Couleur basee sur le fluide du magazine
        Vector3f color = getFluidColor(holderStack);
        setUniform("FluidColor", color.x, color.y, color.z);
    }

    /**
     * Determine la couleur du fluide contenu dans le holder.
     */
    private static Vector3f getFluidColor(ItemStack holderStack) {
        if (holderStack.isEmpty()) return EMPTY_COLOR;

        String fluidId = MagazineData.getFluidId(holderStack);
        if (fluidId.isEmpty()) return EMPTY_COLOR;

        return switch (fluidId) {
            case HONEY_ID -> HONEY_COLOR;
            case ROYAL_ID -> ROYAL_COLOR;
            case NECTAR_ID -> NECTAR_COLOR;
            default -> EMPTY_COLOR;
        };
    }

    // ========== Uniform helpers ==========

    private static void setUniform(String name, float value) {
        if (shaderInstance == null) return;
        var uniform = shaderInstance.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }

    private static void setUniform(String name, float x, float y, float z) {
        if (shaderInstance == null) return;
        var uniform = shaderInstance.getUniform(name);
        if (uniform != null) {
            uniform.set(x, y, z);
        }
    }
}
