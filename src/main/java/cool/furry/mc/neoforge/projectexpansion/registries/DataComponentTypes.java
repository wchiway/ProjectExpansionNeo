package cool.furry.mc.neoforge.projectexpansion.registries;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.capability.CapabilityAlchemicalBookLocations;
import cool.furry.mc.neoforge.projectexpansion.util.BasicDataComponentTypes;
import cool.furry.mc.neoforge.projectexpansion.util.TagNames;
import cool.furry.mc.neoforge.projectexpansion.util.Util;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import moze_intel.projecte.gameObjs.registration.PEDeferredHolder;
import moze_intel.projecte.gameObjs.registration.impl.DataComponentTypeDeferredRegister;

import java.util.UUID;

public class DataComponentTypes {
    public static final DataComponentTypeDeferredRegister Registry = new DataComponentTypeDeferredRegister(Main.MOD_ID);

    public record OwnerData(UUID uuid, String name) {
        public static final Codec<OwnerData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(UUIDUtil.CODEC.fieldOf(TagNames.OWNER).forGetter(OwnerData::uuid), Codec.STRING.fieldOf(TagNames.OWNER_NAME).forGetter(OwnerData::name)).apply(instance, OwnerData::new)
        );
        public static final StreamCodec<ByteBuf, OwnerData> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC, OwnerData::uuid, ByteBufCodecs.STRING_UTF8, OwnerData::name, OwnerData::new);

        public boolean isNone() {
            return uuid == Util.DUMMY_UUID;
        }
    }

    // Prepaid fractional EMC travels with the fuel so moving or reloading it cannot reset billing.
    public record FuelCredit(int numerator, int denominator) {
        public static final FuelCredit EMPTY = new FuelCredit(0, 1);
        public static final Codec<FuelCredit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(0, Integer.MAX_VALUE).fieldOf("numerator").forGetter(FuelCredit::numerator),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("denominator").forGetter(FuelCredit::denominator)
        ).apply(instance, FuelCredit::new));
        public static final StreamCodec<ByteBuf, FuelCredit> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, FuelCredit::numerator, ByteBufCodecs.VAR_INT, FuelCredit::denominator, FuelCredit::new);

        public long rescale(int denominator) {
            if (numerator < 0 || this.denominator <= 0) return 0;
            return (long) Math.min(numerator, this.denominator - 1) * denominator / this.denominator;
        }
    }

    public static final PEDeferredHolder<DataComponentType<?>, DataComponentType<FuelCredit>> FUEL_CREDIT = Registry.simple("fuel_credit", builder -> builder.persistent(FuelCredit.CODEC).networkSynchronized(FuelCredit.STREAM_CODEC));
    public static final PEDeferredHolder<DataComponentType<?>, DataComponentType<OwnerData>> OWNER = Registry.simple("uuid", (builder) -> builder.persistent(OwnerData.CODEC).networkSynchronized(OwnerData.STREAM_CODEC));
    public static final PEDeferredHolder<DataComponentType<?>, DataComponentType<CapabilityAlchemicalBookLocations.AlchemicalBookLocationData>> ALCHEMICAL_BOOK_LOCATIONS = Registry.simple("alchemical_book_locations", (builder) -> builder.persistent(CapabilityAlchemicalBookLocations.AlchemicalBookLocationData.CODEC).networkSynchronized(CapabilityAlchemicalBookLocations.AlchemicalBookLocationData.STREAM_CODEC));

    public static final PEDeferredHolder<DataComponentType<?>, DataComponentType<BasicDataComponentTypes.LongValue>> LAST_USED = Registry.simple("last_used", (builder) -> builder.persistent(BasicDataComponentTypes.LongValue.CODEC).networkSynchronized(BasicDataComponentTypes.LongValue.STREAM_CODEC));
    public static final PEDeferredHolder<DataComponentType<?>, DataComponentType<BasicDataComponentTypes.LongValue>> KNOWLEDGE_GAINED = Registry.simple("knowledge_gained", (builder) -> builder.persistent(BasicDataComponentTypes.LongValue.CODEC).networkSynchronized(BasicDataComponentTypes.LongValue.STREAM_CODEC));



}
