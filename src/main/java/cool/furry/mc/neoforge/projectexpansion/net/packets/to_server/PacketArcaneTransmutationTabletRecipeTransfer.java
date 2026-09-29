package cool.furry.mc.neoforge.projectexpansion.net.packets.to_server;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import cool.furry.mc.neoforge.projectexpansion.net.packets.IPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import moze_intel.projecte.network.PEPacketContext;

import java.util.List;

public record PacketArcaneTransmutationTabletRecipeTransfer(List<List<ItemStack>> recipe, boolean transferAll) implements IPacket {
    public static final CustomPacketPayload.Type<PacketArcaneTransmutationTabletRecipeTransfer> TYPE = new CustomPacketPayload.Type<>(Main.rl("arcane_transmutation_recipe_transfer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketArcaneTransmutationTabletRecipeTransfer> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()).apply(ByteBufCodecs.list()), PacketArcaneTransmutationTabletRecipeTransfer::recipe,
            ByteBufCodecs.BOOL, PacketArcaneTransmutationTabletRecipeTransfer::transferAll,
            PacketArcaneTransmutationTabletRecipeTransfer::new
    );

    @Override
    public void handle(PEPacketContext context) {
        Player player = context.player();
        if (player.containerMenu instanceof ContainerArcaneTransmutationTablet container) {
            container.onRecipeTransfer(recipe, transferAll);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
