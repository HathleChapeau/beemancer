/**
 * ============================================================
 * [BeeWandItem.java]
 * Description: Baguette pour observer et tracker les entités/blocs
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | WandClassTracker    | Config tracking      | Définition des données trackées|
 * | MagicBeeEntity      | Entité abeille       | Tracking des abeilles          |
 * | MagicHiveBlockEntity| Ruche                | Tracking des ruches            |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - WandOverlayRenderer.java: Affichage HUD
 * - BeemancerItems.java: Enregistrement
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.item.bee;

import com.chapeau.beemancer.common.block.hive.MagicHiveBlockEntity;
import com.chapeau.beemancer.common.entity.bee.BeeActivityState;
import com.chapeau.beemancer.common.entity.bee.BeeInventory;
import com.chapeau.beemancer.common.entity.bee.MagicBeeEntity;
import com.chapeau.beemancer.core.behavior.BeeBehaviorType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BeeWandItem extends Item {
    
    // Liste des trackers configurés
    private static final List<WandClassTracker<?>> TRACKERS = new ArrayList<>();
    
    static {
        // Configuration du tracking pour MagicBeeEntity
        TRACKERS.add(new WandClassTracker<>(MagicBeeEntity.class)
                .track("Species", bee -> bee.getSpeciesId())
                .track("Health", bee -> String.format("%.1f/%.1f", bee.getHealth(), bee.getMaxHealth()))
                .track("Lifetime", bee -> formatTicks(bee.getRemainingLifetime()))
                .track("Behavior", bee -> bee.getBehaviorConfig().getBehaviorType().name())
                .track("Pollinated", bee -> bee.isPollinated() ? "Yes" : "No")
                .track("Enraged", bee -> bee.isEnraged() ? "Yes" : "No")
                .track("Hive", bee -> bee.hasAssignedHive() ? formatPos(bee.getAssignedHivePos()) : "None")
                .track("Slot", bee -> bee.hasAssignedHive() ? String.valueOf(bee.getAssignedSlot()) : "-")
                .track("Inventory", bee -> {
                    BeeInventory inv = bee.getInventory();
                    if (inv == null) return "N/A";
                    return inv.getTotalItemCount() + " items";
                })
        );
        
        // Configuration du tracking pour MagicHiveBlockEntity
        TRACKERS.add(new WandClassTracker<>(MagicHiveBlockEntity.class)
                .track("Breeding Mode", hive -> hive.isBreedingMode() ? "Active" : "Inactive")
                .track("Bees Inside", hive -> countBeesInState(hive, MagicHiveBlockEntity.BeeState.INSIDE))
                .track("Bees Outside", hive -> countBeesInState(hive, MagicHiveBlockEntity.BeeState.OUTSIDE))
                .track("Empty Slots", hive -> countBeesInState(hive, MagicHiveBlockEntity.BeeState.EMPTY))
                .track(" ", hive -> " ")
                .track("Scan Cooldown", MagicHiveBlockEntity::getFlowerScanCooldown)
                .track("Bee Cooldown", hive -> Arrays.toString( hive.getBeeCooldowns()))
                .track("Bee Flowers", hive -> Arrays.toString( hive.getBeeFlowers()))
        );
    }
    
    // Sélection actuelle (peut être Entity ou BlockEntity)
    @Nullable
    private static Object clientSelectedObject = null;
    @Nullable
    private static BlockPos clientSelectedBlockPos = null;
    
    public BeeWandItem(Properties properties) {
        super(properties.stacksTo(1));
    }
    
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();
        Vec3 clickLocation = context.getClickLocation();
        
        if (player == null) return InteractionResult.PASS;
        
        // Chercher d'abord une entité à la position cliquée
        AABB searchBox = new AABB(clickLocation.subtract(0.5, 0.5, 0.5), clickLocation.add(0.5, 0.5, 0.5));
        List<Entity> entities = level.getEntities(player, searchBox);
        
        Entity clickedEntity = null;
        double closestDist = Double.MAX_VALUE;
        for (Entity entity : entities) {
            double dist = entity.position().distanceTo(clickLocation);
            if (dist < closestDist) {
                closestDist = dist;
                clickedEntity = entity;
            }
        }
        
        if (clickedEntity != null) {
            // Sélectionner l'entité
            selectEntity(stack, clickedEntity, level.isClientSide());
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        
        // Sinon, chercher un BlockEntity
        BlockEntity blockEntity = level.getBlockEntity(clickedPos);
        if (blockEntity != null) {
            selectBlockEntity(stack, blockEntity, clickedPos, level.isClientSide());
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        
        // Rien à sélectionner
        return InteractionResult.PASS;
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        // Shift+clic droit pour désélectionner
        if (player.isShiftKeyDown()) {
            clearSelection(stack, level.isClientSide());
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        
        return InteractionResultHolder.pass(stack);
    }
    
    // --- Sélection ---
    
    private void selectEntity(ItemStack stack, Entity entity, boolean isClientSide) {
        if (isClientSide) {
            clientSelectedObject = entity;
            clientSelectedBlockPos = null;
        } else {
            // Stocker l'UUID pour persistance
            CompoundTag tag = new CompoundTag();
            tag.putUUID("SelectedEntity", entity.getUUID());
            tag.putString("SelectionType", "entity");
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }
    
    private void selectBlockEntity(ItemStack stack, BlockEntity blockEntity, BlockPos pos, boolean isClientSide) {
        if (isClientSide) {
            clientSelectedObject = blockEntity;
            clientSelectedBlockPos = pos;
        } else {
            CompoundTag tag = new CompoundTag();
            tag.putInt("SelectedX", pos.getX());
            tag.putInt("SelectedY", pos.getY());
            tag.putInt("SelectedZ", pos.getZ());
            tag.putString("SelectionType", "block");
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }
    
    private void clearSelection(ItemStack stack, boolean isClientSide) {
        if (isClientSide) {
            clientSelectedObject = null;
            clientSelectedBlockPos = null;
        } else {
            stack.remove(DataComponents.CUSTOM_DATA);
        }
    }
    
    // --- Accès statique pour le renderer ---
    
    /**
     * Récupère l'objet actuellement sélectionné (côté client).
     */
    @Nullable
    public static Object getClientSelectedObject() {
        return clientSelectedObject;
    }
    
    /**
     * Met à jour la sélection client depuis le monde (appelé chaque tick).
     */
    public static void updateClientSelection(Level level, ItemStack wandStack) {
        if (level == null || wandStack.isEmpty()) {
            clientSelectedObject = null;
            clientSelectedBlockPos = null;
            return;
        }
        
        // Si on a une position de bloc, récupérer le BlockEntity
        if (clientSelectedBlockPos != null) {
            BlockEntity be = level.getBlockEntity(clientSelectedBlockPos);
            clientSelectedObject = be;
            return;
        }
        
        // Sinon, l'entité est déjà référencée et sera mise à jour automatiquement
        // Vérifier si elle existe toujours
        if (clientSelectedObject instanceof Entity entity) {
            if (entity.isRemoved()) {
                clientSelectedObject = null;
            }
        }
    }
    
    /**
     * Récupère les données trackées pour l'objet actuellement sélectionné.
     * Retourne une liste vide si pas de sélection ou pas de tracker.
     */
    public static List<WandClassTracker.TrackedValue> getTrackedValues() {
        if (clientSelectedObject == null) {
            return List.of();
        }
        
        for (WandClassTracker<?> tracker : TRACKERS) {
            if (tracker.matches(clientSelectedObject)) {
                return tracker.getValues(clientSelectedObject);
            }
        }
        
        return List.of();
    }
    
    /**
     * Vérifie si un objet est sélectionné.
     */
    public static boolean hasSelection() {
        return clientSelectedObject != null;
    }
    
    /**
     * Récupère le nom de la classe de l'objet sélectionné.
     */
    public static String getSelectedClassName() {
        if (clientSelectedObject == null) return "";
        return clientSelectedObject.getClass().getSimpleName();
    }
    
    // --- Utilitaires ---

    private static String formatArray(Arrays arrays){
        String t = "";
        //for (int i = 0; i < arrays.

        return t;
    }

    private static String formatTicks(int ticks) {
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
    
    private static String formatPos(BlockPos pos) {
        if (pos == null) return "None";
        return String.format("(%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ());
    }
    
    private static int countBeesInState(MagicHiveBlockEntity hive, MagicHiveBlockEntity.BeeState state) {
        int count = 0;
        for (int i = 0; i < MagicHiveBlockEntity.BEE_SLOTS; i++) {
            if (hive.getBeeState(i) == state) {
                count++;
            }
        }
        return count;
    }
}
