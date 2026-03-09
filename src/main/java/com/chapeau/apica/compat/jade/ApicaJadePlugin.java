/**
 * ============================================================
 * [ApicaJadePlugin.java]
 * Description: Integration Jade — affiche les cooldowns en mode creatif
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Jade API            | Tooltips in-world    | IWailaPlugin, providers        |
 * | ApiBlockEntity      | Cooldown Api         | getRemainingCooldown()         |
 * | CompanionBeeEntity  | Cooldown compagnon   | getFeedCooldownRemaining()     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - Jade (decouverte automatique via @WailaPlugin)
 *
 * ============================================================
 */
package com.chapeau.apica.compat.jade;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.block.api.ApiBlock;
import com.chapeau.apica.common.block.api.ApiBlockEntity;
import com.chapeau.apica.common.entity.companion.CompanionBeeEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Plugin Jade pour Apica.
 * Affiche les cooldowns restants d'Api et des compagnons en mode creatif uniquement.
 * Classe chargee exclusivement par Jade — le mod fonctionne sans Jade.
 */
@WailaPlugin("")
public class ApicaJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(ApiCooldownServerProvider.INSTANCE, ApiBlockEntity.class);
        registration.registerEntityDataProvider(CompanionCooldownServerProvider.INSTANCE, CompanionBeeEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(ApiCooldownProvider.INSTANCE, ApiBlock.class);
        registration.registerEntityComponent(CompanionCooldownProvider.INSTANCE, CompanionBeeEntity.class);
    }

    // ==================== Api Block ====================

    enum ApiCooldownServerProvider implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
                Apica.MOD_ID, "api_cooldown");

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!accessor.getPlayer().isCreative()) return;
            if (accessor.getBlockEntity() instanceof ApiBlockEntity api) {
                long remaining = api.getRemainingCooldown(accessor.getLevel().getGameTime());
                data.putLong("ApiCooldown", remaining);
                data.putInt("ApiLevel", api.getApiLevel());
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    enum ApiCooldownProvider implements IComponentProvider<BlockAccessor> {
        INSTANCE;

        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
                Apica.MOD_ID, "api_cooldown");

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!accessor.getPlayer().isCreative()) return;

            CompoundTag data = accessor.getServerData();
            if (!data.contains("ApiCooldown")) return;

            int level = data.getInt("ApiLevel");
            tooltip.add(Component.literal("Level: " + level));

            long cooldown = data.getLong("ApiCooldown");
            if (cooldown > 0) {
                int seconds = (int) (cooldown / 20);
                int min = seconds / 60;
                int sec = seconds % 60;
                tooltip.add(Component.literal("Cooldown: " + min + "m " + sec + "s")
                        .withStyle(net.minecraft.ChatFormatting.RED));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    // ==================== Companion Bee ====================

    enum CompanionCooldownServerProvider implements IServerDataProvider<EntityAccessor> {
        INSTANCE;

        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
                Apica.MOD_ID, "companion_cooldown");

        @Override
        public void appendServerData(CompoundTag data, EntityAccessor accessor) {
            if (!accessor.getPlayer().isCreative()) return;
            if (accessor.getEntity() instanceof CompanionBeeEntity companion) {
                long remaining = companion.getFeedCooldownRemaining();
                data.putLong("CompanionCooldown", remaining);
                data.putString("CompanionType", companion.getCompanionType().name());
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    enum CompanionCooldownProvider implements IComponentProvider<EntityAccessor> {
        INSTANCE;

        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
                Apica.MOD_ID, "companion_cooldown");

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (!accessor.getPlayer().isCreative()) return;

            CompoundTag data = accessor.getServerData();
            if (!data.contains("CompanionCooldown")) return;

            String type = data.getString("CompanionType");
            tooltip.add(Component.literal("Type: " + type));

            long cooldown = data.getLong("CompanionCooldown");
            if (cooldown > 0) {
                int seconds = (int) (cooldown / 20);
                int min = seconds / 60;
                int sec = seconds % 60;
                tooltip.add(Component.literal("Feed cooldown: " + min + "m " + sec + "s")
                        .withStyle(net.minecraft.ChatFormatting.RED));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}
