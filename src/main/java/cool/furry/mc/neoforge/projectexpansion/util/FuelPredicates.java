package cool.furry.mc.neoforge.projectexpansion.util;

import cool.furry.mc.neoforge.projectexpansion.item.ItemInfiniteFuel;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * Predicates for host machines whose fuel slots use their own item filters instead of the vanilla fuel map.
 * <p>
 * Kept in a separate class so mixins that must not be remapped can reference it without naming Minecraft types.
 */
public final class FuelPredicates {
    /** Matches the infinite fuel item. The item class is only loaded when the predicate is first evaluated. */
    public static final Predicate<ItemStack> INFINITE_FUEL = stack -> stack.getItem() instanceof ItemInfiniteFuel;

    private FuelPredicates() {
    }
}
