package cool.furry.mc.neoforge.projectexpansion.registries;
import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.capability.CapabilityAlchemicalBookLocations.AlchemicalBookLocationData;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
public final class AttachmentTypes {
    public static final AttachmentType<AlchemicalBookLocationData> ALCHEMICAL_BOOK_LOCATIONS =
        AttachmentRegistry.create(Main.rl("alchemical_book_locations"), builder -> builder
            .initializer(AlchemicalBookLocationData::new).persistent(AlchemicalBookLocationData.CODEC).copyOnDeath());
    public static void init() {}
}
