package cool.furry.mc.neoforge.projectexpansion.net;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
public final class StreamCodecs {
    private StreamCodecs() {}
    public static <E extends Enum<E>> StreamCodec<FriendlyByteBuf, E> enumCodec(Class<E> type) {
        return StreamCodec.of((buf, value) -> buf.writeEnum(value), buf -> buf.readEnum(type));
    }
}
