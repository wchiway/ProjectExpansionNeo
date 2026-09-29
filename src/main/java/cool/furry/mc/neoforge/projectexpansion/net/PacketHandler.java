package cool.furry.mc.neoforge.projectexpansion.net;
import cool.furry.mc.neoforge.projectexpansion.net.packets.IPacket;
import cool.furry.mc.neoforge.projectexpansion.net.packets.to_client.*;
import cool.furry.mc.neoforge.projectexpansion.net.packets.to_server.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
public final class PacketHandler {
    public PacketHandler() {
        server(PacketArcaneTransmutationTabletRecipeTransfer.TYPE, PacketArcaneTransmutationTabletRecipeTransfer.STREAM_CODEC);
        server(PacketArcaneTransmutationTabletSmallButton.TYPE, PacketArcaneTransmutationTabletSmallButton.STREAM_CODEC);
        server(PacketCreateTeleportLocation.TYPE, PacketCreateTeleportLocation.STREAM_CODEC);
        server(PacketDeleteTeleportLocation.TYPE, PacketDeleteTeleportLocation.STREAM_CODEC);
        server(PacketOpenTransmutationTablet.TYPE, PacketOpenTransmutationTablet.STREAM_CODEC);
        server(PacketTeleportBack.TYPE, PacketTeleportBack.STREAM_CODEC);
        server(PacketTeleportToLocation.TYPE, PacketTeleportToLocation.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ClearKnowledgePacket.TYPE, ClearKnowledgePacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(PacketOpenAlchemicalBookGUI.TYPE, PacketOpenAlchemicalBookGUI.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(PacketSyncAlchemicalBookLocations.TYPE, PacketSyncAlchemicalBookLocations.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(PacketUpdateCondenserLock.TYPE, PacketUpdateCondenserLock.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(PacketUpdateWindowLong.TYPE, PacketUpdateWindowLong.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(PacketUpdateWindowInt.TYPE, PacketUpdateWindowInt.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(PacketUpdateWindowBigInteger.TYPE, PacketUpdateWindowBigInteger.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(UpdateTransmutationTargetsPacket.TYPE, UpdateTransmutationTargetsPacket.STREAM_CODEC);
    }
    private static <T extends IPacket> void server(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        PayloadTypeRegistry.playC2S().register(type, codec);
        ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> payload.handle(context::player));
    }
    public static void registerClientReceivers() { ClientReceivers.register(); }
    private static final class ClientReceivers {
        static void register() {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(ClearKnowledgePacket.TYPE, (payload, context) -> payload.handle(context::player));
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(PacketOpenAlchemicalBookGUI.TYPE, (payload, context) -> payload.handle(context::player));
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(PacketSyncAlchemicalBookLocations.TYPE, (payload, context) -> payload.handle(context::player));
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(PacketUpdateCondenserLock.TYPE, (payload, context) -> payload.handle(context::player));
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(PacketUpdateWindowLong.TYPE, (payload, context) -> payload.handle(context::player));
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(PacketUpdateWindowInt.TYPE, (payload, context) -> payload.handle(context::player));
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(PacketUpdateWindowBigInteger.TYPE, (payload, context) -> payload.handle(context::player));
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(UpdateTransmutationTargetsPacket.TYPE, (payload, context) -> payload.handle(context::player));
        }
    }
}
