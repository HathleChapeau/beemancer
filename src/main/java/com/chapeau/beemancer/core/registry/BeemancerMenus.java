/**
 * ============================================================
 * [BeemancerMenus.java]
 * Description: Registre centralisé de tous les menus/containers
 * ============================================================
 */
package com.chapeau.beemancer.core.registry;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.menu.BeeCreatorMenu;
import com.chapeau.beemancer.common.menu.IncubatorMenu;
import com.chapeau.beemancer.common.menu.MagicHiveMenu;
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

    public static final Supplier<MenuType<BeeCreatorMenu>> BEE_CREATOR = 
            MENUS.register("bee_creator",
                    () -> IMenuTypeExtension.create(BeeCreatorMenu::new));

    public static final Supplier<MenuType<MagicHiveMenu>> MAGIC_HIVE = 
            MENUS.register("magic_hive",
                    () -> IMenuTypeExtension.create(MagicHiveMenu::new));

    public static final Supplier<MenuType<IncubatorMenu>> INCUBATOR = 
            MENUS.register("incubator",
                    () -> IMenuTypeExtension.create(IncubatorMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
