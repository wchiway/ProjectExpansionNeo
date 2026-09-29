package cool.furry.mc.neoforge.projectexpansion.platform;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.block.entity.BlockEntityBase;
import cool.furry.mc.neoforge.projectexpansion.block.entity.BlockEntityEMCLink;
import cool.furry.mc.neoforge.projectexpansion.block.entity.BlockEntityTransmutationInterface;
import moze_intel.projecte.api.capabilities.PEBlockCapability;
import moze_intel.projecte.api.capabilities.PEEntityCapability;
import moze_intel.projecte.api.item_handlers.ContainerItemHandler;
import moze_intel.projecte.api.item_handlers.IItemHandler;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;

public final class InventoryCapabilities {
    public static final class ItemHandler {
        public static final PEBlockCapability<IItemHandler, Direction> BLOCK =
            PEBlockCapability.create(Main.rl("inventory"), IItemHandler.class, Direction.class);
        public static final PEEntityCapability<IItemHandler> ENTITY =
            PEEntityCapability.create(Main.rl("player_inventory"), IItemHandler.class);
    }
    public static void register() {
        ItemHandler.ENTITY.registerForType((player, context) -> new ContainerItemHandler(player.getInventory()), EntityType.PLAYER);
        ItemStorage.SIDED.registerFallback((world, pos, state, entity, side) -> {
            if (entity instanceof BlockEntityEMCLink link) return link.fabricItems();
            if (entity instanceof BlockEntityTransmutationInterface table) return table.fabricItems();
            if (entity instanceof BlockEntityBase base && ItemHandler.BLOCK.find(world, pos, state, entity, side) != null) {
                return base.fabricInventory(side);
            }
            return null;
        });
        FluidStorage.SIDED.registerForBlockEntity((entity, side) -> entity.fabricFluids(),
            cool.furry.mc.neoforge.projectexpansion.registries.BlockEntityTypes.EMC_LINK.get());
    }
}
