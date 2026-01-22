/**
 * ============================================================
 * [HarvestHelper.java]
 * Description: Utilitaire pour la logique de récolte par type de bloc
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | (aucune)            |                      |                                |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - HarvestingBehaviorGoal.java: Logique de récolte
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilitaire pour la logique de récolte des différents types de blocs.
 */
public final class HarvestHelper {
    
    private HarvestHelper() {}
    
    /**
     * Type de récolte pour un bloc.
     */
    public enum HarvestType {
        CROP,           // Crops (wheat, carrots, potatoes, etc.) - récolte si mature, replante
        NETHER_WART,    // Nether wart - récolte si mature, replante
        MUSHROOM,       // Champignons - casse et récupère drops
        CRYSTAL,        // Cristaux d'améthyste - casse et récupère drops
        FLOWER,         // Fleurs normales - casse et récupère drops
        TREE,           // Arbres - non supporté pour l'instant
        UNSUPPORTED     // Type non supporté
    }
    
    /**
     * Détermine le type de récolte pour un bloc.
     */
    public static HarvestType getHarvestType(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        
        // Nether wart
        if (block == Blocks.NETHER_WART) {
            return HarvestType.NETHER_WART;
        }
        
        // Crops
        if (state.is(BlockTags.CROPS)) {
            return HarvestType.CROP;
        }
        
        // Mushrooms
        if (block == Blocks.BROWN_MUSHROOM || block == Blocks.RED_MUSHROOM ||
            block == Blocks.BROWN_MUSHROOM_BLOCK || block == Blocks.RED_MUSHROOM_BLOCK ||
            block == Blocks.MUSHROOM_STEM ||
            block == Blocks.CRIMSON_FUNGUS || block == Blocks.WARPED_FUNGUS) {
            return HarvestType.MUSHROOM;
        }
        
        // Crystals
        if (block == Blocks.AMETHYST_CLUSTER || block == Blocks.LARGE_AMETHYST_BUD ||
            block == Blocks.MEDIUM_AMETHYST_BUD || block == Blocks.SMALL_AMETHYST_BUD ||
            block == Blocks.BUDDING_AMETHYST || block == Blocks.AMETHYST_BLOCK) {
            return HarvestType.CRYSTAL;
        }
        
        // Trees (logs, leaves)
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
            return HarvestType.TREE;
        }
        
        // Default: Flower
        if (state.is(BlockTags.FLOWERS)) {
            return HarvestType.FLOWER;
        }
        
        return HarvestType.UNSUPPORTED;
    }
    
    /**
     * Vérifie si un bloc est prêt à être récolté.
     */
    public static boolean isReadyToHarvest(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        HarvestType type = getHarvestType(level, pos);
        
        switch (type) {
            case CROP:
                if (block instanceof CropBlock crop) {
                    return crop.isMaxAge(state);
                }
                return false;
                
            case NETHER_WART:
                if (state.hasProperty(NetherWartBlock.AGE)) {
                    return state.getValue(NetherWartBlock.AGE) >= 3;
                }
                return false;
                
            case MUSHROOM:
            case CRYSTAL:
            case FLOWER:
                return true; // Always harvestable
                
            case TREE:
            case UNSUPPORTED:
            default:
                return false;
        }
    }
    
    /**
     * Effectue la récolte et retourne les items récoltés.
     * 
     * @param level Le monde (doit être ServerLevel)
     * @param pos Position du bloc
     * @return Liste des items récoltés
     */
    public static List<ItemStack> harvest(Level level, BlockPos pos) {
        List<ItemStack> drops = new ArrayList<>();
        
        if (!(level instanceof ServerLevel serverLevel)) {
            return drops;
        }
        
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        HarvestType type = getHarvestType(level, pos);
        
        switch (type) {
            case CROP:
                drops.addAll(harvestCrop(serverLevel, pos, state, block));
                break;
                
            case NETHER_WART:
                drops.addAll(harvestNetherWart(serverLevel, pos, state));
                break;
                
            case MUSHROOM:
            case CRYSTAL:
            case FLOWER:
                drops.addAll(breakBlock(serverLevel, pos, state, block));
                break;
                
            case TREE:
            case UNSUPPORTED:
                // Do nothing
                break;
        }
        
        return drops;
    }
    
    private static List<ItemStack> harvestCrop(ServerLevel level, BlockPos pos, BlockState state, Block block) {
        List<ItemStack> drops = new ArrayList<>();
        
        if (!(block instanceof CropBlock crop)) {
            return drops;
        }
        
        // Get drops
        List<ItemStack> blockDrops = Block.getDrops(state, level, pos, null);
        
        // Separate seeds from produce
        ItemStack seedToReplant = ItemStack.EMPTY;
        for (ItemStack drop : blockDrops) {
            // Identify seed items for replanting
            if (isSeedItem(drop, block)) {
                if (seedToReplant.isEmpty()) {
                    seedToReplant = drop.copyWithCount(1);
                    if (drop.getCount() > 1) {
                        drops.add(drop.copyWithCount(drop.getCount() - 1));
                    }
                } else {
                    drops.add(drop.copy());
                }
            } else {
                drops.add(drop.copy());
            }
        }
        
        // Replant
        BlockState seedState = crop.getStateForAge(0);
        level.setBlock(pos, seedState, Block.UPDATE_ALL);
        
        return drops;
    }
    
    private static boolean isSeedItem(ItemStack stack, Block crop) {
        if (crop == Blocks.WHEAT) return stack.is(Items.WHEAT_SEEDS);
        if (crop == Blocks.CARROTS) return stack.is(Items.CARROT);
        if (crop == Blocks.POTATOES) return stack.is(Items.POTATO);
        if (crop == Blocks.BEETROOTS) return stack.is(Items.BEETROOT_SEEDS);
        if (crop == Blocks.MELON_STEM) return stack.is(Items.MELON_SEEDS);
        if (crop == Blocks.PUMPKIN_STEM) return stack.is(Items.PUMPKIN_SEEDS);
        if (crop == Blocks.TORCHFLOWER_CROP) return stack.is(Items.TORCHFLOWER_SEEDS);
        if (crop == Blocks.PITCHER_CROP) return stack.is(Items.PITCHER_POD);
        return false;
    }
    
    private static List<ItemStack> harvestNetherWart(ServerLevel level, BlockPos pos, BlockState state) {
        List<ItemStack> drops = new ArrayList<>();
        
        // Get drops
        List<ItemStack> blockDrops = Block.getDrops(state, level, pos, null);
        
        // Keep one for replanting
        boolean keptOne = false;
        for (ItemStack drop : blockDrops) {
            if (drop.is(Items.NETHER_WART)) {
                if (!keptOne) {
                    keptOne = true;
                    if (drop.getCount() > 1) {
                        drops.add(drop.copyWithCount(drop.getCount() - 1));
                    }
                } else {
                    drops.add(drop.copy());
                }
            } else {
                drops.add(drop.copy());
            }
        }
        
        // Replant
        level.setBlock(pos, Blocks.NETHER_WART.defaultBlockState(), Block.UPDATE_ALL);
        
        return drops;
    }
    
    private static List<ItemStack> breakBlock(ServerLevel level, BlockPos pos, BlockState state, Block block) {
        List<ItemStack> drops = new ArrayList<>();
        
        // Get drops
        List<ItemStack> blockDrops = Block.getDrops(state, level, pos, null);
        for (ItemStack drop : blockDrops) {
            drops.add(drop.copy());
        }
        
        // Remove block
        level.destroyBlock(pos, false);
        
        return drops;
    }
}
