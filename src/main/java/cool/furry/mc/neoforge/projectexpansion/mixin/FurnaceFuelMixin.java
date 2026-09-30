package cool.furry.mc.neoforge.projectexpansion.mixin;

import cool.furry.mc.neoforge.projectexpansion.item.ItemInfiniteFuel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceFuelMixin {
    @Inject(method = "getBurnDuration", at = @At("HEAD"), cancellable = true)
    private void projectexpansion$fuelDuration(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (stack.getItem() instanceof ItemInfiniteFuel fuel) cir.setReturnValue(fuel.getBurnTime(stack));
    }

    /**
     * Vanilla only accepts items present in its static fuel map, so the fuel slot and automation rejected the item
     * before it could ever burn. The map is global state shared with other machines, so the item is accepted here
     * instead of being registered as a generic fuel.
     */
    @Inject(method = "isFuel", at = @At("HEAD"), cancellable = true)
    private static void projectexpansion$acceptInfiniteFuel(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.getItem() instanceof ItemInfiniteFuel) cir.setReturnValue(true);
    }

    /**
     * The infinite fuel is charged instead of consumed. This is the only fuel consumption site in the furnace tick,
     * and it is only reached once the fuel duration above granted a positive burn time.
     */
    @Redirect(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private static void projectexpansion$keepInfiniteFuel(ItemStack stack, int amount) {
        if (stack.getItem() instanceof ItemInfiniteFuel fuel) {
            fuel.consumeCharge(stack);
        } else {
            stack.shrink(amount);
        }
    }
}
