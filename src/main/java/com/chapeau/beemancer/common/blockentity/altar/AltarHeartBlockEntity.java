/**
 * ============================================================
 * [AltarHeartBlockEntity.java]
 * Description: BlockEntity du Coeur de l'Autel - Contrôleur Honey Altar
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation           |
 * |---------------------|----------------------|-----------------------|
 * | MultiblockController| Interface contrôleur | Implémentation        |
 * | MultiblockPatterns  | Définition pattern   | HONEY_ALTAR           |
 * | MultiblockValidator | Validation           | tryFormAltar()        |
 * | MultiblockEvents    | Enregistrement       | Détection destruction |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - AltarHeartBlock.java (création BlockEntity)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.altar;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.block.altar.AltarHeartBlock;
import com.chapeau.beemancer.common.block.altar.HoneyCrystalConduitBlock;
import com.chapeau.beemancer.common.block.altar.HoneyPedestalBlock;
import com.chapeau.beemancer.common.block.altar.HoneyReservoirBlock;
import com.chapeau.beemancer.common.block.altar.HoneyedStoneBlock;
import com.chapeau.beemancer.common.block.altar.HoneyedStoneStairBlock;
import com.chapeau.beemancer.common.block.pollenpot.PollenPotBlockEntity;
import com.chapeau.beemancer.core.multiblock.MultiblockController;
import com.chapeau.beemancer.core.multiblock.MultiblockEvents;
import com.chapeau.beemancer.core.multiblock.MultiblockPattern;
import com.chapeau.beemancer.core.multiblock.MultiblockPatterns;
import com.chapeau.beemancer.core.multiblock.MultiblockValidator;
import com.chapeau.beemancer.core.recipe.AltarRecipeInput;
import com.chapeau.beemancer.core.recipe.BeemancerRecipeTypes;
import com.chapeau.beemancer.core.recipe.type.AltarRecipe;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * BlockEntity du Coeur de l'Autel.
 * Implémente MultiblockController pour gérer le Honey Altar.
 */
public class AltarHeartBlockEntity extends BlockEntity implements MultiblockController {

    private boolean altarFormed = false;

    // === Animation ===
    /** Vitesse de rotation des conduits en degrés par tick (20 ticks = 1 seconde) */
    private float conduitRotationSpeed = 1.0f;

