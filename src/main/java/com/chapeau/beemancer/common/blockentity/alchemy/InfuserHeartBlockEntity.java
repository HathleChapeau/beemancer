/**
 * ============================================================
 * [InfuserHeartBlockEntity.java]
 * Description: BlockEntity du Coeur de l'Infuser multibloc
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation           |
 * |-------------------------|----------------------|-----------------------|
 * | MultiblockController    | Interface controleur | Implementation        |
 * | BeemancerBlockEntities  | Type registration    | Constructor           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - InfuserHeartBlock.java (creation BlockEntity)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.alchemy;

import com.chapeau.beemancer.common.block.alchemy.InfuserHeartBlock;
import com.chapeau.beemancer.core.multiblock.MultiblockController;
import com.chapeau.beemancer.core.multiblock.MultiblockPattern;
import com.chapeau.beemancer.core.multiblock.MultiblockProperty;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * BlockEntity minimal pour le Infuser Heart.
 * Pattern de multibloc a definir ulterieurement.
 */
public class InfuserHeartBlockEntity extends BlockEntity implements MultiblockController {

    private boolean formed = false;

    public InfuserHeartBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.INFUSER_HEART.get(), pos, state);
    }

    @Override
    public boolean isFormed() {
        return formed;
    }

    @Override
    public MultiblockPattern getPattern() {
        return null;
    }

    @Override
    public BlockPos getControllerPos() {
        return worldPosition;
    }

    @Override
    public void onMultiblockFormed() {
        formed = true;
        if (level != null) {
            BlockState state = level.getBlockState(worldPosition);
            if (state.hasProperty(InfuserHeartBlock.MULTIBLOCK)) {
                level.setBlock(worldPosition, state.setValue(InfuserHeartBlock.MULTIBLOCK, MultiblockProperty.INFUSER), 3);
            }
        }
        setChanged();
    }

    @Override
    public void onMultiblockBroken() {
        formed = false;
        if (level != null) {
            BlockState state = level.getBlockState(worldPosition);
            if (state.hasProperty(InfuserHeartBlock.MULTIBLOCK)) {
                level.setBlock(worldPosition, state.setValue(InfuserHeartBlock.MULTIBLOCK, MultiblockProperty.NONE), 3);
            }
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Formed", formed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        formed = tag.getBoolean("Formed");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("Formed", formed);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
