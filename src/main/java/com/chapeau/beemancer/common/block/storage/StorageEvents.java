/**
 * ============================================================
 * [StorageEvents.java]
 * Description: Gestionnaire d'événements pour le système de stockage
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                      | Raison                  | Utilisation           |
 * |---------------------------------|------------------------|-----------------------|
 * | StorageEditModeHandler          | État édition           | Vérification mode     |
 * | StorageControllerBlock          | Logique coffres        | Délégation clic       |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - Beemancer.java (enregistrement événements)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.storage;

import com.chapeau.beemancer.common.blockentity.storage.StorageControllerBlockEntity;
import com.chapeau.beemancer.common.blockentity.storage.StorageTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Gère les événements liés au système de stockage.
 *
 * Intercepte les clics droits sur les coffres quand un joueur
 * est en mode édition d'un Storage Controller.
 */
public class StorageEvents {

    /**
     * Intercepte les clics droits sur les blocs pour gérer le mode édition.
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos clickedPos = event.getPos();

        // Ignorer côté client
        if (level.isClientSide()) return;

        // Vérifier si le joueur est en mode édition
        if (!StorageEditModeHandler.isEditing(player, level)) return;

        // Récupérer la position du controller
        BlockPos controllerPos = StorageEditModeHandler.getEditingController(player.getUUID());
        if (controllerPos == null) return;

        BlockState clickedState = level.getBlockState(clickedPos);

        // Shift+clic sur un terminal = lier au controller
        if (player.isShiftKeyDown()) {
            BlockEntity be = level.getBlockEntity(clickedPos);
            if (be instanceof StorageTerminalBlockEntity terminal) {
                BlockEntity controllerBe = level.getBlockEntity(controllerPos);
                if (controllerBe instanceof StorageControllerBlockEntity controller) {
                    terminal.linkToController(controllerPos);
                    player.displayClientMessage(
                        Component.translatable("message.beemancer.storage_terminal.linked_manually"),
                        true
                    );
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    return;
                }
            }
        }

        // Vérifier si c'est un coffre ou barrel
        if (!(clickedState.getBlock() instanceof ChestBlock) &&
            !clickedState.is(Blocks.CHEST) &&
            !clickedState.is(Blocks.TRAPPED_CHEST) &&
            !clickedState.is(Blocks.BARREL)) {
            return;
        }

        // Déléguer au controller
        boolean handled = StorageControllerBlock.handleChestClick(
            level, controllerPos, clickedPos, player
        );

        if (handled) {
            // Annuler l'ouverture du coffre
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /**
     * Nettoie le mode édition quand un joueur se déconnecte.
     */
    @SubscribeEvent
    public static void onPlayerLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        StorageEditModeHandler.stopEditing(event.getEntity().getUUID());
    }
}