    public AltarHeartBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.ALTAR_HEART.get(), pos, state);
    }

    // ==================== MultiblockController ====================

    @Override
    public MultiblockPattern getPattern() {
        return MultiblockPatterns.HONEY_ALTAR;
    }

    @Override
    public boolean isFormed() {
        return altarFormed;
    }

    @Override
    public BlockPos getControllerPos() {
        return worldPosition;
    }

    @Override
    public void onMultiblockFormed() {
        altarFormed = true;
        if (level != null && !level.isClientSide()) {
            // Mettre à jour le contrôleur
            level.setBlock(worldPosition, getBlockState().setValue(AltarHeartBlock.FORMED, true), 3);

            // Mettre à jour tous les blocs du multibloc
            updateMultiblockBlocksState(true);

            MultiblockEvents.registerActiveController(level, worldPosition);
            setChanged();
        }
    }

    @Override
    public void onMultiblockBroken() {
        altarFormed = false;
        if (level != null && !level.isClientSide()) {
            // Mettre à jour le contrôleur
            if (level.getBlockState(worldPosition).hasProperty(AltarHeartBlock.FORMED)) {
                level.setBlock(worldPosition, getBlockState().setValue(AltarHeartBlock.FORMED, false), 3);
            }

            // Mettre à jour tous les blocs du multibloc
            updateMultiblockBlocksState(false);

            MultiblockEvents.unregisterController(worldPosition);
            setChanged();
        }
    }

    /**
     * Met à jour l'état FORMED de tous les blocs du multibloc.
     */
    private void updateMultiblockBlocksState(boolean formed) {
        if (level == null) return;

        // === Pedestal à Y-2 ===
        updateBlockFormed(worldPosition.offset(0, -2, 0), HoneyPedestalBlock.FORMED, formed);

        // === 4 Stairs à Y-2 ===
        BlockPos[] stairOffsets = {
            new BlockPos(0, -2, -1),  // N
            new BlockPos(0, -2, 1),   // S
            new BlockPos(1, -2, 0),   // E
            new BlockPos(-1, -2, 0)   // W
        };
        for (BlockPos offset : stairOffsets) {
            updateBlockFormed(worldPosition.offset(offset), HoneyedStoneStairBlock.FORMED, formed);
        }

        // === 4 Conduits cardinaux à Y+1 (orientés vers le centre) ===
        updateConduit(worldPosition.offset(0, 1, -1), formed, Direction.SOUTH);   // N pointe vers S
        updateConduit(worldPosition.offset(0, 1, 1), formed, Direction.NORTH);    // S pointe vers N
        updateConduit(worldPosition.offset(1, 1, 0), formed, Direction.WEST);     // E pointe vers W
        updateConduit(worldPosition.offset(-1, 1, 0), formed, Direction.EAST);    // W pointe vers E

        // === Honeyed Stone centre à Y+1 (layer 1: base avec colonne) ===
        updateHoneyedStone(worldPosition.offset(0, 1, 0), formed, 1);

        // === Honeyed Stone centre à Y+2 (layer 2: gros cube) ===
        updateHoneyedStone(worldPosition.offset(0, 2, 0), formed, 2);

        // === 4 Réservoirs à Y+2 (orientés vers le centre) ===
        updateReservoir(worldPosition.offset(0, 2, -1), formed, Direction.SOUTH);  // N pointe vers S
        updateReservoir(worldPosition.offset(0, 2, 1), formed, Direction.NORTH);   // S pointe vers N
        updateReservoir(worldPosition.offset(1, 2, 0), formed, Direction.WEST);    // E pointe vers W
        updateReservoir(worldPosition.offset(-1, 2, 0), formed, Direction.EAST);   // W pointe vers E
    }

    /**
     * Met à jour la propriété FORMED d'un bloc si elle existe.
     */
    private void updateBlockFormed(BlockPos pos, net.minecraft.world.level.block.state.properties.BooleanProperty property, boolean formed) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(property)) {
            level.setBlock(pos, state.setValue(property, formed), 3);
        }
    }

    /**
     * Met à jour un HoneyedStoneBlock avec FORMED et LAYER.
     */
    private void updateHoneyedStone(BlockPos pos, boolean formed, int layer) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(HoneyedStoneBlock.FORMED) && state.hasProperty(HoneyedStoneBlock.LAYER)) {
            level.setBlock(pos, state
                .setValue(HoneyedStoneBlock.FORMED, formed)
                .setValue(HoneyedStoneBlock.LAYER, formed ? layer : 0), 3);
        }
    }

    /**
     * Met à jour un HoneyReservoirBlock avec FORMED et FACING.
     */
    private void updateReservoir(BlockPos pos, boolean formed, Direction facing) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(HoneyReservoirBlock.FORMED) && state.hasProperty(HoneyReservoirBlock.FACING)) {
            level.setBlock(pos, state
                .setValue(HoneyReservoirBlock.FORMED, formed)
                .setValue(HoneyReservoirBlock.FACING, facing), 3);
        }
    }

    /**
     * Met à jour un HoneyCrystalConduitBlock avec FORMED et FACING.
     */
    private void updateConduit(BlockPos pos, boolean formed, Direction facing) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(HoneyCrystalConduitBlock.FORMED) && state.hasProperty(HoneyCrystalConduitBlock.FACING)) {
            level.setBlock(pos, state
                .setValue(HoneyCrystalConduitBlock.FORMED, formed)
                .setValue(HoneyCrystalConduitBlock.FACING, facing), 3);
        }
    }

    // ==================== Public API ====================

    /**
     * Tente de former le Honey Altar.
     * @return true si la formation a réussi
     */
    public boolean tryFormAltar() {
        if (level == null || level.isClientSide()) return false;

        // Utiliser validateDetailed pour avoir les infos d'échec
        var result = MultiblockValidator.validateDetailed(getPattern(), level, worldPosition);

        if (result.valid()) {
            onMultiblockFormed();
            return true;
        }

        // Log l'échec pour debug
        Beemancer.LOGGER.debug("Altar validation failed at {} - {}",
            result.failedAt(), result.reason());
        return false;
    }

    /**
     * Récupère les 4 réservoirs du multiblock formé.
     * @return Liste des HoneyReservoirBlockEntity (peut être vide si non formé)
     */
    public List<HoneyReservoirBlockEntity> getReservoirs() {
        List<HoneyReservoirBlockEntity> reservoirs = new ArrayList<>();
        if (!altarFormed || level == null) return reservoirs;

        // Positions des réservoirs à Y+2 relatif au contrôleur
        BlockPos[] offsets = {
            new BlockPos(0, 2, -1),  // Nord
            new BlockPos(0, 2, 1),   // Sud
            new BlockPos(1, 2, 0),   // Est
            new BlockPos(-1, 2, 0)   // Ouest
        };

        for (BlockPos offset : offsets) {
            BlockEntity be = level.getBlockEntity(worldPosition.offset(offset));
            if (be instanceof HoneyReservoirBlockEntity reservoir) {
                reservoirs.add(reservoir);
            }
        }

        return reservoirs;
    }

    /**
     * Calcule le total de fluide d'un type spécifique dans tous les réservoirs.
     * @param fluidType Le type de fluide à compter
     * @return Le montant total en mB
     */
    public int getTotalFluidAmount(Fluid fluidType) {
        int total = 0;
        for (HoneyReservoirBlockEntity reservoir : getReservoirs()) {
            if (!reservoir.getFluid().isEmpty() && reservoir.getFluid().getFluid() == fluidType) {
                total += reservoir.getFluidAmount();
            }
        }
        return total;
    }

    /**
     * Vérifie si l'altar est actuellement formé.
     */
    public boolean isAltarFormed() {
        return isFormed();
    }

    // ==================== Altar Crafting ====================

    private static final int PEDESTAL_SEARCH_RADIUS = 5;
    private static final int POLLEN_POT_SEARCH_RADIUS = 5;

    /**
     * Tente d'executer un craft d'altar.
     * Appele quand le joueur clique sur le coeur avec l'altar forme.
     * @return true si un craft a ete effectue ou du pollen consomme
     */
    public boolean tryCraft() {
        if (level == null || level.isClientSide() || !altarFormed) {
            return false;
        }

        // Position du pedestal central (Y-2 par rapport au coeur)
        BlockPos centerPedestalPos = worldPosition.offset(0, -2, 0);

        // Recuperer l'item central
        ItemStack centerItem = getCenterItem(centerPedestalPos);

        // Recuperer les items des pedestals autour
        List<HoneyPedestalBlockEntity> surroundingPedestals = getSurroundingPedestals(centerPedestalPos);
        List<ItemStack> pedestalItems = new ArrayList<>();
        for (HoneyPedestalBlockEntity pedestal : surroundingPedestals) {
            if (!pedestal.isEmpty()) {
                pedestalItems.add(pedestal.getStoredItem());
            }
        }

        // Recuperer le pollen disponible
        List<PollenPotBlockEntity> pollenPots = getSurroundingPollenPots(centerPedestalPos);
        Map<Item, Integer> availablePollen = new HashMap<>();
        for (PollenPotBlockEntity pot : pollenPots) {
            if (!pot.isEmpty()) {
                Item pollenItem = pot.getPollenItem();
                availablePollen.merge(pollenItem, pot.getPollenCount(), Integer::sum);
            }
        }

        // Creer l'input pour la recette
        AltarRecipeInput input = new AltarRecipeInput(centerItem, pedestalItems, availablePollen);

        // Chercher une recette qui match
        Optional<RecipeHolder<AltarRecipe>> recipeHolder = level.getRecipeManager()
            .getRecipeFor(BeemancerRecipeTypes.ALTAR.get(), input, level);

        if (recipeHolder.isEmpty()) {
            // Pas de recette trouvee - son d'echec
            playFailSound();
            return false;
        }

        AltarRecipe recipe = recipeHolder.get().value();

        // Verifier si le pollen est suffisant
        if (recipe.hasEnoughPollen(availablePollen)) {
            // Craft complet!
            executeCraft(recipe, centerPedestalPos, surroundingPedestals, pollenPots, availablePollen);
            return true;
        } else {
            // Pollen insuffisant - consommer ce qu'on peut
            Map<Item, Integer> toConsume = recipe.getPollenToConsume(availablePollen);
            if (!toConsume.isEmpty()) {
                consumePollenPartial(pollenPots, toConsume);
                playPartialSound();
                return true;
            } else {
                playFailSound();
                return false;
            }
        }
    }

    /**
     * Execute le craft complet.
     */
    private void executeCraft(AltarRecipe recipe, BlockPos centerPedestalPos,
                               List<HoneyPedestalBlockEntity> surroundingPedestals,
                               List<PollenPotBlockEntity> pollenPots,
                               Map<Item, Integer> availablePollen) {
        // Consommer l'item central
        BlockEntity centerBe = level.getBlockEntity(centerPedestalPos);
        if (centerBe instanceof HoneyPedestalBlockEntity centerPedestal) {
            centerPedestal.consumeItem();
        }

        // Consommer les items des pedestals (dans l'ordre de la recette)
        List<ItemStack> remainingRequired = new ArrayList<>();
        for (var ingredient : recipe.pedestalItems()) {
            remainingRequired.add(ingredient.getItems()[0]); // Simplification
        }

        for (HoneyPedestalBlockEntity pedestal : surroundingPedestals) {
            if (pedestal.isEmpty()) continue;
            ItemStack pedestalItem = pedestal.getStoredItem();

            // Chercher si cet item est requis
            for (int i = 0; i < remainingRequired.size(); i++) {
                if (recipe.pedestalItems().get(i).test(pedestalItem)) {
                    pedestal.consumeItem();
                    remainingRequired.remove(i);
                    break;
                }
            }
        }

        // Consommer le pollen
        Map<Item, Integer> pollenToConsume = recipe.getPollenToConsume(availablePollen);
        consumePollenPartial(pollenPots, pollenToConsume);

        // Placer le resultat sur le pedestal central
        ItemStack result = recipe.result().copy();
        if (centerBe instanceof HoneyPedestalBlockEntity centerPedestal) {
            centerPedestal.placeItem(result);
        }

        // Effets visuels et sonores
        playSuccessEffects(centerPedestalPos);
    }

    /**
     * Consomme du pollen partiellement depuis les pollen pots.
     */
    private void consumePollenPartial(List<PollenPotBlockEntity> pollenPots, Map<Item, Integer> toConsume) {
        Map<Item, Integer> remaining = new HashMap<>(toConsume);

        for (PollenPotBlockEntity pot : pollenPots) {
            if (pot.isEmpty()) continue;
            Item pollenItem = pot.getPollenItem();

            Integer needed = remaining.get(pollenItem);
            if (needed == null || needed <= 0) continue;

            int available = pot.getPollenCount();
            int consume = Math.min(needed, available);

            for (int i = 0; i < consume; i++) {
                pot.removePollen();
            }

            remaining.put(pollenItem, needed - consume);
        }
    }

    /**
     * Recupere l'item du pedestal central.
     */
    private ItemStack getCenterItem(BlockPos centerPedestalPos) {
        BlockEntity be = level.getBlockEntity(centerPedestalPos);
        if (be instanceof HoneyPedestalBlockEntity pedestal) {
            return pedestal.getStoredItem();
        }
        return ItemStack.EMPTY;
    }

    /**
     * Trouve tous les pedestals dans un rayon autour du pedestal central.
     */
    private List<HoneyPedestalBlockEntity> getSurroundingPedestals(BlockPos centerPos) {
        List<HoneyPedestalBlockEntity> pedestals = new ArrayList<>();
        int y = centerPos.getY();

        for (int dx = -PEDESTAL_SEARCH_RADIUS; dx <= PEDESTAL_SEARCH_RADIUS; dx++) {
            for (int dz = -PEDESTAL_SEARCH_RADIUS; dz <= PEDESTAL_SEARCH_RADIUS; dz++) {
                if (dx == 0 && dz == 0) continue; // Skip le pedestal central

                BlockPos checkPos = new BlockPos(centerPos.getX() + dx, y, centerPos.getZ() + dz);
                BlockEntity be = level.getBlockEntity(checkPos);
                if (be instanceof HoneyPedestalBlockEntity pedestal) {
                    pedestals.add(pedestal);
                }
            }
        }

        return pedestals;
    }

    /**
     * Trouve tous les pollen pots dans un rayon autour du pedestal central.
     */
    private List<PollenPotBlockEntity> getSurroundingPollenPots(BlockPos centerPos) {
        List<PollenPotBlockEntity> pots = new ArrayList<>();
        int y = centerPos.getY();

        for (int dx = -POLLEN_POT_SEARCH_RADIUS; dx <= POLLEN_POT_SEARCH_RADIUS; dx++) {
            for (int dz = -POLLEN_POT_SEARCH_RADIUS; dz <= POLLEN_POT_SEARCH_RADIUS; dz++) {
                BlockPos checkPos = new BlockPos(centerPos.getX() + dx, y, centerPos.getZ() + dz);
                BlockEntity be = level.getBlockEntity(checkPos);
                if (be instanceof PollenPotBlockEntity pot) {
                    pots.add(pot);
                }
            }
        }

        return pots;
    }

    /**
     * Joue les effets de succes (explosion de particules + son).
     */
    private void playSuccessEffects(BlockPos centerPos) {
        if (level instanceof ServerLevel serverLevel) {
            double x = centerPos.getX() + 0.5;
            double y = centerPos.getY() + 1.0;
            double z = centerPos.getZ() + 0.5;

            // Explosion de particules
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                x, y, z, 20, 0.5, 0.5, 0.5, 0.1);
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                x, y, z, 10, 0.3, 0.3, 0.3, 0.05);

            // Son de succes
            level.playSound(null, centerPos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0f, 1.2f);
        }
    }

    /**
     * Joue un son d'echec (pchit).
     */
    private void playFailSound() {
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.5f);
        }
    }

    /**
     * Joue un son de consommation partielle.
     */
    private void playPartialSound() {
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.7f, 1.0f);
        }
    }

    // ==================== Animation ====================

    /**
     * @return La vitesse de rotation des conduits en degrés par tick
     */
    public float getConduitRotationSpeed() {
        return conduitRotationSpeed;
    }

    /**
     * Définit la vitesse de rotation des conduits.
     * @param speed Vitesse en degrés par tick (1.0 = 1°/tick = 18°/sec)
     */
    public void setConduitRotationSpeed(float speed) {
        this.conduitRotationSpeed = speed;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * Calcule l'angle de rotation interpolé pour le rendu.
     * @param partialTick Interpolation entre ticks (0.0 - 1.0)
     * @return L'angle de rotation en degrés
     */
    public float getInterpolatedRotationAngle(float partialTick) {
        if (!altarFormed || level == null) return 0f;

        // Utilise gameTime pour une rotation fluide et déterministe
        long gameTime = level.getGameTime();
        return (gameTime + partialTick) * conduitRotationSpeed;
    }

    // ==================== Lifecycle ====================

    @Override
    public void setRemoved() {
        super.setRemoved();
        // Se désinscrire quand le bloc est enlevé
        MultiblockEvents.unregisterController(worldPosition);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // Si déjà formé au chargement, se réinscrire
        if (altarFormed && level != null && !level.isClientSide()) {
            MultiblockEvents.registerActiveController(level, worldPosition);
        }
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("AltarFormed", altarFormed);
        tag.putFloat("ConduitRotationSpeed", conduitRotationSpeed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        altarFormed = tag.getBoolean("AltarFormed");
        if (tag.contains("ConduitRotationSpeed")) {
            conduitRotationSpeed = tag.getFloat("ConduitRotationSpeed");
        }
    }

    // ==================== Sync ====================

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
