/**
 * ============================================================
 * [BeemancerMenus.java]
 * Description: Registre centralisé de tous les menus/containers
 * ============================================================
 * 
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison              | Utilisation         |
 * |---------------------|--------------------|--------------------|
 * | Beemancer           | MOD_ID             | Clé du registre    |
 * | StorageCrateMenu    | Menu à enregistrer | Création du type   |
 * ------------------------------------------------------------
 * 
 * UTILISÉ PAR:
 * - Beemancer.java (enregistrement)
 * - StorageCrateBlockEntity.java (ouverture du menu)
 * 
 * ============================================================
 */
package com.chapeau.beemancer.core.registry;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.menu.StorageCrateMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class BeemancerMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = 
            DeferredRegister.create(Registries.MENU, Beemancer.MOD_ID);

    public static final Supplier<MenuType<StorageCrateMenu>> STORAGE_CRATE = 
            MENUS.register("storage_crate",
                    () -> IMenuTypeExtension.create(StorageCrateMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
