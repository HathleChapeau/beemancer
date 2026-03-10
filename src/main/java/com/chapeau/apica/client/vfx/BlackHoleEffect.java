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

    private static final ResourceLocation SPHERE_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/vfx/sphere.png");
    private static final ResourceLocation LINE_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/vfx/rounded_line.png");

    private static final float PI = (float) Math.PI;

    public BlackHoleEffect() {
        setupDefaultConfiguration();
    }

    /**
     * Configuration DEBUG: quads sur tous les axes pour tester la visibilite.
     * Scale reduit (0.15) pour espace modele item.
     */
    private void setupDefaultConfiguration() {
        // === DEBUG: 6 quads sur chaque axe + 1 billboard ===
        float s = 0.15f; // Scale adapte a l'espace modele

        // Axe Z (face camera normalement) - ROUGE
        addQuad(VfxQuad.builder(SPHERE_TEXTURE)
            .fixed()
            .axis(0, 0, 1)
            .up(0, 1, 0)
            .scale(s)
            .rotationSpeed(0.1f)
            .color(1.0f, 0.0f, 0.0f, 1.0f)
            .build());

        // Axe Z inverse - ROUGE FONCE
        addQuad(VfxQuad.builder(SPHERE_TEXTURE)
            .fixed()
            .axis(0, 0, -1)
            .up(0, 1, 0)
            .scale(s)
            .rotationSpeed(-0.1f)
            .color(0.5f, 0.0f, 0.0f, 1.0f)
            .build());

        // Axe X - VERT
        addQuad(VfxQuad.builder(SPHERE_TEXTURE)
            .fixed()
            .axis(1, 0, 0)
            .up(0, 1, 0)
            .scale(s)
            .rotationSpeed(0.1f)
            .color(0.0f, 1.0f, 0.0f, 1.0f)
            .build());

        // Axe X inverse - VERT FONCE
        addQuad(VfxQuad.builder(SPHERE_TEXTURE)
            .fixed()
            .axis(-1, 0, 0)
            .up(0, 1, 0)
            .scale(s)
            .rotationSpeed(-0.1f)
            .color(0.0f, 0.5f, 0.0f, 1.0f)
            .build());

        // Axe Y - BLEU
        addQuad(VfxQuad.builder(SPHERE_TEXTURE)
            .fixed()
            .axis(0, 1, 0)
            .up(0, 0, 1)
            .scale(s)
            .rotationSpeed(0.1f)
            .color(0.0f, 0.0f, 1.0f, 1.0f)
            .build());

        // Axe Y inverse - BLEU FONCE
        addQuad(VfxQuad.builder(SPHERE_TEXTURE)
            .fixed()
            .axis(0, -1, 0)
            .up(0, 0, 1)
            .scale(s)
            .rotationSpeed(-0.1f)
            .color(0.0f, 0.0f, 0.5f, 1.0f)
            .build());

        // Billboard test - BLANC (plus petit)
        addQuad(VfxQuad.builder(SPHERE_TEXTURE)
            .billboard()
            .scale(s * 0.7f)
            .rotationSpeed(0.2f)
            .color(1.0f, 1.0f, 1.0f, 1.0f)
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
        private float scale = 0.15f;
        private float rotationSpeed = 0.05f;
        private float initialRotation = 0;
        private float r = 0.8f, g = 0.4f, b = 1.0f, a = 0.9f;

        private SphereBuilder(Builder parent) {
            this.parent = parent;
        }

        public SphereBuilder scale(float scale) { this.scale = scale; return this; }
        public SphereBuilder rotationSpeed(float speed) { this.rotationSpeed = speed; return this; }
        public SphereBuilder initialRotation(float rot) { this.initialRotation = rot; return this; }
        public SphereBuilder color(float r, float g, float b, float a) {
            this.r = r; this.g = g; this.b = b; this.a = a;
            return this;
        }

        public Builder done() {
            parent.addQuadInternal(VfxQuad.builder(SPHERE_TEXTURE)
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
            VfxQuad.Builder qb = VfxQuad.builder(LINE_TEXTURE)
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
