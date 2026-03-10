/**
 * ============================================================
 * [VfxQuad.java]
 * Description: Configuration d'un quad VFX avec parametres fluents
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ResourceLocation    | Texture              | Chemin de la texture           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - VfxEffect.java (liste de quads)
 * - BlackHoleEffect.java (configuration)
 *
 * ============================================================
 */
package com.chapeau.apica.client.vfx;

import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

/**
 * Configuration immutable d'un quad VFX.
 * Deux modes:
 * - Billboard: face toujours la camera, tourne autour de l'axe de vue
 * - Fixed: orientation fixe dans l'espace monde, tourne autour d'un axe defini
 */
public class VfxQuad {

    public enum Mode { BILLBOARD, FIXED }

    private final ResourceLocation texture;
    private final Mode mode;
    private final float scale;
    private final float rotationSpeed;
    private final float initialRotation;
    private final Vector3f fixedAxis;
    private final Vector3f fixedUp;
    private final float r, g, b, a;

    private VfxQuad(Builder builder) {
        this.texture = builder.texture;
        this.mode = builder.mode;
        this.scale = builder.scale;
        this.rotationSpeed = builder.rotationSpeed;
        this.initialRotation = builder.initialRotation;
        this.fixedAxis = builder.fixedAxis;
        this.fixedUp = builder.fixedUp;
        this.r = builder.r;
        this.g = builder.g;
        this.b = builder.b;
        this.a = builder.a;
    }

    public ResourceLocation texture() { return texture; }
    public Mode mode() { return mode; }
    public float scale() { return scale; }
    public float rotationSpeed() { return rotationSpeed; }
    public float initialRotation() { return initialRotation; }
    public Vector3f fixedAxis() { return fixedAxis; }
    public Vector3f fixedUp() { return fixedUp; }
    public float r() { return r; }
    public float g() { return g; }
    public float b() { return b; }
    public float a() { return a; }

    public static Builder builder(ResourceLocation texture) {
        return new Builder(texture);
    }

    public static class Builder {
        private final ResourceLocation texture;
        private Mode mode = Mode.BILLBOARD;
        private float scale = 1.0f;
        private float rotationSpeed = 0.0f;
        private float initialRotation = 0.0f;
        private Vector3f fixedAxis = new Vector3f(0, 0, 1);
        private Vector3f fixedUp = new Vector3f(0, 1, 0);
        private float r = 1.0f, g = 1.0f, b = 1.0f, a = 1.0f;

        private Builder(ResourceLocation texture) {
            this.texture = texture;
        }

        /** Mode billboard: face toujours la camera. */
        public Builder billboard() {
            this.mode = Mode.BILLBOARD;
            return this;
        }

        /** Mode fixed: orientation fixe dans l'espace monde. */
        public Builder fixed() {
            this.mode = Mode.FIXED;
            return this;
        }

        /** Echelle du quad. */
        public Builder scale(float scale) {
            this.scale = scale;
            return this;
        }

        /** Vitesse de rotation en radians/tick. */
        public Builder rotationSpeed(float speed) {
            this.rotationSpeed = speed;
            return this;
        }

        /** Rotation initiale en radians. */
        public Builder initialRotation(float rotation) {
            this.initialRotation = rotation;
            return this;
        }

        /** Axe de la normale du quad (mode FIXED). */
        public Builder axis(float x, float y, float z) {
            this.fixedAxis = new Vector3f(x, y, z).normalize();
            return this;
        }

        /** Vecteur up du quad (mode FIXED). */
        public Builder up(float x, float y, float z) {
            this.fixedUp = new Vector3f(x, y, z).normalize();
            return this;
        }

        /** Couleur RGBA. */
        public Builder color(float r, float g, float b, float a) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            return this;
        }

        /** Couleur RGB (alpha = 1). */
        public Builder color(float r, float g, float b) {
            return color(r, g, b, 1.0f);
        }

        public VfxQuad build() {
            return new VfxQuad(this);
        }
    }
}
