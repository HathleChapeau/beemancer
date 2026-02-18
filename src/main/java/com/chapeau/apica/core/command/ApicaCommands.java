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
 * | QuestPlayerData     | Donnees quetes       | Reset quetes                   |
 * | CodexManager        | Gestionnaire codex   | Acces aux nodes                |
 * | ApicaAttachments    | Attachments          | Sauvegarde donnees             |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - Apica.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.apica.core.command;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.codex.CodexManager;
import com.chapeau.apica.common.codex.CodexNode;
import com.chapeau.apica.common.codex.CodexPage;
import com.chapeau.apica.common.codex.CodexPlayerData;
import com.chapeau.apica.common.quest.QuestPlayerData;
import com.chapeau.apica.core.bee.BeeSpeciesManager;
import com.chapeau.apica.core.network.packets.CodexSyncPacket;
import com.chapeau.apica.core.network.packets.QuestSyncPacket;
import com.chapeau.apica.core.registry.ApicaAttachments;
import com.chapeau.apica.core.util.BeeInjectionHelper;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class ApicaCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("bee")
                .then(Commands.literal("codex")
                    .then(Commands.literal("reset")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("all")
                            .executes(context -> resetAll(context.getSource()))
                        )
                        .then(Commands.literal("knowledge")
                            .executes(context -> resetKnowledge(context.getSource()))
                        )
                        .then(Commands.literal("quests")
                            .executes(context -> resetQuests(context.getSource()))
                        )
                        .then(Commands.literal("traits")
                            .executes(context -> resetTraits(context.getSource()))
                        )
                    )
                    .then(Commands.literal("getAllKnowledge")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> unlockAllKnowledge(context.getSource()))
                    )
                )
                .then(Commands.literal("dimension")
                    .then(Commands.literal("delete")
                        .requires(source -> source.hasPermission(4))
                        .then(Commands.argument("dimension", StringArgumentType.string())
                            .executes(context -> deleteDimension(
                                context.getSource(),
                                StringArgumentType.getString(context, "dimension")
                            ))
                        )
                    )
                )
        );
    }

    // ============================================================
    // CODEX COMMANDS
    // ============================================================

    private static int resetAll(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            CodexPlayerData freshCodex = new CodexPlayerData();
            player.setData(ApicaAttachments.CODEX_DATA, freshCodex);
            PacketDistributor.sendToPlayer(player, new CodexSyncPacket(freshCodex));

            QuestPlayerData freshQuests = new QuestPlayerData();
            player.setData(ApicaAttachments.QUEST_DATA, freshQuests);
            PacketDistributor.sendToPlayer(player, new QuestSyncPacket(freshQuests));

            source.sendSuccess(() -> Component.literal("Codex knowledge and quests have been reset!"), true);
            return Command.SINGLE_SUCCESS;
        }

        source.sendFailure(Component.literal("This command can only be used by a player."));
        return 0;
    }

    private static int resetKnowledge(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            CodexPlayerData freshData = new CodexPlayerData();
            player.setData(ApicaAttachments.CODEX_DATA, freshData);
            PacketDistributor.sendToPlayer(player, new CodexSyncPacket(freshData));

            source.sendSuccess(() -> Component.literal("Codex knowledge has been reset!"), true);
            return Command.SINGLE_SUCCESS;
        }

        source.sendFailure(Component.literal("This command can only be used by a player."));
        return 0;
    }

    private static int resetQuests(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            QuestPlayerData freshData = new QuestPlayerData();
            player.setData(ApicaAttachments.QUEST_DATA, freshData);
            PacketDistributor.sendToPlayer(player, new QuestSyncPacket(freshData));

            source.sendSuccess(() -> Component.literal("All quests have been reset!"), true);
            return Command.SINGLE_SUCCESS;
        }

        source.sendFailure(Component.literal("This command can only be used by a player."));
        return 0;
    }

    private static int unlockAllKnowledge(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            CodexPlayerData data = player.getData(ApicaAttachments.CODEX_DATA);

            int count = 0;
            for (CodexPage page : CodexPage.values()) {
                for (CodexNode node : CodexManager.getNodesForPage(page)) {
                    if (!data.isUnlocked(node)) {
                        data.unlock(node);
                        count++;
                    }
                }
            }

            for (String speciesId : BeeSpeciesManager.getAllSpeciesIds()) {
                data.learnSpecies(speciesId);
                BeeSpeciesManager.BeeSpeciesData speciesData = BeeSpeciesManager.getSpecies(speciesId);
                if (speciesData != null) {
                    for (int lvl = 1; lvl <= 4; lvl++) {
                        data.learnTrait("drop:" + lvl);
                        data.learnTrait("speed:" + lvl);
                        data.learnTrait("foraging:" + lvl);
                        data.learnTrait("tolerance:" + lvl);
                    }
                    for (int lvl = 0; lvl <= 2; lvl++) {
                        data.learnTrait("activity:" + lvl);
                    }
                }
            }

            player.setData(ApicaAttachments.CODEX_DATA, data);
            PacketDistributor.sendToPlayer(player, new CodexSyncPacket(data));

            final int finalCount = count;
            source.sendSuccess(() -> Component.literal("Unlocked " + finalCount + " codex entries + all species & traits!"), true);
            return Command.SINGLE_SUCCESS;
        }

        source.sendFailure(Component.literal("This command can only be used by a player."));
        return 0;
    }

    private static int resetTraits(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            CodexPlayerData data = player.getData(ApicaAttachments.CODEX_DATA);
            data.getKnownSpecies().clear();
            data.getKnownTraits().clear();
            player.setData(ApicaAttachments.CODEX_DATA, data);
            PacketDistributor.sendToPlayer(player, new CodexSyncPacket(data));

            source.sendSuccess(() -> Component.literal("Trait and species knowledge has been reset!"), true);
            return Command.SINGLE_SUCCESS;
        }

        source.sendFailure(Component.literal("This command can only be used by a player."));
        return 0;
    }

    // ============================================================
    // DIMENSION COMMANDS
    // ============================================================

    private static int deleteDimension(CommandSourceStack source, String dimensionName) {
        MinecraftServer server = source.getServer();

        ResourceLocation dimId;
        try {
            dimId = ResourceLocation.parse(dimensionName);
        } catch (Exception e) {
            source.sendFailure(Component.literal("Invalid dimension name: " + dimensionName));
            return 0;
        }

        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimId);

        // Interdire la suppression de l'overworld
        if (dimKey.equals(Level.OVERWORLD)) {
            source.sendFailure(Component.literal("Cannot delete the Overworld!"));
            return 0;
        }

        // Verifier si des joueurs sont dans cette dimension
        ServerLevel level = server.getLevel(dimKey);
        if (level != null) {
            for (ServerPlayer player : level.players()) {
                source.sendFailure(Component.literal(
                    "Cannot delete dimension '" + dimensionName + "': player " +
                    player.getName().getString() + " is currently in it!"));
                return 0;
            }
        }

        // Resoudre le chemin du dossier de la dimension
        Path worldDir = server.getWorldPath(LevelResource.ROOT);
        Path dimFolder = resolveDimensionFolder(worldDir, dimId);

        if (!Files.exists(dimFolder)) {
            source.sendFailure(Component.literal(
                "Dimension folder not found: " + dimFolder.toAbsolutePath()));
            return 0;
        }

        // Suppression recursive du dossier
        try {
            deleteDirectoryRecursive(dimFolder);
            Apica.LOGGER.info("Deleted dimension folder: {}", dimFolder.toAbsolutePath());
            source.sendSuccess(() -> Component.literal(
                "Dimension '" + dimensionName + "' deleted from save! Restart the server for full effect."),
                true);
            return Command.SINGLE_SUCCESS;
        } catch (IOException e) {
            Apica.LOGGER.error("Failed to delete dimension folder: {}", dimFolder, e);
            source.sendFailure(Component.literal(
                "Failed to delete dimension folder: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Resout le chemin du dossier d'une dimension dans la save.
     * - minecraft:the_nether -> DIM-1
     * - minecraft:the_end -> DIM1
     * - namespace:path -> dimensions/namespace/path
     */
    private static Path resolveDimensionFolder(Path worldDir, ResourceLocation dimId) {
        if (dimId.getNamespace().equals("minecraft")) {
            return switch (dimId.getPath()) {
                case "the_nether" -> worldDir.resolve("DIM-1");
                case "the_end" -> worldDir.resolve("DIM1");
                default -> worldDir.resolve("dimensions").resolve(dimId.getNamespace()).resolve(dimId.getPath());
            };
        }
        return worldDir.resolve("dimensions").resolve(dimId.getNamespace()).resolve(dimId.getPath());
    }

    /**
     * Supprime un repertoire et tout son contenu recursivement.
     */
    private static void deleteDirectoryRecursive(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
