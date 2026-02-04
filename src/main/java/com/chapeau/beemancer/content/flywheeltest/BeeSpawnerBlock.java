/**
 * ============================================================
 * [BeeSpawnerBlock.java]
 * Description: Bloc de test pour spawner des FlywheelTestBee
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Block               | Base bloc            | Comportement standard          |
 * | BeemancerEntities   | Type entite test     | Spawn des test bees            |
 * | BeeSpeciesManager   | Especes disponibles  | Attribution espece aleatoire   |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BeemancerBlocks.java (enregistrement)
 * - BeemancerItems.java (block item)
 * - BeemancerCreativeTabs.java (onglet creatif)
 *
 * ============================================================
 */
package com.chapeau.beemancer.content.flywheeltest;

import com.chapeau.beemancer.core.bee.BeeSpeciesManager;
import com.chapeau.beemancer.core.registry.BeemancerEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BeeSpawnerBlock extends Block {

    public BeeSpawnerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                               BlockPos pos, Player player, InteractionHand hand,
                                               BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown()) {
            killAllTestBees(level, pos);
        } else {
            spawnTestBees(level, pos);
        }
        return ItemInteractionResult.SUCCESS;
    }

    private void spawnTestBees(Level level, BlockPos pos) {
        Set<String> speciesIds = BeeSpeciesManager.getAllSpeciesIds();
        List<String> speciesList = new ArrayList<>(speciesIds);

        if (speciesList.isEmpty()) {
            return;
        }

        int count = 5 + level.random.nextInt(6);
        for (int i = 0; i < count; i++) {
            FlywheelTestBeeEntity bee = BeemancerEntities.FLYWHEEL_TEST_BEE.get().create(level);
            if (bee == null) continue;

            String species = speciesList.get(level.random.nextInt(speciesList.size()));
            bee.setSpeciesId(species);

            double x = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 6.0;
            double y = pos.getY() + 1.5 + level.random.nextDouble() * 3.0;
            double z = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 6.0;

            bee.setPos(x, y, z);
            bee.setYRot(level.random.nextFloat() * 360.0f);
            level.addFreshEntity(bee);
        }
    }

    private void killAllTestBees(Level level, BlockPos pos) {
        AABB area = new AABB(pos).inflate(32.0);
        List<FlywheelTestBeeEntity> bees = level.getEntitiesOfClass(FlywheelTestBeeEntity.class, area);
        for (FlywheelTestBeeEntity bee : bees) {
            bee.discard();
        }
    }
}
