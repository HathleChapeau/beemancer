/**
 * ============================================================
 * [HoneyTankBlock.java]
 * Description: Tank de stockage pour fluides Apica
 * ============================================================
 */
package com.chapeau.apica.common.block.alchemy;

import com.chapeau.apica.common.blockentity.alchemy.HoneyTankBlockEntity;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;
import java.util.List;

public class HoneyTankBlock extends BaseEntityBlock {
    public static final MapCodec<HoneyTankBlock> CODEC = simpleCodec(HoneyTankBlock::new);

    public HoneyTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HoneyTankBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ApicaBlockEntities.HONEY_TANK.get(),
            HoneyTankBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HoneyTankBlockEntity tank) {
                if (FluidUtil.interactWithFluidHandler(player, hand, tank.getFluidTank())) {
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof HoneyTankBlockEntity tank && !tank.getFluid().isEmpty()) {
            for (ItemStack drop : drops) {
                if (drop.is(this.asItem())) {
                    be.saveToItem(drop, builder.getLevel().registryAccess());
                }
            }
        }
        return drops;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        CustomData customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData != null && context.registries() != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("Fluid")) {
                FluidTank tempTank = new FluidTank(HoneyTankBlockEntity.CAPACITY);
                tempTank.readFromNBT(context.registries(), tag.getCompound("Fluid"));
                FluidStack fluid = tempTank.getFluid();
                if (!fluid.isEmpty()) {
                    String name = fluid.getHoverName().getString();
                    tooltip.add(Component.literal(name + ": " + tempTank.getFluidAmount()
                        + " / " + tempTank.getCapacity() + " mB")
                        .withStyle(style -> style.withColor(0xAAAAAA)));
                }
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HoneyTankBlockEntity tank) {
                serverPlayer.openMenu(tank, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
