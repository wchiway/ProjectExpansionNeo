package cool.furry.mc.neoforge.projectexpansion.platform;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;
public final class ServerContext {
    private static MinecraftServer server;
    private ServerContext() {}
    public static void set(@Nullable MinecraftServer value) { server = value; }
    public static @Nullable MinecraftServer getCurrentServer() { return server; }
}
