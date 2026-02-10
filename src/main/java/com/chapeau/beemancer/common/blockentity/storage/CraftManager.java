/**
 * ============================================================
 * [CraftManager.java]
 * Description: Orchestrateur des crafts automatiques - resolution, sequencage, timeout
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | CrafterBlockEntity            | Acces inventaire     | Bibliotheque, buffer, output   |
 * | CraftTask                     | Tache de craft       | Cycle de vie                   |
 * | CraftingPaperData             | Donnees recette      | Resolution ingredients         |
 * | StorageControllerBlockEntity  | Reseau stockage      | Livraisons, recherche items    |
 * | DeliveryTask                  | Livraison par bee    | Assignation transport          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageControllerBlockEntity.java (tick, integration livraison)
 * - CrafterBlockEntity.java (delegation craft)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.common.block.storage.DeliveryTask;
import com.chapeau.beemancer.common.data.CraftTask;
import com.chapeau.beemancer.common.data.CraftingPaperData;
import com.chapeau.beemancer.common.item.CraftingPaperItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Gere la file de crafts automatiques pour un CrafterBlockEntity.
 *
 * Responsabilites:
 * - Resolution des crafts imbriques (parcours de la bibliotheque du crafter)
 * - Detection de cycles (un craft qui se reference lui-meme)
 * - Calcul des ressources primaires necessaires
 * - Sequencage: sous-crafts d'abord, craft final ensuite
 * - Timeout: annulation si aucune activite pendant 2 minutes
 * - Un seul craft actif a la fois dans le crafter
 *
 * Vit sur le CrafterBlockEntity, ticke depuis le StorageControllerBlockEntity.
 */
public class CraftManager {

    private static final int TICK_INTERVAL = 20; // Tick toutes les secondes
    private static final int MAX_NESTED_DEPTH = 8;

    private final CrafterBlockEntity crafter;
    private final Map<UUID, CraftTask> tasks = new LinkedHashMap<>();
    @Nullable private UUID activeRootTaskId = null;
    private int tickCounter = 0;

    public CraftManager(CrafterBlockEntity crafter) {
        this.crafter = crafter;
    }

    // === Accessors ===

    public Map<UUID, CraftTask> getAllTasks() { return tasks; }
    @Nullable public UUID getActiveRootTaskId() { return activeRootTaskId; }

    @Nullable
    public CraftTask getActiveRootTask() {
        if (activeRootTaskId == null) return null;
        return tasks.get(activeRootTaskId);
    }

    public boolean isBusy() {
        return activeRootTaskId != null;
    }

    // === Submit a new craft ===

    /**
     * Soumet un craft depuis un CraftingPaper dans la bibliotheque.
     * Resout les sous-crafts imbriques et cree l'arbre de taches.
     *
     * @param recipeData la recette a executer
     * @param gameTick le tick actuel
     * @return la CraftTask racine, ou null si le crafter est occupe ou en erreur
     */
    @Nullable
    public CraftTask submitCraft(CraftingPaperData recipeData, long gameTick) {
        if (isBusy()) return null;

        // Creer la tache racine
        CraftTask rootTask = new CraftTask(recipeData, gameTick, null);

        // Resoudre les sous-crafts depuis la bibliotheque
        Set<String> visitedRecipes = new HashSet<>();
        visitedRecipes.add(recipeResultKey(recipeData));

        boolean resolved = resolveSubCrafts(rootTask, gameTick, visitedRecipes, 0);
        if (!resolved) {
            return null;
        }

        // Enregistrer toutes les taches
        tasks.put(rootTask.getTaskId(), rootTask);
        activeRootTaskId = rootTask.getTaskId();
        crafter.setCrafting(true);

        return rootTask;
    }

