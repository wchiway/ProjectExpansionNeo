package cool.furry.mc.neoforge.projectexpansion.net;
import java.util.concurrent.CompletableFuture;
public final class PacketWork {
    private PacketWork() {}
    /** Fabric play payload receivers already execute on the logical game thread. */
    public static CompletableFuture<Void> run(Runnable action) {
        action.run();
        return CompletableFuture.completedFuture(null);
    }
}
