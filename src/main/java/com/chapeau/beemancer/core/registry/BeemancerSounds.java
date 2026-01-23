/**
 * ============================================================
 * [BeemancerSounds.java]
 * Description: Registre des sons du mod Beemancer
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | SoundEvent          | Événements sonores   | Définition des sons            |
 * | DeferredRegister    | Enregistrement       | Pattern NeoForge               |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - CodexScreen (sons de navigation et déblocage)
 * - Autres features nécessitant des sons
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.registry;

import com.chapeau.beemancer.Beemancer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class BeemancerSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
        DeferredRegister.create(Registries.SOUND_EVENT, Beemancer.MOD_ID);

    // --- Codex Sounds ---
    public static final Supplier<SoundEvent> CODEX_OPEN = registerSound("codex_open");
    public static final Supplier<SoundEvent> CODEX_CLOSE = registerSound("codex_close");
    public static final Supplier<SoundEvent> CODEX_PAGE_TURN = registerSound("codex_page_turn");
    public static final Supplier<SoundEvent> CODEX_NODE_UNLOCK = registerSound("codex_node_unlock");
    public static final Supplier<SoundEvent> CODEX_NODE_CLICK = registerSound("codex_node_click");

    private static Supplier<SoundEvent> registerSound(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}
