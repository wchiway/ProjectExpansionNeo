package cool.furry.mc.neoforge.projectexpansion.mixin;

import cool.furry.mc.neoforge.projectexpansion.Main;
import moze_intel.projecte.PECore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PECore.class, remap = false)
public abstract class ProjectEInitializationMixin {
    @Inject(method = "onInitialize", at = @At("TAIL"))
    private void projectexpansion$afterRegistries(CallbackInfo ci) {
        Main.projectEInitialized();
    }
}
