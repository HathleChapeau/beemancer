/**
 * ============================================================
 * [StorageHiveBlock.java]
 * Description: Bloc ruche de stockage (3 tiers) lié au Storage Controller
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                  | Raison                | Utilisation                    |
 * |-----------------------------|----------------------|--------------------------------|
 * | Block                       | Base Minecraft       | Bloc avec blockstate           |
 * | EntityBlock                 | BlockEntity          | Création du BlockEntity        |
 * | StorageHiveBlockEntity      | Logique              | Stockage controllerPos         |
 * | StorageControllerBlockEntity| Controller           | Unlinking on break             |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - BeemancerBlocks.java (enregistrement 3 tiers)
 * - StorageEvents.java (linking en edit mode)
 * - StorageControllerBlockEntity.java (calcul bees/multiplier)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.storage;

import com.chapeau.beemancer.common.blockentity.storage.StorageControllerBlockEntity;
import com.chapeau.beemancer.common.blockentity.storage.StorageHiveBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Bloc ruche de stockage liée directement au Storage Controller.
 * Chaque hive augmente le nombre de delivery bees, multiplie la consommation
 * de miel, et ajoute 1 slot essence au controller.
 *
 * 3 tiers: T1=1 bee, T2=2 bees, T3=3 bees.
 * Honey multiplier: T1=x1.5, T2=x2.0, T3=x2.5 (multiplicatif).
 * Max 4 hives par controller.
 *
 * 3 états visuels via HIVE_STATE:
 * - UNLINKED: posé mais non relié à un controller
 * - LINKED: relié mais controller non formé (multibloc inactif)
 * - ACTIVE: relié et controller formé (multibloc actif)
 */
public class StorageHiveBlock extends Block implements EntityBlock {

    public static final EnumProperty<HiveState> HIVE_STATE = EnumProperty.create("hive_state", HiveState.class);

    private final int tier;

    public StorageHiveBlock(Properties properties) {
        this(properties, 1);
    }

    public StorageHiveBlock(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(HIVE_STATE, HiveState.UNLINKED));
    }

    public int getTier() {
        return tier;
    }

    /**
     * Retourne le multiplicateur de consommation de miel pour ce tier.
     */
    public float getHoneyMultiplier() {
        return switch (tier) {
            case 2 -> ControllerStats.HIVE_MULTIPLIER_T2;
            case 3 -> ControllerStats.HIVE_MULTIPLIER_T3;
            default -> ControllerStats.HIVE_MULTIPLIER_T1;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HIVE_STATE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StorageHiveBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        if (type != BeemancerBlockEntities.STORAGE_HIVE.get()) return null;
        return (lvl, pos, st, be) -> ((StorageHiveBlockEntity) be).serverTick();
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof StorageHiveBlockEntity hive) {
                StorageControllerBlockEntity controller = hive.getController();
                if (controller != null) {
                    controller.unlinkHive(pos);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * États visuels de la Storage Hive.
     */
    public enum HiveState implements StringRepresentable {
        UNLINKED("unlinked"),
        LINKED("linked"),
        ACTIVE("active");

        private final String name;

        HiveState(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
