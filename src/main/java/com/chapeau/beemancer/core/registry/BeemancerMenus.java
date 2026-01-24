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
import com.chapeau.beemancer.common.menu.alchemy.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class BeemancerMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = 
            DeferredRegister.create(Registries.MENU, Beemancer.MOD_ID);

    // --- CORE MENUS ---
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

    // --- ALCHEMY MENUS ---
    public static final Supplier<MenuType<ManualCentrifugeMenu>> MANUAL_CENTRIFUGE = 
            MENUS.register("manual_centrifuge",
                    () -> IMenuTypeExtension.create(ManualCentrifugeMenu::new));

    public static final Supplier<MenuType<PoweredCentrifugeMenu>> POWERED_CENTRIFUGE = 
            MENUS.register("powered_centrifuge",
                    () -> IMenuTypeExtension.create(PoweredCentrifugeMenu::new));

    public static final Supplier<MenuType<HoneyTankMenu>> HONEY_TANK =
            MENUS.register("honey_tank",
                    () -> IMenuTypeExtension.create(HoneyTankMenu::new));

    public static final Supplier<MenuType<CreativeTankMenu>> CREATIVE_TANK =
            MENUS.register("creative_tank",
                    () -> IMenuTypeExtension.create(CreativeTankMenu::new));

    public static final Supplier<MenuType<CrystallizerMenu>> CRYSTALLIZER = 
            MENUS.register("crystallizer",
                    () -> IMenuTypeExtension.create(CrystallizerMenu::new));

    public static final Supplier<MenuType<AlembicMenu>> ALEMBIC =
            MENUS.register("alembic",
                    () -> IMenuTypeExtension.create(AlembicMenu::new));

    public static final Supplier<MenuType<InfuserMenu>> INFUSER =
            MENUS.register("infuser",
                    () -> IMenuTypeExtension.create(InfuserMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
