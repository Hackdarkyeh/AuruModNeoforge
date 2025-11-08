package aurum.aurum.init.GUI;

import aurum.aurum.Aurum;
import aurum.aurum.client.gui.SoulModificationTableMenu.SoulModificationTableMenu;
import aurum.aurum.client.gui.ArmorTable.ArmorTableMenu;
import aurum.aurum.client.gui.DarkEnergyTable.DarkEnergyTableMenu;
import aurum.aurum.client.gui.EnergyGeneratorBlock.EnergyGeneratorMenu;
import aurum.aurum.client.gui.ExtractorBlock.ExtractorMenu;
import aurum.aurum.client.gui.Pedestal.PedestalMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;


public class ModMenuType <T extends AbstractContainerMenu> implements FeatureElement, net.neoforged.neoforge.common.extensions.IMenuTypeExtension<T> {

    public static final DeferredRegister<MenuType<?>> MENU_TYPE_REGISTRY = DeferredRegister.create(Registries.MENU, Aurum.MODID);

    public static DeferredHolder<MenuType<?>,MenuType<EnergyGeneratorMenu>> ENERGY_GENERATOR_MENU = MENU_TYPE_REGISTRY.register(
            "energy_generator_menu", () -> new MenuType<>(EnergyGeneratorMenu::new, FeatureFlags.VANILLA_SET));

    public static DeferredHolder<MenuType<?>,MenuType<ExtractorMenu>> EXTRACTOR_MENU = MENU_TYPE_REGISTRY.register(
            "extractor_menu", () -> new MenuType<>(ExtractorMenu::new, FeatureFlags.VANILLA_SET));

    public static DeferredHolder<MenuType<?>,MenuType<ArmorTableMenu>> ARMOR_TABLE_MENU = MENU_TYPE_REGISTRY.register(
            "armor_table_menu", () -> new MenuType<>(ArmorTableMenu::new, FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<PedestalMenu>> PEDESTAL_MENU =
            registerMenuType("pedestal_menu", PedestalMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<SoulModificationTableMenu>> SOUL_MODIFICATION_TABLE =
            registerMenuType("soul_modification_table_menu", SoulModificationTableMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<DarkEnergyTableMenu>> DARK_ENERGY_TABLE =
            MENU_TYPE_REGISTRY.register("dark_energy_table_menu", () -> new MenuType<>(DarkEnergyTableMenu::new, FeatureFlags.VANILLA_SET));;

    private static <T extends AbstractContainerMenu>DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(String name,
                                                                                                              IContainerFactory<T> factory) {
        return MENU_TYPE_REGISTRY.register(name, () -> IMenuTypeExtension.create(factory));
    }

    //public static final MenuType<ArmorTableMenu> ENERGY_GENERATOR_MENU = register("energy_generator_menu", ArmorTableMenu::new);

    private final FeatureFlagSet requiredFeatures;
    private final MenuType.MenuSupplier<T> constructor;

    private static <T extends AbstractContainerMenu> MenuType<T> register(String pKey, MenuType.MenuSupplier<T> pFactory) {


        return Registry.register(BuiltInRegistries.MENU, pKey, new MenuType<>(pFactory, FeatureFlags.VANILLA_SET));
    }

    private static <T extends AbstractContainerMenu> MenuType<T> register(String pKey, MenuType.MenuSupplier<T> pFactory, FeatureFlag... pRequiredFeatures) {
        return Registry.register(BuiltInRegistries.MENU, pKey, new MenuType<>(pFactory, FeatureFlags.REGISTRY.subset(pRequiredFeatures)));
    }


    public ModMenuType(MenuType.MenuSupplier<T> pConstructor, FeatureFlagSet pRequiredFeatures) {
        this.constructor = pConstructor;
        this.requiredFeatures = pRequiredFeatures;
    }

    public T create(int pContainerId, Inventory pPlayerInventory) {
        return this.constructor.create(pContainerId, pPlayerInventory);
    }

    @Override
    public T create(int pContainerId, Inventory pPlayerInventory, net.minecraft.network.RegistryFriendlyByteBuf extraData) {
        if (this.constructor instanceof net.neoforged.neoforge.network.IContainerFactory) {
            return ((net.neoforged.neoforge.network.IContainerFactory<T>) this.constructor).create(pContainerId, pPlayerInventory, extraData);
        }
        return create(pContainerId, pPlayerInventory);
    }

    @Override
    public FeatureFlagSet requiredFeatures() {
        return this.requiredFeatures;
    }

    public interface MenuSupplier<T extends AbstractContainerMenu> {
        T create(int pContainerId, Inventory pPlayerInventory);
    }
}
