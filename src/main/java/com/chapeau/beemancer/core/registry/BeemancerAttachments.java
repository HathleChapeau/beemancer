/**
 * ============================================================
 * [BeemancerAttachments.java]
 * Description: Registre des data attachments pour les joueurs
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexPlayerData     | Données du codex     | Stockage progression joueur    |
 * | NeoForge Attachments| Système d'attachment | Persistance des données        |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - Beemancer (enregistrement au démarrage)
 * - CodexScreen (accès aux données joueur)
 * - CodexPackets (synchronisation)
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.registry;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.codex.CodexPlayerData;
import com.chapeau.beemancer.common.quest.QuestPlayerData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class BeemancerAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Beemancer.MOD_ID);

    public static final Supplier<AttachmentType<CodexPlayerData>> CODEX_DATA = ATTACHMENTS.register(
        "codex_data",
        () -> AttachmentType.builder(CodexPlayerData::new)
            .serialize(CodexPlayerData.CODEC)
            .copyOnDeath()
            .build()
    );

    public static final Supplier<AttachmentType<QuestPlayerData>> QUEST_DATA = ATTACHMENTS.register(
        "quest_data",
        () -> AttachmentType.builder(() -> new QuestPlayerData())
            .serialize(QuestPlayerData.CODEC)
            .copyOnDeath()
            .build()
    );

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }
}
