package cool.furry.mc.neoforge.projectexpansion.mixin;

import cool.furry.mc.neoforge.projectexpansion.util.FuelPredicates;
import moze_intel.projecte.gameObjs.container.slots.SlotPredicates;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

/**
 * ProjectE's matter furnaces only accept their own fuels or items that hold EMC in their fuel slot, through this
 * shared predicate. Widening it once, while the class is initialized, covers both the furnace menu slot and the
 * automation wrapper without touching ProjectE's item registration.
 * <p>
 * The mixin is not remapped: the target is another mod's class and no Minecraft type is named here, which keeps the
 * shadowed field name intact.
 */
@Mixin(value = SlotPredicates.class, remap = false)
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class SlotPredicatesMixin {
    @Shadow
    @Final
    @Mutable
    public static Predicate FURNACE_FUEL;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void projectexpansion$acceptInfiniteFuel(CallbackInfo ci) {
        FURNACE_FUEL = FURNACE_FUEL.or(FuelPredicates.INFINITE_FUEL);
    }
}
