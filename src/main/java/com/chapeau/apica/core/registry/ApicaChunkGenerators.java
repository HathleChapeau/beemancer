/**
 * ============================================================
 * [ApicaChunkGenerators.java]
 * Description: Registre des codecs de ChunkGenerator custom
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | ApicaChunkGenerator     | Codec à enregistrer  | Référence au CODEC statique    |
 * | Apica                   | MOD_ID               | Namespace du registre          |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - Apica.java (registerAllRegistries)
 *
 * ============================================================
 */
package com.chapeau.apica.core.registry;

import com.chapeau.apica.Apica;
import com.chapeau.apica.content.dimension.ApicaChunkGenerator;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ApicaChunkGenerators {

    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, Apica.MOD_ID);

    public static final Supplier<MapCodec<? extends ChunkGenerator>> APICA =
            GENERATORS.register("apica", () -> ApicaChunkGenerator.CODEC);

    public static void register(IEventBus eventBus) {
        GENERATORS.register(eventBus);
    }
}
