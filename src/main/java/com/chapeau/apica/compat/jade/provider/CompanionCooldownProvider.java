/**
 * ============================================================
 * [CompanionCooldownProvider.java]
 * Description: Provider Jade pour les cooldowns de Companion Bee
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Jade API            | Tooltips in-world    | IComponentProvider             |
 * | CompanionBeeEntity  | Cooldown compagnon   | getFeedCooldownRemaining()     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaJadePlugin (enregistrement provider)
 *
 * ============================================================
 */
package com.chapeau.apica.compat.jade.provider;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.entity.companion.CompanionBeeEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class CompanionCooldownProvider {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "companion_cooldown");

    public enum Server implements IServerDataProvider<EntityAccessor> {
        INSTANCE;

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

    public enum Client implements IEntityComponentProvider {
        INSTANCE;

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
                        .withStyle(ChatFormatting.RED));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}
