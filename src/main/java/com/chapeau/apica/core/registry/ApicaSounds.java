/**
 * ============================================================
 * [ApicaSounds.java]
 * Description: Registre des sons du mod Apica
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | SoundEvent          | Événements sonores   | Définition des sons            |
 * | SoundEvents         | Sons vanilla         | Réutilisation sons Minecraft   |
 * | DeferredRegister    | Enregistrement       | Pattern NeoForge               |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - CodexScreen (sons de navigation et déblocage)
 * - CodexBookScreen (sons de pages)
 * - CodexItem (son d'ouverture)
 *
 * ============================================================
 */
package com.chapeau.apica.core.registry;

import com.chapeau.apica.Apica;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ApicaSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
        DeferredRegister.create(Registries.SOUND_EVENT, Apica.MOD_ID);

    // --- Codex Sounds (vanilla) ---
    public static final Supplier<SoundEvent> CODEX_OPEN = () -> SoundEvents.BOOK_PAGE_TURN;
    public static final Supplier<SoundEvent> CODEX_CLOSE = () -> SoundEvents.BOOK_PAGE_TURN;
    public static final Supplier<SoundEvent> CODEX_PAGE_TURN = () -> SoundEvents.BOOK_PAGE_TURN;
    public static final Supplier<SoundEvent> CODEX_NODE_UNLOCK = () -> SoundEvents.EXPERIENCE_ORB_PICKUP;
    public static final Supplier<SoundEvent> CODEX_NODE_CLICK = () -> SoundEvents.UI_BUTTON_CLICK.value();

    private static Supplier<SoundEvent> registerSound(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}
