package cool.furry.mc.neoforge.projectexpansion.platform;

import java.util.function.Consumer;
import moze_intel.projecte.gameObjs.registration.impl.ContainerTypeDeferredRegister;
import moze_intel.projecte.gameObjs.registration.impl.ContainerTypeDeferredRegister.RawScreenData;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class Menus {
    private Menus() {}

    public static void open(Player player, MenuProvider provider, BlockPos pos) {
        open(player, provider, buf -> buf.writeBlockPos(pos));
    }

    public static void open(Player player, MenuProvider provider, Consumer<RegistryFriendlyByteBuf> writer) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        serverPlayer.openMenu(new ExtendedScreenHandlerFactory<RawScreenData>() {
            @Override
            public RawScreenData getScreenOpeningData(ServerPlayer target) {
                return ContainerTypeDeferredRegister.createScreenData(target.registryAccess(), writer);
            }
            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player target) {
                return provider.createMenu(id, inventory, target);
            }
            @Override
            public Component getDisplayName() { return provider.getDisplayName(); }
        });
    }
}
