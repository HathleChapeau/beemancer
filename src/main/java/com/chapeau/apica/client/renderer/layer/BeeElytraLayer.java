/**
 * ============================================================
 * [BeeElytraLayer.java]
 * Description: Render layer for Bee Elytra wings on player model
 * ============================================================
 *
 * DEPENDENCIES:
 * ------------------------------------------------------------
 * | Dependency          | Reason                | Usage                          |
 * |---------------------|----------------------|--------------------------------|
 * | ElytraLayer         | Vanilla elytra layer | Extends for custom rendering   |
 * | ApicaItems          | Registry reference   | Item check (BEE_ELYTRA)        |
 * | Apica               | Mod ID               | Texture ResourceLocation       |
 * ------------------------------------------------------------
 *
 * USED BY:
 * - ClientSetup (layer registration via EntityRenderersEvent.AddLayers)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.layer;

import com.chapeau.apica.Apica;
import com.chapeau.apica.core.registry.ApicaItems;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BeeElytraLayer<T extends LivingEntity, M extends EntityModel<T>> extends ElytraLayer<T, M> {

    private static final ResourceLocation BEE_WINGS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/entity/bee_elytra.png");

    public BeeElytraLayer(RenderLayerParent<T, M> renderer, EntityModelSet modelSet) {
        super(renderer, modelSet);
    }

    @Override
    public boolean shouldRender(ItemStack stack, T entity) {
        return stack.is(ApicaItems.BEE_ELYTRA.get());
    }

    @Override
    public ResourceLocation getElytraTexture(ItemStack stack, T entity) {
        return BEE_WINGS_TEXTURE;
    }
}
