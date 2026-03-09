/**
 * ============================================================
 * [HoverbikeSpawnItem.java]
 * Description: Item pour spawner un HoverBee dans le monde
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ApicaEntities       | Registre entites     | Type du HoverBee               |
 * | HoverbikeEntity     | Entite a spawner     | Creation de l'instance         |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ApicaItems.java: Enregistrement
 * - ApicaCreativeTabs.java: Onglet creatif
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.mount;

import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import com.chapeau.apica.core.registry.ApicaEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Clic droit sur un bloc pour spawner un HoverBee au-dessus.
 * L'espece est stockee dans les CustomData de l'item (cle "Species").
 */
public class HoverbikeSpawnItem extends Item {

    private static final String SPECIES_KEY = "Species";

    public HoverbikeSpawnItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        BlockPos pos = context.getClickedPos().above();
        Vec3 spawnPos = Vec3.atBottomCenterOf(pos);

        HoverbikeEntity hoverBee = ApicaEntities.HOVERBIKE.get().create(serverLevel);
        if (hoverBee != null) {
            hoverBee.setPos(spawnPos);
            hoverBee.setYRot(context.getPlayer() != null ? context.getPlayer().getYRot() : 0);
            if (context.getPlayer() != null) {
                hoverBee.setOwner(context.getPlayer());
            }

            String species = getSpecies(context.getItemInHand());
            if (!species.isEmpty()) {
                hoverBee.setSpeciesId(species);
            }

            serverLevel.addFreshEntity(hoverBee);

            if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public Component getName(ItemStack stack) {
        String species = getSpecies(stack);
        if (!species.isEmpty()) {
            return Component.literal("HoverBee (" + species + ")");
        }
        return super.getName(stack);
    }

    /** Cree un ItemStack de HoverBee spawn avec l'espece donnee. */
    public static ItemStack createWithSpecies(Item item, String speciesId) {
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = new CompoundTag();
        tag.putString(SPECIES_KEY, speciesId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    /** Lit l'espece stockee dans le CustomData de l'item. */
    public static String getSpecies(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(SPECIES_KEY)) {
                return tag.getString(SPECIES_KEY);
            }
        }
        return "";
    }
}
