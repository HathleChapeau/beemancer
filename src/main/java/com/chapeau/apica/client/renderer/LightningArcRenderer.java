/**
 * ============================================================
 * [LightningArcRenderer.java]
 * Description: Génère et rend des arcs électriques (éclairs) entre deux points 3D
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | AnimationTimer      | Temps client         | Tick de création / calcul âge  |
 * | RenderType          | Type de rendu        | Custom type sans depth test    |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - MiningLaserItemRenderer.java (arcs entre les anneaux du grip)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer;

import com.chapeau.apica.Apica;
import com.chapeau.apica.client.animation.AnimationTimer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

/**
 * Utilitaire de génération et rendu d'arcs électriques (lightning).
 * Chaque arc est une poly-ligne entre un point de départ et d'arrivée,
 * avec des nœuds intermédiaires déplacés aléatoirement perpendiculairement à la ligne.
 * Rendu en 2 quads croisés par segment (X-shape) pour visibilité sous tous les angles.
 *
 * Utilise un RenderType custom emissif sans depth test pour garantir la visibilité
 * des arcs même lorsqu'ils sont géométriquement à l'intérieur d'un modèle.
 */
@OnlyIn(Dist.CLIENT)
public class LightningArcRenderer {

    /** Texture par défaut (copie du beam laser) */
    public static final ResourceLocation DEFAULT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/particle/lightning_arc.png");

    private static final int FULL_BRIGHT = 15728880;
    private static final Map<ResourceLocation, RenderType> TYPE_CACHE = new HashMap<>();

    // =========================================================================
    // Custom RenderType (emissif, sans depth test)
    // =========================================================================

    /**
     * Accès aux constantes protégées de RenderStateShard via héritage.
     * Le constructeur n'est jamais appelé — seules les méthodes statiques sont utilisées.
     */
    private static abstract class TypeAccess extends RenderType {
        private TypeAccess() {
            super("", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                    256, false, false, () -> {}, () -> {});
        }

