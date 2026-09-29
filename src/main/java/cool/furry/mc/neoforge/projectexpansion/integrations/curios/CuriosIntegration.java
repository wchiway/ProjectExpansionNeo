package cool.furry.mc.neoforge.projectexpansion.integrations.curios;

import java.util.ArrayList;
import java.util.Optional;
import moze_intel.projecte.api.item_handlers.IItemHandlerModifiable;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;

/** Access to the dedicated tablet slot on Fabric. */
public final class CuriosIntegration {
    public static final String MOD_ID = "trinkets";
    public static final String TABLET_SLOT_ID = "transmutation_tablet";
    public static boolean modLoaded() { return FabricLoader.getInstance().isModLoaded(MOD_ID); }
    public static Optional<IItemHandlerModifiable> getCuriosInventory(Player player) {
        return modLoaded() ? Integrator.inventory(player) : Optional.empty();
    }
    private static final class Integrator {
        static Optional<IItemHandlerModifiable> inventory(Player player) {
            return dev.emi.trinkets.api.TrinketsApi.getTrinketComponent(player).flatMap(component -> {
                var inventories = new ArrayList<dev.emi.trinkets.api.TrinketInventory>();
                for (var group : component.getInventory().values()) {
                    var inventory = group.get(TABLET_SLOT_ID);
                    if (inventory != null) inventories.add(inventory);
                }
                return inventories.isEmpty() ? Optional.empty()
                    : Optional.of(new moze_intel.projecte.integration.trinkets.TrinketItemHandler(inventories));
            });
        }
    }
}
