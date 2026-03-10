/**
 * ============================================================
 * [BlackHoleEffect.java]
 * Description: Effet Black Hole pour le Railgun (sphere + lignes rotatives)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | VfxEffect           | Base VFX             | Systeme de rendu               |
 * | VfxQuad             | Config quad          | Sphere + lignes                |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - RailgunItemRenderer.java (effet de charge)
 *
 * ============================================================
 */
package com.chapeau.apica.client.vfx;

import com.chapeau.apica.Apica;
import net.minecraft.resources.ResourceLocation;

/**
 * Effet Black Hole compose de:
 * - 1 sphere centrale (billboard, tourne sur elle-meme)
 * - 2 lignes billboard (suivent la camera, tournent)
 * - 4 lignes fixes (orientations differentes, tournent autour de leur axe)
 *
 * Chaque quad utilise depth test pour un rendu 3D correct.
 */
public class BlackHoleEffect extends VfxEffect {

    // Textures disponibles
    public static final ResourceLocation SPHERE =
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/particle/sphere.png");
    public static final ResourceLocation SPHERE_LESS_BLEND =
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/particle/sphere_less_blend.png");
    public static final ResourceLocation LINE =
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/particle/rounded_line.png");
    public static final ResourceLocation LINE_SMALL =
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/particle/rounded_line_small.png");

    private static final float PI = (float) Math.PI;

    public BlackHoleEffect() {
        setupDefaultConfiguration();
    }

    /**
     * Configuration par defaut du Black Hole:
     * - 1 sphere centrale billboard (violette, rotation lente)
     * - 2 lignes billboard (violet clair, rotation opposee)
     * - 4 lignes fixes sur axes differents (violet fonce, rotation variee)
     */
    private void setupDefaultConfiguration() {
        // === SPHERE CENTRALE (billboard) ===
        addQuad(VfxQuad.builder(SPHERE_TEXTURE)
            .billboard()
            .scale(0.2f)
            .rotationSpeed(0.03f)
            .color(0.6f, 0.2f, 1.0f, 0.95f)
            .build());

        // === LIGNES BILLBOARD (suivent la camera) ===
        addQuad(VfxQuad.builder(LINE_TEXTURE)
            .billboard()
            .scale(0.35f)
            .rotationSpeed(0.16f)
            .initialRotation(0)
            .color(0.7f, 0.3f, 1.0f, 0.8f)
            .build());

        addQuad(VfxQuad.builder(LINE_TEXTURE)
            .billboard()
            .scale(0.3f)
            .rotationSpeed(-0.12f)
            .initialRotation(PI / 2)
            .color(0.8f, 0.4f, 1.0f, 0.7f)
            .build());

        // === LIGNES FIXES (orientations differentes) ===
        // Axe X
        addQuad(VfxQuad.builder(LINE_TEXTURE)
            .fixed()
            .axis(1, 0, 0)
            .up(0, 1, 0)
            .scale(0.28f)
            .rotationSpeed(0.10f)
            .initialRotation(0)
            .color(0.5f, 0.15f, 0.9f, 0.6f)
            .build());

        // Axe Y
        addQuad(VfxQuad.builder(LINE_TEXTURE)
            .fixed()
            .axis(0, 1, 0)
            .up(0, 0, 1)
            .scale(0.26f)
            .rotationSpeed(-0.14f)
            .initialRotation(PI / 4)
            .color(0.5f, 0.15f, 0.9f, 0.6f)
            .build());

        // Axe Z
        addQuad(VfxQuad.builder(LINE_TEXTURE)
            .fixed()
            .axis(0, 0, 1)
            .up(0, 1, 0)
            .scale(0.25f)
            .rotationSpeed(0.08f)
            .initialRotation(PI / 3)
            .color(0.5f, 0.15f, 0.9f, 0.6f)
            .build());

        // Axe diagonal
        addQuad(VfxQuad.builder(LINE_TEXTURE)
            .fixed()
            .axis(1, 1, 0)
            .up(0, 0, 1)
            .scale(0.22f)
            .rotationSpeed(-0.12f)
            .initialRotation(PI / 6)
            .color(0.5f, 0.15f, 0.9f, 0.5f)
            .build());
    }

