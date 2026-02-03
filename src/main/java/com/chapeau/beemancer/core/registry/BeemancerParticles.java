/**
 * ============================================================
 * [BeemancerParticles.java]
 * Description: Registre des types de particules custom Beemancer
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Beemancer           | MOD_ID               | Namespace du registre          |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - Beemancer.java (enregistrement registre)
 * - ClientSetup.java (enregistrement providers)
 * - InfuserBlockEntity.java (spawn particules)
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.registry;

import com.chapeau.beemancer.Beemancer;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BeemancerParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
        DeferredRegister.create(Registries.PARTICLE_TYPE, Beemancer.MOD_ID);

    // --- Pixel particles ---
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HONEY_PIXEL =
        PARTICLES.register("honey_pixel", () -> new SimpleParticleType(false));

    public static void register(IEventBus bus) {
        PARTICLES.register(bus);
    }
}
