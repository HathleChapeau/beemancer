/**
 * ============================================================
 * [BeeNestBlock.java]
 * Description: Nid d'abeille naturel parametre par espece
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance               | Raison                  | Utilisation                    |
 * |--------------------------|------------------------|--------------------------------|
 * | BeeNestBlockEntity       | BlockEntity associe    | Tick spawning abeilles         |
 * | ApicaBlockEntities   | Registre BE            | Type pour ticker               |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaBlocks.java (registration)
 * - BeeNestFeature.java (worldgen placement)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.hive;

import com.chapeau.apica.core.registry.ApicaBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Bloc nid d'abeille genere naturellement dans le monde.
 * Chaque variante (species) correspond a une espece Tier I.
 * Casser le nid drop un honeycomb (loot table).
 */
public class BeeNestBlock extends Block implements EntityBlock {

    public static final EnumProperty<NestSpecies> SPECIES = EnumProperty.create("species", NestSpecies.class);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BeeNestBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(SPECIES, NestSpecies.MEADOW)
                .setValue(FACING, Direction.SOUTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SPECIES, FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BeeNestBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        if (type != ApicaBlockEntities.BEE_NEST.get()) return null;
        return (lvl, pos, st, be) -> ((BeeNestBlockEntity) be).serverTick();
    }

    public enum NestSpecies implements StringRepresentable {
        MEADOW("meadow"),
        FOREST("forest"),
        ROCKY("rocky"),
        RIVER("river"),
        DUNE("dune"),
        NETHER("nether"),
        END("end");

        private final String name;

        NestSpecies(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public static NestSpecies fromSpeciesId(String id) {
            for (NestSpecies s : values()) {
                if (s.name.equals(id)) return s;
            }
            return MEADOW;
        }
    }
}
