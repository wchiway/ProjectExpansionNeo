package cool.furry.mc.neoforge.projectexpansion.platform;
import java.util.function.BiFunction;
import moze_intel.projecte.api.capabilities.PEBlockCapability;
import moze_intel.projecte.api.capabilities.PEEntityCapability;
import moze_intel.projecte.api.capabilities.PEItemCapability;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
/** Registers extension providers in the same Fabric lookups used by ProjectEF. */
public final class CapabilityRegistrar {
    public <A, E extends Entity> void registerEntity(PEEntityCapability<A> capability, EntityType<E> type, BiFunction<E, Void, A> provider) {
        capability.registerForType(provider, type);
    }
    public <A> void registerItem(PEItemCapability<A> capability, ItemApiLookup.ItemApiProvider<A, Void> provider, ItemLike... items) {
        capability.registerForItems(provider, items);
    }
    public <A, C, E extends BlockEntity> void registerBlockEntity(PEBlockCapability<A, C> capability, BlockEntityType<E> type, BiFunction<? super E, C, A> provider) {
        capability.registerForBlockEntities((entity, context) -> provider.apply(type.getBlockEntity(entity.getLevel(), entity.getBlockPos()), context), type);
    }
}
