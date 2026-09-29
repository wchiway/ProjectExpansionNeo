package cool.furry.mc.neoforge.projectexpansion.util;

import cool.furry.mc.neoforge.projectexpansion.platform.CapabilityRegistrar;

// ProjectE has similar functionality, but I feel like it's too internal to rely on
@FunctionalInterface
public interface IHasCapability {
    void registerCapabilities(CapabilityRegistrar event);
}
