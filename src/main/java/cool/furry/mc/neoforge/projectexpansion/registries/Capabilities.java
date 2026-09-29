package cool.furry.mc.neoforge.projectexpansion.registries;
import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.capability.IAlchemicalBookLocationsProvider;
import cool.furry.mc.neoforge.projectexpansion.util.IEmcStorageBigInteger;
import moze_intel.projecte.api.capabilities.PEBlockCapability;
import moze_intel.projecte.api.capabilities.PEEntityCapability;
import moze_intel.projecte.api.capabilities.PEItemCapability;
import net.minecraft.core.Direction;
public final class Capabilities {
    public static final PEEntityCapability<IAlchemicalBookLocationsProvider> ALCHEMICAL_BOOK_LOCATIONS_ENTITY =
        PEEntityCapability.create(Main.rl("alchemical_book_locations"), IAlchemicalBookLocationsProvider.class);
    public static final PEItemCapability<IAlchemicalBookLocationsProvider> ALCHEMICAL_BOOK_LOCATIONS_ITEM =
        PEItemCapability.create(Main.rl("alchemical_book_locations"), IAlchemicalBookLocationsProvider.class);
    public static final PEBlockCapability<IEmcStorageBigInteger, Direction> BIG_EMC_STORAGE_CAPABILITY =
        PEBlockCapability.create(Main.rl("alchemical_book_locations"), IEmcStorageBigInteger.class, Direction.class);
}