        static RenderType create(ResourceLocation texture) {
            CompositeState state = CompositeState.builder()
                    .setShaderState(RENDERTYPE_EYES_SHADER)
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false);
            return RenderType.create("apica_lightning", DefaultVertexFormat.NEW_ENTITY,
                    VertexFormat.Mode.QUADS, 256, true, true, state);
        }
    }

    /** Retourne le RenderType lightning pour une texture (avec cache) */
    public static RenderType getLightningType(ResourceLocation texture) {
        return TYPE_CACHE.computeIfAbsent(texture, TypeAccess::create);
    }

    // =========================================================================
    // Arc data
    // =========================================================================

    /** Données d'un arc généré : points de la poly-ligne + paramètres de durée */
    public static class LightningArc {
        public final Vec3[] points;
        public final int tickCreated;
        public final int lifetime;
        public final boolean fadeIn;
        public final boolean fadeOut;

        public LightningArc(Vec3[] points, int tickCreated, int lifetime,
                             boolean fadeIn, boolean fadeOut) {
            this.points = points;
            this.tickCreated = tickCreated;
            this.lifetime = lifetime;
            this.fadeIn = fadeIn;
            this.fadeOut = fadeOut;
        }

        /** Calcule l'alpha courant avec fade in (premier 25%) et fade out (dernier 25%) */
        public float getAlpha(int currentTick, float baseAlpha) {
            int age = currentTick - tickCreated;
            if (age >= lifetime) return 0f;
            float progress = age / (float) lifetime;
            float alpha = baseAlpha;
            if (fadeIn && progress < 0.25f) alpha *= progress / 0.25f;
            if (fadeOut && progress > 0.75f) alpha *= (1f - progress) / 0.25f;
            return alpha;
        }

        /** Vérifie si l'arc a dépassé sa durée de vie */
        public boolean isExpired(int currentTick) {
            return (currentTick - tickCreated) >= lifetime;
        }
    }

    // =========================================================================
    // Génération
    // =========================================================================

    /**
     * Génère un arc électrique entre deux points.
     * Algorithme : divise la ligne en (nodeCount+1) segments, décale chaque nœud
     * de ±10% le long de la ligne, puis le déplace perpendiculairement de 0 à amplitude.
     *
     * @param start     Point de départ (fixe)
     * @param end       Point d'arrivée (fixe)
     * @param nodeCount Nombre de nœuds intermédiaires (min 0)
     * @param amplitude Déplacement perpendiculaire maximal
     * @param lifetime  Durée de vie en ticks
     * @param fadeIn    Fondu d'apparition sur le premier 25%
     * @param fadeOut   Fondu de disparition sur le dernier 25%
     * @param random    Source de hasard
     */
    public static LightningArc generateArc(Vec3 start, Vec3 end, int nodeCount, float amplitude,
                                            int lifetime, boolean fadeIn, boolean fadeOut,
                                            RandomSource random) {
        int totalPoints = nodeCount + 2;
        Vec3[] points = new Vec3[totalPoints];
        points[0] = start;
        points[totalPoints - 1] = end;

        if (nodeCount <= 0) {
            return new LightningArc(points, AnimationTimer.getTicks(), lifetime, fadeIn, fadeOut);
        }

        Vec3 direction = end.subtract(start);
        double length = direction.length();
        Vec3 dir = direction.normalize();

        Vec3 ref = Math.abs(dir.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 perpX = dir.cross(ref).normalize();
        Vec3 perpY = dir.cross(perpX).normalize();
        double segLength = length / (nodeCount + 1);

        for (int i = 1; i <= nodeCount; i++) {
            double t = (double) i / (nodeCount + 1);
            double lineOffset = (random.nextDouble() * 2.0 - 1.0) * 0.1 * segLength;
            Vec3 basePoint = start.add(direction.scale(t)).add(dir.scale(lineOffset));

            double angle = random.nextDouble() * Math.PI * 2.0;
            double displacement = random.nextDouble() * amplitude;
            Vec3 perpDir = perpX.scale(Math.cos(angle)).add(perpY.scale(Math.sin(angle)));
            points[i] = basePoint.add(perpDir.scale(displacement));
        }

        return new LightningArc(points, AnimationTimer.getTicks(), lifetime, fadeIn, fadeOut);
    }

    // =========================================================================
    // Rendu
    // =========================================================================

    /** Rend un arc avec la texture par défaut */
    public static void renderArc(PoseStack poseStack, MultiBufferSource buffer,
                                  LightningArc arc, float halfWidth,
                                  float r, float g, float b, float baseAlpha) {
        renderArc(poseStack, buffer, arc, halfWidth, r, g, b, baseAlpha, DEFAULT_TEXTURE);
    }

    /** Rend un arc avec une texture spécifique */
    public static void renderArc(PoseStack poseStack, MultiBufferSource buffer,
                                  LightningArc arc, float halfWidth,
                                  float r, float g, float b, float baseAlpha,
                                  ResourceLocation texture) {
        int currentTick = AnimationTimer.getTicks();
        float alpha = arc.getAlpha(currentTick, baseAlpha);
        if (alpha <= 0f) return;

        VertexConsumer vc = buffer.getBuffer(getLightningType(texture));
        PoseStack.Pose pose = poseStack.last();

        for (int i = 0; i < arc.points.length - 1; i++) {
            renderSegment(vc, pose, arc.points[i], arc.points[i + 1],
                    halfWidth, r, g, b, alpha);
        }
    }

    private static void renderSegment(VertexConsumer vc, PoseStack.Pose pose,
                                       Vec3 from, Vec3 to, float halfWidth,
                                       float r, float g, float b, float a) {
        Vec3 segDir = to.subtract(from);
        if (segDir.lengthSqr() < 0.00000001) return;
        Vec3 dir = segDir.normalize();

        Vec3 ref = Math.abs(dir.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 perpA = dir.cross(ref).normalize().scale(halfWidth);
        Vec3 perpB = dir.cross(perpA.normalize()).normalize().scale(halfWidth);

        emitQuad(vc, pose, from, to, perpA, r, g, b, a);
        emitQuad(vc, pose, from, to, perpB, r, g, b, a);
    }

    private static void emitQuad(VertexConsumer vc, PoseStack.Pose pose,
                                  Vec3 from, Vec3 to, Vec3 perpOffset,
                                  float r, float g, float b, float a) {
        Vec3 p0 = from.add(perpOffset);
        Vec3 p1 = from.subtract(perpOffset);
        Vec3 p2 = to.subtract(perpOffset);
        Vec3 p3 = to.add(perpOffset);
        Vec3 norm = perpOffset.normalize();
        float nx = (float) norm.x, ny = (float) norm.y, nz = (float) norm.z;
        int overlay = OverlayTexture.NO_OVERLAY;

        vc.addVertex(pose, (float) p0.x, (float) p0.y, (float) p0.z)
                .setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay)
                .setLight(FULL_BRIGHT).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, (float) p1.x, (float) p1.y, (float) p1.z)
                .setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay)
                .setLight(FULL_BRIGHT).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, (float) p2.x, (float) p2.y, (float) p2.z)
                .setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay)
                .setLight(FULL_BRIGHT).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, (float) p3.x, (float) p3.y, (float) p3.z)
                .setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay)
                .setLight(FULL_BRIGHT).setNormal(pose, nx, ny, nz);
    }
}
