/**
 * ============================================================
 * [ApicaCommands.java]
 * Description: Commandes du mod Apica (/bee)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexPlayerData     | Donnees joueur       | Reset/unlock knowledge         |
 * | CodexManager        | Gestionnaire codex   | Acces aux nodes                |
 * | ApicaAttachments| Attachments          | Sauvegarde donnees             |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - Apica.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.apica.core.command;

import com.chapeau.apica.common.codex.CodexManager;
import com.chapeau.apica.common.codex.CodexNode;
import com.chapeau.apica.common.codex.CodexPage;
import com.chapeau.apica.common.codex.CodexPlayerData;
import com.chapeau.apica.core.network.packets.CodexSyncPacket;
import com.chapeau.apica.core.registry.ApicaAttachments;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class ApicaCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("bee")
                .then(Commands.literal("codex")
                    .then(Commands.literal("reset")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> resetCodex(context.getSource()))
                    )
                    .then(Commands.literal("getAllKnowledge")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> unlockAllKnowledge(context.getSource()))
                    )
                )
        );
    }

    private static int resetCodex(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            // Create fresh codex data
            CodexPlayerData freshData = new CodexPlayerData();
            player.setData(ApicaAttachments.CODEX_DATA, freshData);

            // Sync to client
            PacketDistributor.sendToPlayer(player, new CodexSyncPacket(freshData));

            source.sendSuccess(() -> Component.literal("Codex knowledge has been reset!"), true);
            return Command.SINGLE_SUCCESS;
        }

        source.sendFailure(Component.literal("This command can only be used by a player."));
        return 0;
    }

    private static int unlockAllKnowledge(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            CodexPlayerData data = player.getData(ApicaAttachments.CODEX_DATA);

            int count = 0;
            // Unlock all nodes from all pages
            for (CodexPage page : CodexPage.values()) {
                for (CodexNode node : CodexManager.getNodesForPage(page)) {
                    if (!data.isUnlocked(node)) {
                        data.unlock(node);
                        count++;
                    }
                }
            }

            // Save updated data
            player.setData(ApicaAttachments.CODEX_DATA, data);

            // Sync to client
            PacketDistributor.sendToPlayer(player, new CodexSyncPacket(data));

            final int finalCount = count;
            source.sendSuccess(() -> Component.literal("Unlocked " + finalCount + " codex entries!"), true);
            return Command.SINGLE_SUCCESS;
        }

        source.sendFailure(Component.literal("This command can only be used by a player."));
        return 0;
    }
}
