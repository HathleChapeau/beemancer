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
import com.chapeau.apica.common.entity.mount.HoverbikeConfigManager;
import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import com.chapeau.apica.core.registry.ApicaEntities;
import com.chapeau.apica.common.quest.Quest;
import com.chapeau.apica.common.quest.QuestManager;
import com.chapeau.apica.common.quest.QuestPlayerData;
import com.chapeau.apica.core.bee.BeeSpeciesManager;
import com.chapeau.apica.core.network.packets.CodexSyncPacket;
import com.chapeau.apica.core.network.packets.QuestSyncPacket;
import com.chapeau.apica.core.registry.ApicaAttachments;
import com.chapeau.apica.common.item.bee.MagicBeeItem;
import com.chapeau.apica.common.item.essence.SpeciesEssenceItem;
import com.chapeau.apica.core.gene.BeeGeneData;
import com.chapeau.apica.core.gene.Gene;
import com.chapeau.apica.core.gene.GeneCategory;
import com.chapeau.apica.core.gene.GeneRegistry;
import com.chapeau.apica.core.util.BeeInjectionHelper;
import net.minecraft.world.item.ItemStack;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
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
                .then(Commands.literal("reload")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> reloadConfigs(context.getSource()))
                )
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
                .then(Commands.literal("giveSpecies")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("species", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            for (String id : BeeSpeciesManager.getAllSpeciesIds()) {
                                if (id.startsWith(builder.getRemainingLowerCase())) {
                                    builder.suggest(id);
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> giveSpecies(
                            context.getSource(),
                            StringArgumentType.getString(context, "species")
                        ))
                    )
                )
                .then(Commands.literal("giveSpeciesEssence")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("species", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            for (String id : BeeSpeciesManager.getAllSpeciesIds()) {
                                if (id.startsWith(builder.getRemainingLowerCase())) {
                                    builder.suggest(id);
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> giveSpeciesEssence(
                            context.getSource(),
                            StringArgumentType.getString(context, "species")
                        ))
                    )
                )
                .then(Commands.literal("giveHoverBee")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("species", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            for (String id : BeeSpeciesManager.getAllSpeciesIds()) {
                                if (id.startsWith(builder.getRemainingLowerCase())) {
                                    builder.suggest(id);
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> giveHoverBee(
                            context.getSource(),
                            StringArgumentType.getString(context, "species")
                        ))
                    )
                )
                .then(Commands.literal("dimension")
                    .then(Commands.literal("delete")
                        .requires(source -> source.hasPermission(4))
                        .then(Commands.argument("dimension", ResourceLocationArgument.id())
                            .suggests((context, builder) -> {
                                MinecraftServer server = context.getSource().getServer();
                                for (ServerLevel level : server.getAllLevels()) {
                                    String dimName = level.dimension().location().toString();
                                    if (dimName.startsWith(builder.getRemainingLowerCase())) {
                                        builder.suggest(dimName);
                                    }
                                }
                                return builder.buildFuture();
                            })
                            .executes(context -> deleteDimension(
                                context.getSource(),
                                ResourceLocationArgument.getId(context, "dimension").toString()
                            ))
                        )
                    )
                )
        );
    }

    // ============================================================
    // RELOAD COMMAND
    // ============================================================

    private static int reloadConfigs(CommandSourceStack source) {
        HoverbikeConfigManager.init();
        source.sendSuccess(() -> Component.literal("Apica configs reloaded (hoverbike base stats, modifiers, part base stats)."), true);
        Apica.LOGGER.info("Apica configs reloaded via /bee reload");
        return Command.SINGLE_SUCCESS;
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
                data.learnFrequency(speciesId);
                BeeSpeciesManager.BeeSpeciesData speciesData = BeeSpeciesManager.getSpecies(speciesId);
                if (speciesData != null) {
                    for (int lvl = 1; lvl <= 4; lvl++) {
                        data.learnTrait("drop:" + lvl);
                        data.learnTrait("speed:" + lvl);
                        data.learnTrait("foraging:" + lvl);
                        data.learnTrait("tolerance:" + lvl);
                    }
                    for (int lvl = 1; lvl <= 3; lvl++) {
                        data.learnTrait("activity:" + lvl);
                    }
                }
            }

            player.setData(ApicaAttachments.CODEX_DATA, data);
            PacketDistributor.sendToPlayer(player, new CodexSyncPacket(data));

            // Complete all quests (for codex book quest-gated sections)
            QuestPlayerData questData = player.getData(ApicaAttachments.QUEST_DATA);
            int questCount = 0;
            for (Quest quest : QuestManager.getAllQuests()) {
                if (!questData.isCompleted(quest.getId())) {
                    questData.complete(quest.getId());
                    questCount++;
                }
            }
            if (questCount > 0) {
                player.setData(ApicaAttachments.QUEST_DATA, questData);
                PacketDistributor.sendToPlayer(player, new QuestSyncPacket(questData));
            }

            final int finalCount = count;
            final int finalQuestCount = questCount;
            source.sendSuccess(() -> Component.literal("Unlocked " + finalCount + " codex entries + all species & traits + " + finalQuestCount + " quests!"), true);
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
            data.getKnownFrequencies().clear();
            player.setData(ApicaAttachments.CODEX_DATA, data);
            PacketDistributor.sendToPlayer(player, new CodexSyncPacket(data));

            source.sendSuccess(() -> Component.literal("Trait, species and frequency knowledge has been reset!"), true);
            return Command.SINGLE_SUCCESS;
        }

        source.sendFailure(Component.literal("This command can only be used by a player."));
        return 0;
    }

    // ============================================================
    // GIVE COMMANDS
    // ============================================================

    private static int giveSpecies(CommandSourceStack source, String speciesId) {
        if (source.getEntity() instanceof ServerPlayer player) {
            if (!BeeSpeciesManager.hasSpecies(speciesId)) {
                source.sendFailure(Component.literal("Unknown species: " + speciesId));
                return 0;
            }

            Gene speciesGene = GeneRegistry.getGene(GeneCategory.SPECIES, speciesId);
            if (speciesGene == null) {
                source.sendFailure(Component.literal("No gene found for species: " + speciesId));
                return 0;
            }

            BeeGeneData geneData = new BeeGeneData();
            geneData.setGene(speciesGene);
            ItemStack beeStack = MagicBeeItem.createWithGenes(geneData);

            if (!player.getInventory().add(beeStack)) {
                player.drop(beeStack, false);
            }

            source.sendSuccess(() -> Component.literal("Gave 1 " + speciesId + " bee"), true);
            return Command.SINGLE_SUCCESS;
        }

        source.sendFailure(Component.literal("This command can only be used by a player."));
        return 0;
    }

    private static int giveSpeciesEssence(CommandSourceStack source, String speciesId) {
        if (source.getEntity() instanceof ServerPlayer player) {
            if (!BeeSpeciesManager.hasSpecies(speciesId)) {
                source.sendFailure(Component.literal("Unknown species: " + speciesId));
                return 0;
            }

            ItemStack essence = SpeciesEssenceItem.createForSpecies(speciesId);
            if (!player.getInventory().add(essence)) {
                player.drop(essence, false);
            }

            source.sendSuccess(() -> Component.literal("Gave 1 Species Essence (" + speciesId + ")"), true);
            return Command.SINGLE_SUCCESS;
        }

        source.sendFailure(Component.literal("This command can only be used by a player."));
        return 0;
    }

    // ============================================================
    // HOVERBEE COMMAND
    // ============================================================

    private static int giveHoverBee(CommandSourceStack source, String speciesId) {
        if (source.getEntity() instanceof ServerPlayer player) {
            if (!BeeSpeciesManager.hasSpecies(speciesId)) {
                source.sendFailure(Component.literal("Unknown species: " + speciesId));
                return 0;
            }

            if (!(player.level() instanceof ServerLevel serverLevel)) return 0;

            HoverbikeEntity hoverBee = ApicaEntities.HOVERBIKE.get().create(serverLevel);
            if (hoverBee == null) {
                source.sendFailure(Component.literal("Failed to create HoverBee entity."));
                return 0;
            }

            hoverBee.setSpeciesId(speciesId);
            hoverBee.setPos(player.position().add(0, 1, 0));
            hoverBee.setYRot(player.getYRot());
            hoverBee.setOwner(player);
            serverLevel.addFreshEntity(hoverBee);

            source.sendSuccess(() -> Component.literal("Spawned HoverBee (" + speciesId + ")"), true);
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