    /**
     * Resout recursivement les sous-crafts.
     * Pour chaque ingredient de la recette, verifie si un CraftingPaper
     * dans la bibliotheque du crafter produit cet ingredient.
     * Si oui, cree une sous-tache et remplace la ressource primaire.
     *
     * @return false si cycle detecte ou profondeur max atteinte
     */
    private boolean resolveSubCrafts(CraftTask task, long gameTick,
                                      Set<String> visitedRecipes, int depth) {
        if (depth >= MAX_NESTED_DEPTH) return false;

        Map<CraftTask.ResourceKey, Integer> missing = task.getRequiredResources();

        for (Map.Entry<CraftTask.ResourceKey, Integer> entry : missing.entrySet()) {
            CraftTask.ResourceKey resourceKey = entry.getKey();
            ItemStack needed = resourceKey.getTemplate();

            // Chercher dans la bibliotheque un CraftingPaper qui produit cet item
            CraftingPaperData subRecipe = findRecipeProducing(needed);
            if (subRecipe == null) continue;

            // Detection de cycle
            String resultKey = recipeResultKey(subRecipe);
            if (visitedRecipes.contains(resultKey)) continue;
            visitedRecipes.add(resultKey);

            // Creer une sous-tache pour chaque unite necessaire
            int countNeeded = entry.getValue();
            int produced = subRecipe.result().getCount();
            int craftTimes = (countNeeded + produced - 1) / produced;

            for (int i = 0; i < craftTimes; i++) {
                CraftTask subTask = new CraftTask(subRecipe, gameTick, task.getTaskId());

                // Resolution recursive
                boolean ok = resolveSubCrafts(subTask, gameTick, visitedRecipes, depth + 1);
                if (!ok) {
                    visitedRecipes.remove(resultKey);
                    return false;
                }

                tasks.put(subTask.getTaskId(), subTask);
                task.addSubTask(subTask.getTaskId());
            }

            visitedRecipes.remove(resultKey);
        }

        return true;
    }

    /**
     * Cherche dans la bibliotheque du crafter un CraftingPaper
     * dont le resultat correspond a l'item donne.
     */
    @Nullable
    private CraftingPaperData findRecipeProducing(ItemStack needed) {
        if (crafter.getLevel() == null) return null;
        HolderLookup.Provider registries = crafter.getLevel().registryAccess();
        ItemStackHandler inv = crafter.getInventory();

        for (int slot = CrafterBlockEntity.LIBRARY_START; slot <= CrafterBlockEntity.LIBRARY_END; slot++) {
            ItemStack paper = inv.getStackInSlot(slot);
            if (paper.isEmpty() || !(paper.getItem() instanceof CraftingPaperItem)) continue;

            CraftingPaperData data = CraftingPaperData.readFromStack(paper, registries);
            if (data == null) continue;

            if (ItemStack.isSameItemSameComponents(data.result(), needed)) {
                return data;
            }
        }
        return null;
    }

    /**
     * Cle unique pour une recette (base sur le resultat), utilisee pour la detection de cycles.
     */
    private String recipeResultKey(CraftingPaperData data) {
        return data.result().getItem().toString() + ":" + data.result().getCount();
    }

    // === Tick ===

    /**
     * Tick principal du CraftManager. Appele depuis le controller.
     *
     * @param controller le controller du reseau
     * @param gameTick le tick actuel du jeu
     */
    public void tick(StorageControllerBlockEntity controller, long gameTick) {
        tickCounter++;
        if (tickCounter < TICK_INTERVAL) return;
        tickCounter = 0;

        if (activeRootTaskId == null) {
            // Flush orphan buffer items if no craft is active
            if (!crafter.getCraftBuffer().isEmpty()) {
                returnBufferToNetwork(controller);
            }
            return;
        }

        CraftTask rootTask = tasks.get(activeRootTaskId);
        if (rootTask == null) {
            cleanupActiveTask();
            return;
        }

        // Verifier timeout global (base sur la tache racine)
        if (rootTask.isTimedOut(gameTick)) {
            cancelAllTasks(controller);
            return;
        }

        // Traiter toutes les taches de l'arbre (feuilles d'abord)
        tickTaskTree(rootTask, controller, gameTick);
    }

    /**
     * Traite l'arbre de taches recursivement (profondeur d'abord).
     * Les sous-taches sont traitees avant la tache parente.
     */
    private void tickTaskTree(CraftTask task, StorageControllerBlockEntity controller, long gameTick) {
        if (task.getState() == CraftTask.TaskState.COMPLETED
                || task.getState() == CraftTask.TaskState.CANCELLED) {
            return;
        }

        // Traiter les sous-taches d'abord
        for (UUID subId : task.getSubTaskIds()) {
            CraftTask subTask = tasks.get(subId);
            if (subTask != null) {
                tickTaskTree(subTask, controller, gameTick);
            }
        }

        // Attendre que les sous-taches soient completees
        if (task.hasSubTasks() && !task.areSubTasksComplete(tasks)) {
            return;
        }

        switch (task.getState()) {
            case PENDING_RESOURCES -> tickPendingResources(task, controller, gameTick);
            case CRAFTING -> tickCrafting(task, controller, gameTick);
            case RETURNING_ITEMS -> tickReturningItems(task, gameTick);
            default -> { }
        }
    }

