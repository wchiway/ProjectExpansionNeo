package cool.furry.mc.neoforge.projectexpansion.mixin;

import moze_intel.projecte.gameObjs.items.Tome;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Makes the Tome of Knowledge have the enchantment glint, because I want it to dammit
@SuppressWarnings("unused")
@Mixin(Item.class)
public class TomeMixin {
    @Inject(method = "isFoil", at = @At("HEAD"), cancellable = true)
    private void projectexpansion$tomeGlint(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Tome) cir.setReturnValue(true);
    }
}
