package cool.furry.mc.neoforge.projectexpansion.registries;

import cool.furry.mc.neoforge.projectexpansion.Main;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import moze_intel.projecte.gameObjs.registration.PEDeferredHolder;
import moze_intel.projecte.gameObjs.registration.PEDeferredRegister;

public class Attributes {
    public static final PEDeferredRegister<Attribute> Registry = PEDeferredRegister.create(Registries.ATTRIBUTE, Main.MOD_ID);

    public static final PEDeferredHolder<Attribute, Attribute> SUN_EXPOSURE_PROTECTION = Registry.register("sun_exposure_protection", () -> new RangedAttribute(String.format("attribute.%s.sun_exposure_protection", Main.MOD_ID), 0, 0, 1).setSyncable(true));
}