    /**
     * Etat PENDING_RESOURCES: demande les ingredients manquants via des DeliveryTasks.
     */
    private void tickPendingResources(CraftTask task, StorageControllerBlockEntity controller,
                                       long gameTick) {
        if (controller.getLevel() == null) return;

        // Verifier si toutes les ressources sont deja livrees
        if (task.areAllResourcesDelivered()) {
            task.setState(CraftTask.TaskState.CRAFTING);
            task.touchActivity(gameTick);
            return;
        }

        // Ne pas envoyer de nouvelles bees si des livraisons sont en cours
        if (task.hasActiveDeliveries()) return;

        // Demander chaque ressource manquante
        Map<CraftTask.ResourceKey, Integer> missing = task.getMissingResources();
        BlockPos crafterPos = crafter.getBlockPos();

        for (Map.Entry<CraftTask.ResourceKey, Integer> entry : missing.entrySet()) {
            ItemStack template = entry.getKey().getTemplate();
            int count = entry.getValue();

            // Trouver un coffre contenant cet item
            BlockPos source = controller.findChestWithItem(template, 1);
            if (source == null) continue;

            // Creer une DeliveryTask craft: coffre -> crafter
            DeliveryTask dt = DeliveryTask.createCraftDelivery(
                    template, count, source, crafterPos,
                    false, task.getTaskId(), false
            );
            controller.getDeliveryManager().addDeliveryTask(dt);
            task.addActiveDelivery(dt.getTaskId());
            task.touchActivity(gameTick);
        }
    }

    /**
     * Etat CRAFTING: verifie le craft buffer et effectue le craft.
     * Le resultat est renvoye au reseau via une abeille preloaded.
     */
    private void tickCrafting(CraftTask task, StorageControllerBlockEntity controller,
                               long gameTick) {
        // Verifier que tous les ingredients sont dans le buffer
        if (!verifyBufferContents(task)) {
            // Ressources insuffisantes dans le buffer, retour a PENDING
            task.setState(CraftTask.TaskState.PENDING_RESOURCES);
            return;
        }

        // Extraire le resultat (consomme les ingredients du buffer)
        ItemStack result = crafter.extractCraftResult(task);
        if (result.isEmpty()) return;

        // Trouver un coffre destination dans le reseau
        BlockPos dest = controller.findSlotForItem(result);
        if (dest == null) {
            // Pas de place dans le reseau, remettre le resultat dans le buffer
            crafter.addToCraftBuffer(result);
            return;
        }

        // Creer une DeliveryTask craft preloaded: le resultat est deja sur la bee
        BlockPos crafterPos = crafter.getBlockPos();
        DeliveryTask returnTask = DeliveryTask.createCraftDelivery(
                result, result.getCount(), crafterPos, dest,
                true, task.getTaskId(), true
        );
        controller.getDeliveryManager().addDeliveryTask(returnTask);
        task.addActiveDelivery(returnTask.getTaskId());

        // Transition vers RETURNING_ITEMS
        task.setState(CraftTask.TaskState.RETURNING_ITEMS);
        task.touchActivity(gameTick);
    }

    /**
     * Etat RETURNING_ITEMS: attend que la bee de retour ait depose le resultat.
     * Quand la livraison est completee, la tache passe a COMPLETED.
     */
    private void tickReturningItems(CraftTask task, long gameTick) {
        // La transition RETURNING_ITEMS -> COMPLETED est geree par onDeliveryCompleted()
        // Ici on verifie juste si les deliveries actives ont disparu (echec silencieux)
        if (!task.hasActiveDeliveries()) {
            task.setState(CraftTask.TaskState.COMPLETED);
            task.touchActivity(gameTick);

            if (task.isRoot()) {
                cleanupCompletedTree();
            }
        }
    }

    /**
     * Verifie que le craft buffer contient suffisamment de chaque ingredient.
     */
    private boolean verifyBufferContents(CraftTask task) {
        List<ItemStack> buffer = crafter.getCraftBuffer();
        Map<CraftTask.ResourceKey, Integer> required = task.getRequiredResources();

        for (Map.Entry<CraftTask.ResourceKey, Integer> entry : required.entrySet()) {
            int found = 0;
            for (ItemStack bufferStack : buffer) {
                if (entry.getKey().matches(bufferStack)) {
                    found += bufferStack.getCount();
                }
            }
            if (found < entry.getValue()) return false;
        }
        return true;
    }

    // === Delivery callback ===

