package cool.furry.mc.neoforge.projectexpansion.registries;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.block.BlockCompactSun;
import cool.furry.mc.neoforge.projectexpansion.block.BlockCondenserMK3;
import cool.furry.mc.neoforge.projectexpansion.block.BlockTransmutationInterface;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import moze_intel.projecte.gameObjs.registration.PEDeferredHolder;
import net.minecraft.core.registries.Registries;
import moze_intel.projecte.gameObjs.registration.PEDeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class Blocks {
    public static final PEDeferredRegister<Block> Registry = PEDeferredRegister.create(Registries.BLOCK, Main.MOD_ID);

    public static final PEDeferredHolder<Block, BlockTransmutationInterface> TRANSMUTATION_INTERFACE = register("transmutation_interface", BlockTransmutationInterface::new, BlockTransmutationInterface::getProperties);
    public static final PEDeferredHolder<Block, BlockCompactSun> COMPACT_SUN = register("compact_sun", BlockCompactSun::new, BlockCompactSun::getProperties);
    public static final PEDeferredHolder<Block, BlockCondenserMK3> CONDENSER_MK3 = register("condenser_mk3", BlockCondenserMK3::new, BlockCondenserMK3::getProperties);

    public static <T extends Block> PEDeferredHolder<Block, T> register(String name, Function<BlockBehaviour.Properties, T> createBlock, Supplier<BlockBehaviour.Properties> properties) {
        return Registry.register(name, () -> createBlock.apply(properties.get()));
    }
}