    /**
     * Builder pour creer un BlackHoleEffect personnalise.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final BlackHoleEffect effect = new BlackHoleEffect();
        private boolean cleared = false;

        private Builder() {}

        /** Supprime la configuration par defaut. */
        public Builder clear() {
            if (!cleared) {
                effect.quads.clear();
                cleared = true;
            }
            return this;
        }

        /** Ajoute une sphere billboard. */
        public SphereBuilder addSphere() {
            return new SphereBuilder(this);
        }

        /** Ajoute une ligne billboard. */
        public LineBuilder addBillboardLine() {
            return new LineBuilder(this, true);
        }

        /** Ajoute une ligne fixe. */
        public LineBuilder addFixedLine() {
            return new LineBuilder(this, false);
        }

        void addQuadInternal(VfxQuad quad) {
            effect.addQuad(quad);
        }

        public BlackHoleEffect build() {
            return effect;
        }
    }

    public static class SphereBuilder {
        private final Builder parent;
        private ResourceLocation texture = SPHERE;
        private float scale = 0.15f;
        private float rotationSpeed = 0.05f;
        private float initialRotation = 0;
        private float r = 0.8f, g = 0.4f, b = 1.0f, a = 0.9f;

        private SphereBuilder(Builder parent) {
            this.parent = parent;
        }

        public SphereBuilder texture(ResourceLocation tex) { this.texture = tex; return this; }
        public SphereBuilder lessBlend() { this.texture = SPHERE_LESS_BLEND; return this; }
        public SphereBuilder scale(float scale) { this.scale = scale; return this; }
        public SphereBuilder rotationSpeed(float speed) { this.rotationSpeed = speed; return this; }
        public SphereBuilder initialRotation(float rot) { this.initialRotation = rot; return this; }
        public SphereBuilder color(float r, float g, float b, float a) {
            this.r = r; this.g = g; this.b = b; this.a = a;
            return this;
        }

        public Builder done() {
            parent.addQuadInternal(VfxQuad.builder(texture)
                .billboard()
                .scale(scale)
                .rotationSpeed(rotationSpeed)
                .initialRotation(initialRotation)
                .color(r, g, b, a)
                .build());
            return parent;
        }
    }

    public static class LineBuilder {
        private final Builder parent;
        private final boolean billboard;
        private ResourceLocation texture = LINE;
        private float scale = 0.25f;
        private float rotationSpeed = 0.1f;
        private float initialRotation = 0;
        private float axisX = 1, axisY = 0, axisZ = 0;
        private float upX = 0, upY = 1, upZ = 0;
        private float r = 0.6f, g = 0.2f, b = 1.0f, a = 0.7f;

        private LineBuilder(Builder parent, boolean billboard) {
            this.parent = parent;
            this.billboard = billboard;
        }

        public LineBuilder texture(ResourceLocation tex) { this.texture = tex; return this; }
        public LineBuilder small() { this.texture = LINE_SMALL; return this; }
        public LineBuilder scale(float scale) { this.scale = scale; return this; }
        public LineBuilder rotationSpeed(float speed) { this.rotationSpeed = speed; return this; }
        public LineBuilder initialRotation(float rot) { this.initialRotation = rot; return this; }
        public LineBuilder axis(float x, float y, float z) {
            this.axisX = x; this.axisY = y; this.axisZ = z;
            return this;
        }
        public LineBuilder up(float x, float y, float z) {
            this.upX = x; this.upY = y; this.upZ = z;
            return this;
        }
        public LineBuilder color(float r, float g, float b, float a) {
            this.r = r; this.g = g; this.b = b; this.a = a;
            return this;
        }

        public Builder done() {
            VfxQuad.Builder qb = VfxQuad.builder(texture)
                .scale(scale)
                .rotationSpeed(rotationSpeed)
                .initialRotation(initialRotation)
                .color(r, g, b, a);

            if (billboard) {
                qb.billboard();
            } else {
                qb.fixed().axis(axisX, axisY, axisZ).up(upX, upY, upZ);
            }

            parent.addQuadInternal(qb.build());
            return parent;
        }
    }
}
