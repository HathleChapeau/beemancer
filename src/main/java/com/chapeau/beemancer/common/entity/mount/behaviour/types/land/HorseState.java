/**
 * ============================================================
 * [HorseState.java]
 * Description: État mutable pour le comportement Horse (terrestre)
 * ============================================================
 *
 * PATTERN: Copié de Cobblemon HorseBehaviour.kt L615-667 (HorseState class)
 * - États spécifiques au comportement terrestre
 * - sprinting, walking, inAir, jumpTicks
 *
 * UTILISÉ PAR:
 * - HorseBehaviour.java: Lecture/écriture des états
 * - RidingController.java: Stocké dans contexte
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.mount.behaviour.types.land;

import com.chapeau.beemancer.common.entity.mount.behaviour.RidingBehaviourState;
import com.chapeau.beemancer.common.entity.mount.behaviour.Side;
import com.chapeau.beemancer.common.entity.mount.behaviour.SidedRidingState;
import net.minecraft.network.FriendlyByteBuf;

/**
 * État mutable pour le comportement Horse.
 * Pattern Cobblemon: HorseState
 */
public class HorseState extends RidingBehaviourState {

    /**
     * Est-ce que le ride sprinte?
     * CLIENT only - basé sur input sprint.
     */
    private final SidedRidingState<Boolean> sprinting;

    /**
     * Est-ce que le ride marche (vélocité > 0)?
     * BOTH - utilisé pour les animations.
     */
    private final SidedRidingState<Boolean> walking;

    /**
     * Sprint toggleable (anti-spam de sprint).
     * CLIENT only.
     */
    private final SidedRidingState<Boolean> sprintToggleable;

    /**
     * Est-ce que le ride est en l'air?
     * CLIENT only - basé sur détection sol.
     */
    private final SidedRidingState<Boolean> inAir;

    /**
     * Compteur de ticks pour le saut.
     * Positif = en cours de saut (force appliquée)
     * Négatif = cooldown avant prochain saut
     * CLIENT only.
     */
    private final SidedRidingState<Integer> jumpTicks;

    public HorseState() {
        super();
        this.sprinting = new SidedRidingState<>(false, Side.CLIENT);
        this.walking = new SidedRidingState<>(false, Side.BOTH);
        this.sprintToggleable = new SidedRidingState<>(false, Side.CLIENT);
        this.inAir = new SidedRidingState<>(false, Side.CLIENT);
        this.jumpTicks = new SidedRidingState<>(0, Side.CLIENT);
    }

    // --- Getters ---

    public boolean isSprinting() {
        return sprinting.get();
    }

    public void setSprinting(boolean value) {
        sprinting.set(value);
    }

    public boolean isWalking() {
        return walking.get();
    }

    public void setWalking(boolean value) {
        walking.set(value);
    }

    public boolean isSprintToggleable() {
        return sprintToggleable.get();
    }

    public void setSprintToggleable(boolean value) {
        sprintToggleable.set(value);
    }

    public boolean isInAir() {
        return inAir.get();
    }

    public void setInAir(boolean value) {
        inAir.set(value);
    }

    public int getJumpTicks() {
        return jumpTicks.get();
    }

    public void setJumpTicks(int value) {
        jumpTicks.set(value);
    }

    // --- Lifecycle ---

    @Override
    public void reset() {
        super.reset();
        sprinting.set(false, true);
        walking.set(false, true);
        sprintToggleable.set(false, true);
        inAir.set(false, true);
        jumpTicks.set(0, true);
    }

    @Override
    public RidingBehaviourState copy() {
        HorseState copy = new HorseState();
        copy.setRideVelocity(this.getRideVelocity(), true);
        copy.setStamina(this.getStamina(), true);
        copy.sprinting.set(this.sprinting.get(), true);
        copy.walking.set(this.walking.get(), true);
        copy.sprintToggleable.set(this.sprintToggleable.get(), true);
        copy.inAir.set(this.inAir.get(), true);
        copy.jumpTicks.set(this.jumpTicks.get(), true);
        return copy;
    }

    @Override
    public boolean shouldSync(RidingBehaviourState previous) {
        if (!(previous instanceof HorseState prev)) return false;
        if (prev.sprinting.get() != sprinting.get()) return true;
        if (prev.walking.get() != walking.get()) return true;
        if (prev.inAir.get() != inAir.get()) return true;
        return super.shouldSync(previous);
    }

    // --- Network ---

    @Override
    public void encode(FriendlyByteBuf buffer) {
        super.encode(buffer);
        buffer.writeBoolean(sprinting.get());
        buffer.writeBoolean(walking.get());
        buffer.writeBoolean(sprintToggleable.get());
        buffer.writeBoolean(inAir.get());
        buffer.writeInt(jumpTicks.get());
    }

    @Override
    public void decode(FriendlyByteBuf buffer) {
        super.decode(buffer);
        sprinting.set(buffer.readBoolean(), true);
        walking.set(buffer.readBoolean(), true);
        sprintToggleable.set(buffer.readBoolean(), true);
        inAir.set(buffer.readBoolean(), true);
        jumpTicks.set(buffer.readInt(), true);
    }
}
