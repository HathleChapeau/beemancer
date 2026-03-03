/**
 * ============================================================
 * [ApicaDimension.java]
 * Description: Constantes et utilitaires pour la dimension Apica
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Apica               | MOD_ID               | Construction ResourceLocation  |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ApicaChunkGenerator.java
 * - ApicaPortalBlock.java
 *
 * ============================================================
 */
package com.chapeau.apica.content.dimension;

import com.chapeau.apica.Apica;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

@EventBusSubscriber(modid = Apica.MOD_ID)
public class ApicaDimension {

    public static final ResourceKey<Level> LEVEL_KEY = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "apica")
    );

    public static final ResourceKey<DimensionType> TYPE_KEY = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "apica")
    );

    public static boolean isApicaDimension(Level level) {
        return LEVEL_KEY.equals(level.dimension());
    }

    @SubscribeEvent
    public static void onMobSpawn(FinalizeSpawnEvent event) {
        if (event.getLevel().getLevel().dimension().equals(LEVEL_KEY)) {
            event.setSpawnCancelled(true);
        }
    }
}