    /**
     * Notifie le manager qu'une livraison a ete completee.
     * Appele quand une bee arrive au crafter avec des items (PENDING_RESOURCES)
     * ou quand une bee depose le resultat dans le reseau (RETURNING_ITEMS).
     */
    public void onDeliveryCompleted(UUID deliveryTaskId, ItemStack delivered, int count, long gameTick) {
        for (CraftTask task : tasks.values()) {
            if (task.getActiveDeliveryIds().contains(deliveryTaskId)) {
                task.removeActiveDelivery(deliveryTaskId);
                task.touchActivity(gameTick);

                if (task.getState() == CraftTask.TaskState.RETURNING_ITEMS) {
                    // La bee de retour a depose le resultat: craft complete
                    task.setState(CraftTask.TaskState.COMPLETED);
                    if (task.isRoot()) {
                        cleanupCompletedTree();
                    }
                } else {
                    // Livraison d'ingredient vers le craft buffer
                    task.recordDelivery(delivered, count);
                }

                // Toucher aussi la racine pour le timeout global
                CraftTask root = getActiveRootTask();
                if (root != null) {
                    root.touchActivity(gameTick);
                }
                return;
            }
        }
    }

    /**
     * Notifie le manager qu'une livraison a echoue.
     */
    public void onDeliveryFailed(UUID deliveryTaskId) {
        for (CraftTask task : tasks.values()) {
            if (task.getActiveDeliveryIds().contains(deliveryTaskId)) {
                task.removeActiveDelivery(deliveryTaskId);
                return;
            }
        }
    }

    // === Cancellation ===

    /**
     * Annule toutes les taches et retourne les items du buffer au reseau.
     */
    public void cancelAllTasks(StorageControllerBlockEntity controller) {
        // Retourner les items du buffer au reseau via depot
        returnBufferToNetwork(controller);

        // Annuler toutes les taches
        for (CraftTask task : tasks.values()) {
            task.setState(CraftTask.TaskState.CANCELLED);
            for (UUID deliveryId : task.getActiveDeliveryIds()) {
                controller.getDeliveryManager().cancelTask(deliveryId);
            }
        }

        cleanupActiveTask();
    }

    /**
     * Retourne les items du craft buffer dans le reseau de stockage via des bees preloaded.
     * Les items sont charges directement sur les bees (pas de phase source).
     */
    private void returnBufferToNetwork(StorageControllerBlockEntity controller) {
        List<ItemStack> buffer = crafter.getCraftBuffer();
        BlockPos crafterPos = crafter.getBlockPos();

        for (ItemStack stack : buffer) {
            if (stack.isEmpty()) continue;

            BlockPos dest = controller.findSlotForItem(stack);
            if (dest == null) {
                // Pas de place: depot direct en fallback
                controller.depositItem(stack);
                continue;
            }

            DeliveryTask returnTask = new DeliveryTask(
                    stack, stack.getCount(), crafterPos, dest,
                    DeliveryTask.TaskOrigin.AUTOMATION, true,
                    crafterPos
            );
            controller.getDeliveryManager().addDeliveryTask(returnTask);
        }
        crafter.clearCraftBuffer();
    }

    // === Cleanup ===

    private void cleanupCompletedTree() {
        // Retirer toutes les taches completees/annulees
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, CraftTask> entry : tasks.entrySet()) {
            CraftTask.TaskState s = entry.getValue().getState();
            if (s == CraftTask.TaskState.COMPLETED || s == CraftTask.TaskState.CANCELLED) {
                toRemove.add(entry.getKey());
            }
        }
        for (UUID id : toRemove) {
            tasks.remove(id);
        }
        activeRootTaskId = null;
        crafter.setCrafting(false);
        crafter.clearCraftBuffer();
    }

    private void cleanupActiveTask() {
        tasks.clear();
        activeRootTaskId = null;
        crafter.setCrafting(false);
    }

    // === NBT ===

    public void save(CompoundTag parentTag, HolderLookup.Provider registries) {
        ListTag taskList = new ListTag();
        for (CraftTask task : tasks.values()) {
            taskList.add(task.save(registries));
        }
        parentTag.put("CraftTasks", taskList);

        if (activeRootTaskId != null) {
            parentTag.putUUID("ActiveRootTaskId", activeRootTaskId);
        }
    }

    public void load(CompoundTag parentTag, HolderLookup.Provider registries) {
        tasks.clear();
        activeRootTaskId = null;

        if (parentTag.contains("CraftTasks", Tag.TAG_LIST)) {
            ListTag taskList = parentTag.getList("CraftTasks", Tag.TAG_COMPOUND);
            for (int i = 0; i < taskList.size(); i++) {
                CraftTask task = CraftTask.load(taskList.getCompound(i), registries);
                tasks.put(task.getTaskId(), task);
            }
        }

        if (parentTag.contains("ActiveRootTaskId")) {
            activeRootTaskId = parentTag.getUUID("ActiveRootTaskId");
            if (!tasks.containsKey(activeRootTaskId)) {
                activeRootTaskId = null;
            }
        }

        // Restore crafting state on crafter
        crafter.setCrafting(activeRootTaskId != null);
    }
}
