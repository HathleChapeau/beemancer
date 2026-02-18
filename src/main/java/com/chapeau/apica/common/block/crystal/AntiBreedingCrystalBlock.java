/**
 * ============================================================
 * [AntiBreedingCrystalBlock.java]
 * Description: Cristal desactivant le breeding des ruches quand place au-dessus
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ParticleEmitter     | Particules client    | Effet visuel ambiant           |
 * | ApicaParticles      | Types particules     | RUNE particle                  |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - MagicHiveBlockEntity (detection mode anti-breeding via pos.above())
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.crystal;

import com.chapeau.apica.client.particle.ParticleEmitter;
import com.chapeau.apica.core.registry.ApicaParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AntiBreedingCrystalBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 14, 12);

    public AntiBreedingCrystalBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        new ParticleEmitter(ApicaParticles.RUNE.get())
            .at(pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5)
            .speed(0, 0.01, 0)
            .spread(0.25, 0.15, 0.25)
            .speedVariance(0.01, 0.01, 0.01)
            .count(2)
            .lifetime(10)
            .gravity(-0.001f)
            .scale(0.05f)
            .fadeOut()
            .spawn(level);
    }
}
