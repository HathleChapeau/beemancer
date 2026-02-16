/**
 * ============================================================
 * [ApicaParticles.java]
 * Description: Registre des types de particules custom Apica
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Apica           | MOD_ID               | Namespace du registre          |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - Apica.java (enregistrement registre)
 * - ClientSetup.java (enregistrement providers)
 * - InfuserBlockEntity.java (spawn particules)
 *
 * ============================================================
 */
package com.chapeau.apica.core.registry;

import com.chapeau.apica.Apica;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ApicaParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
        DeferredRegister.create(Registries.PARTICLE_TYPE, Apica.MOD_ID);

    // --- Pixel particles ---
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HONEY_PIXEL =
        PARTICLES.register("honey_pixel", () -> new SimpleParticleType(false));

    // --- Rune particles (glyphes enchantement, gravite inversee) ---
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RUNE =
        PARTICLES.register("rune", () -> new SimpleParticleType(false));

    public static void register(IEventBus bus) {
        PARTICLES.register(bus);
    }
}
