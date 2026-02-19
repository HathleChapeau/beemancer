/**
 * ============================================================
 * [ApicaMenus.java]
 * Description: Registre centralisé de tous les menus/containers
 * ============================================================
 */
package com.chapeau.apica.core.registry;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.menu.IncubatorMenu;
import com.chapeau.apica.common.menu.InjectorMenu;
import com.chapeau.apica.common.menu.MagicHiveMenu;
import com.chapeau.apica.common.menu.ResonatorMenu;
import com.chapeau.apica.common.menu.alchemy.AlembicMenu;
import com.chapeau.apica.common.menu.alchemy.CreativeTankMenu;
import com.chapeau.apica.common.menu.alchemy.CrystallizerMenu;
import com.chapeau.apica.common.menu.alchemy.HoneyTankMenu;
import com.chapeau.apica.common.menu.alchemy.InfuserMenu;
import com.chapeau.apica.common.menu.alchemy.ManualCentrifugeMenu;
import com.chapeau.apica.common.menu.alchemy.MultiblockTankMenu;
import com.chapeau.apica.common.menu.alchemy.ApicaFurnaceMenu;
import com.chapeau.apica.common.menu.alchemy.PoweredCentrifugeMenu;
import com.chapeau.apica.common.menu.storage.NetworkInterfaceMenu;
import com.chapeau.apica.common.menu.storage.StorageTerminalMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ApicaMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = 
            DeferredRegister.create(Registries.MENU, Apica.MOD_ID);

    // --- CORE MENUS ---
    public static final Supplier<MenuType<StorageTerminalMenu>> STORAGE_TERMINAL =
            MENUS.register("storage_terminal",
                    () -> IMenuTypeExtension.create(StorageTerminalMenu::new));

    public static final Supplier<MenuType<NetworkInterfaceMenu>> NETWORK_INTERFACE =
            MENUS.register("network_interface",
                    () -> IMenuTypeExtension.create(NetworkInterfaceMenu::new));

    public static final Supplier<MenuType<MagicHiveMenu>> MAGIC_HIVE =
            MENUS.register("magic_hive",
                    () -> IMenuTypeExtension.create(MagicHiveMenu::new));

    public static final Supplier<MenuType<IncubatorMenu>> INCUBATOR =
            MENUS.register("incubator",
                    () -> IMenuTypeExtension.create(IncubatorMenu::new));

    public static final Supplier<MenuType<InjectorMenu>> INJECTOR =
            MENUS.register("injector",
                    () -> IMenuTypeExtension.create(InjectorMenu::new));

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

    public static final Supplier<MenuType<MultiblockTankMenu>> MULTIBLOCK_TANK =
            MENUS.register("multiblock_tank",
                    () -> IMenuTypeExtension.create(MultiblockTankMenu::new));

    // --- APICA FURNACES ---
    public static final Supplier<MenuType<ApicaFurnaceMenu>> APICA_FURNACE =
            MENUS.register("apica_furnace",
                    () -> IMenuTypeExtension.create(ApicaFurnaceMenu::new));

    // --- RESONATOR ---
    public static final Supplier<MenuType<ResonatorMenu>> RESONATOR =
            MENUS.register("resonator",
                    () -> IMenuTypeExtension.create(ResonatorMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
