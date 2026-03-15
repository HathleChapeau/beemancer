/**
 * ============================================================
 * [ApiCooldownProvider.java]
 * Description: Provider Jade pour les cooldowns d'Api
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Jade API            | Tooltips in-world    | IComponentProvider             |
 * | ApiBlockEntity      | Cooldown Api         | getRemainingCooldown()         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaJadePlugin (enregistrement provider)
 *
 * ============================================================
 */
package com.chapeau.apica.compat.jade.provider;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.block.api.ApiBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class ApiCooldownProvider {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "api_cooldown");

    public enum Server implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

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

    public enum Client implements IBlockComponentProvider {
        INSTANCE;

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
                        .withStyle(ChatFormatting.RED));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}
