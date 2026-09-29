package cool.furry.mc.neoforge.projectexpansion.test;

import cool.furry.mc.neoforge.projectexpansion.block.entity.BlockEntityEMCLink;
import cool.furry.mc.neoforge.projectexpansion.config.Config;
import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import cool.furry.mc.neoforge.projectexpansion.registries.DataComponentTypes;
import cool.furry.mc.neoforge.projectexpansion.registries.MenuTypes;
import cool.furry.mc.neoforge.projectexpansion.util.Matter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.material.Fluids;

public class ExpansionGameTests implements FabricGameTest {
    @GameTest(template = EMPTY_STRUCTURE)
    public void arcaneTabletOpensInBothHands(GameTestHelper test) {
        var player = test.makeMockServerPlayerInLevel();
        try {
            var tablet = cool.furry.mc.neoforge.projectexpansion.registries.Items.ARCANE_TRANSMUTATION_TABLET.get();
            player.getInventory().selected = 4;
            for (InteractionHand hand : InteractionHand.values()) {
                player.setItemInHand(hand, new ItemStack(tablet));
                var result = tablet.use(test.getLevel(), player, hand);
                test.assertTrue(result.getResult().consumesAction(), "Using the tablet succeeds in " + hand);
                test.assertTrue(player.containerMenu instanceof ContainerArcaneTransmutationTablet, "Using the tablet opens its menu in " + hand);
                var menu = (ContainerArcaneTransmutationTablet) player.containerMenu;
                test.assertTrue(menu.getType() == MenuTypes.ARCANE_TRANSMUTATION_TABLET.get(), "Opened menu uses its registered type");
                test.assertTrue(menu.hand == hand && menu.stillValid(player), "Opened menu remains valid in " + hand);
                test.assertTrue(menu.slots.size() == 73, "Opened menu includes transmutation, player and crafting slots");
                player.closeContainer();
                player.setItemInHand(hand, ItemStack.EMPTY);
            }
            test.succeed();
        } finally {
            player.closeContainer();
            test.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void arcaneTabletOpensWithoutHand(GameTestHelper test) {
        var player = test.makeMockServerPlayerInLevel();
        try {
            cool.furry.mc.neoforge.projectexpansion.registries.Items.ARCANE_TRANSMUTATION_TABLET.get().openContainer(player);
            test.assertTrue(player.containerMenu instanceof ContainerArcaneTransmutationTablet, "The accessory opening path creates the arcane menu");
            var menu = (ContainerArcaneTransmutationTablet) player.containerMenu;
            test.assertTrue(menu.hand == null && menu.stillValid(player), "Accessory menu does not require a held tablet");
            test.succeed();
        } finally {
            player.closeContainer();
            test.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void collectorInventoryTransactions(GameTestHelper test) {
        var player = test.makeMockServerPlayerInLevel();
        try {
            BlockPos relative = new BlockPos(2, 2, 2);
            test.setBlock(relative, Matter.BASIC.getCollector());
            var world = test.getLevel();
            var storage = ItemStorage.SIDED.find(world, test.absolutePos(relative), Direction.NORTH);
            test.assertTrue(storage != null, "Collector must expose Fabric item storage");
            var coal = ItemVariant.of(Items.COAL);
            try (var outer = Transaction.openOuter()) {
                test.assertTrue(storage.insert(coal, 8, outer) == 8, "Collector accepts fuel from its input side");
                var allSides = ItemStorage.SIDED.find(world, test.absolutePos(relative), null);
                try (var inner = outer.openNested()) {
                    test.assertTrue(allSides.insert(coal, 4, inner) == 4, "Second view accepts a nested insertion");
                }
                test.assertTrue(storage.iterator().next().getAmount() == 8, "Nested rollback preserves outer insertion");
            }
            test.assertTrue(storage.iterator().next().getAmount() == 0, "Outer rollback restores every side");
            try (var transaction = Transaction.openOuter()) {
                test.assertTrue(storage.insert(coal, 8, transaction) == 8, "Committed insertion succeeds");
                transaction.commit();
            }
            test.assertTrue(storage.iterator().next().getAmount() == 8, "Committed inventory persists");
            try (var transaction = Transaction.openOuter()) {
                test.assertTrue(storage.extract(coal, 8, transaction) == 0, "Input side cannot extract");
            }
            test.succeed();
        } finally {
            test.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void emcLinkTransactions(GameTestHelper test) {
        var player = test.makeMockServerPlayerInLevel();
        try {
            var provider = PECapabilities.KNOWLEDGE_CAPABILITY.find(player);
            BigInteger initial = new BigInteger("1000000000000000000000000000");
            provider.setEmc(initial);
            BlockPos relative = new BlockPos(2, 2, 2);
            test.setBlock(relative, Matter.RED.getEMCLink());
            var world = test.getLevel();
            var pos = test.absolutePos(relative);
            var link = (BlockEntityEMCLink) world.getBlockEntity(pos);
            link.owner = player.getUUID();
            link.itemStack = new ItemStack(Items.DIAMOND);
            link.remainingExport = link.remainingImport = 64;
            long value = IEMCProxy.INSTANCE.getValue(Items.DIAMOND);
            test.assertTrue(value > 0, "ProjectEF's EMC map is available");
            var storage = ItemStorage.SIDED.find(world, pos, Direction.NORTH);
            var diamond = ItemVariant.of(Items.DIAMOND);
            try (var outer = Transaction.openOuter()) {
                test.assertTrue(storage.extract(diamond, 5, outer) == 5, "Virtual export succeeds");
                try (var inner = outer.openNested()) {
                    test.assertTrue(storage.insert(diamond, 2, inner) == 2, "Virtual import succeeds");
                    inner.commit();
                }
            }
            test.assertTrue(provider.getEmc().equals(initial), "Aborted transfer cannot create or consume EMC");
            test.assertTrue(link.remainingExport == 64 && link.remainingImport == 64, "Aborted transfer restores quotas");
            try (var transaction = Transaction.openOuter()) {
                test.assertTrue(storage.extract(diamond, 3, transaction) == 3, "Committed export succeeds");
                transaction.commit();
            }
            test.assertTrue(provider.getEmc().equals(initial.subtract(BigInteger.valueOf(value).multiply(BigInteger.valueOf(3)))),
                "Committed transfer charges exact big-integer EMC once");
            test.assertTrue(link.remainingExport == 61, "Only committed items consume the export quota");
            test.succeed();
        } finally {
            test.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void fluidTransactionsAndUnits(GameTestHelper test) {
        var player = test.makeMockServerPlayerInLevel();
        try {
            var provider = PECapabilities.KNOWLEDGE_CAPABILITY.find(player);
            BigInteger initial = BigInteger.valueOf(1_000_000);
            provider.setEmc(initial);
            BlockPos relative = new BlockPos(2, 2, 2);
            test.setBlock(relative, Matter.RED.getEMCLink());
            var world = test.getLevel();
            var pos = test.absolutePos(relative);
            var link = (BlockEntityEMCLink) world.getBlockEntity(pos);
            link.owner = player.getUUID();
            link.itemStack = new ItemStack(Items.LAVA_BUCKET);
            link.remainingFluid = 4 * FluidConstants.BUCKET;
            var storage = FluidStorage.SIDED.find(world, pos, Direction.UP);
            var lava = FluidVariant.of(Fluids.LAVA);
            try (var transaction = Transaction.openOuter()) {
                test.assertTrue(storage.iterator().next().getAmount() > 0, "Fluid views can be read inside a transaction");
                test.assertTrue(storage.extract(lava, FluidConstants.BUCKET / 2, transaction) == FluidConstants.BUCKET / 2,
                    "Half a Fabric bucket uses 40,500 droplets");
            }
            test.assertTrue(provider.getEmc().equals(initial), "Fluid simulation leaves EMC unchanged");
            test.assertTrue(link.remainingFluid == 4 * FluidConstants.BUCKET, "Fluid rollback restores quota");
            BigInteger cost = BigDecimal.valueOf(IEMCProxy.INSTANCE.getValue(Items.LAVA_BUCKET))
                .subtract(BigDecimal.valueOf(IEMCProxy.INSTANCE.getValue(Items.BUCKET))
                    .multiply(BigDecimal.valueOf(Matter.RED.getFluidEfficiencyPercentage())).divide(BigDecimal.valueOf(100)))
                .setScale(0, RoundingMode.CEILING).toBigIntegerExact();
            try (var transaction = Transaction.openOuter()) {
                test.assertTrue(storage.extract(lava, FluidConstants.BUCKET, transaction) == FluidConstants.BUCKET, "A bucket can be exported");
                transaction.commit();
            }
            test.assertTrue(provider.getEmc().equals(initial.subtract(cost)), "Fluid export charges once, using bucket units");
            test.assertTrue(link.remainingFluid == 3 * FluidConstants.BUCKET, "Committed fluid consumes one bucket of quota");
            test.succeed();
        } finally {
            test.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void infiniteFuelConsumesEmcOnce(GameTestHelper test) {
        var player = test.makeMockServerPlayerInLevel();
        try {
            var provider = PECapabilities.KNOWLEDGE_CAPABILITY.find(player);
            BigInteger initial = BigInteger.valueOf(10_000);
            provider.setEmc(initial);
            BlockPos relative = new BlockPos(2, 2, 2);
            test.setBlock(relative, Blocks.FURNACE);
            var world = test.getLevel();
            var pos = test.absolutePos(relative);
            var furnace = (AbstractFurnaceBlockEntity) world.getBlockEntity(pos);
            var fuel = new ItemStack(cool.furry.mc.neoforge.projectexpansion.registries.Items.INFINITE_FUEL.get());
            fuel.set(DataComponentTypes.OWNER.get(), new DataComponentTypes.OwnerData(player.getUUID(), player.getName().getString()));
            furnace.setItem(0, new ItemStack(Items.IRON_ORE));
            furnace.setItem(1, fuel);
            AbstractFurnaceBlockEntity.serverTick(world, pos, world.getBlockState(pos), furnace);
            AbstractFurnaceBlockEntity.serverTick(world, pos, world.getBlockState(pos), furnace);
            test.assertTrue(furnace.getItem(1).getCount() == 1, "Infinite fuel is not consumed");
            test.assertTrue(provider.getEmc().equals(initial.subtract(BigInteger.valueOf(Config.server.infiniteFuelCost.get()))),
                "Starting one burn charges EMC exactly once");
            test.succeed();
        } finally {
            test.getLevel().getServer().getPlayerList().remove(player);
        }
    }
}
