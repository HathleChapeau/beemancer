/**
 * ============================================================
 * [ApicaJadePlugin.java]
 * Description: Plugin Jade pour Apica - point d'entree des providers
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Jade API            | Tooltips in-world    | IWailaPlugin, providers        |
 * | ApiCooldownProvider | Cooldown Api         | Affiche cooldown en debug      |
 * | CompanionProvider   | Cooldown compagnon   | Affiche cooldown en debug      |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - Jade (decouverte automatique via @WailaPlugin)
 *
 * NOTE: Ce fichier n'est charge que si Jade est present.
 * Le mod fonctionne normalement sans Jade.
 *
 * ============================================================
 */
package com.chapeau.apica.compat.jade;

import com.chapeau.apica.common.block.api.ApiBlock;
import com.chapeau.apica.common.block.api.ApiBlockEntity;
import com.chapeau.apica.common.entity.companion.CompanionBeeEntity;
import com.chapeau.apica.compat.jade.provider.ApiCooldownProvider;
import com.chapeau.apica.compat.jade.provider.CompanionCooldownProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Plugin Jade pour Apica.
 * Les providers sont dans le sous-package provider/ pour faciliter la maintenance.
 */
@WailaPlugin("")
public class ApicaJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        // Server-side data providers
        registration.registerBlockDataProvider(ApiCooldownProvider.Server.INSTANCE, ApiBlockEntity.class);
        registration.registerEntityDataProvider(CompanionCooldownProvider.Server.INSTANCE, CompanionBeeEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        // Client-side tooltip providers
        registration.registerBlockComponent(ApiCooldownProvider.Client.INSTANCE, ApiBlock.class);
        registration.registerEntityComponent(CompanionCooldownProvider.Client.INSTANCE, CompanionBeeEntity.class);
    }
}
