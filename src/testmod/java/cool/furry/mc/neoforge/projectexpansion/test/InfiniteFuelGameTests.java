package cool.furry.mc.neoforge.projectexpansion.test;

import cool.furry.mc.neoforge.projectexpansion.config.Config;
import cool.furry.mc.neoforge.projectexpansion.item.ItemInfiniteFuel;
import cool.furry.mc.neoforge.projectexpansion.registries.DataComponentTypes;
import java.math.BigInteger;
import java.util.UUID;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.item_handlers.IItemHandler;
import moze_intel.projecte.gameObjs.block_entities.DMFurnaceBlockEntity;
import moze_intel.projecte.gameObjs.container.slots.SlotPredicates;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

public class InfiniteFuelGameTests implements FabricGameTest {
    private static final BigInteger INITIAL_EMC = new BigInteger("1000000000000000000000000000");
    private static final BlockPos POSITION = new BlockPos(2, 2, 2);
    private static final Block[] VANILLA_FURNACES = {Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER};
    private static final String[] MATTER_FURNACES = {"dm_furnace", "rm_furnace"};

    @GameTest(template = EMPTY_STRUCTURE)
    public void vanillaFurnacesBillContinuously(GameTestHelper test) {
        var player = test.makeMockServerPlayerInLevel();
        try {
            var provider = PECapabilities.KNOWLEDGE_CAPABILITY.find(player);
            for (Block block : VANILLA_FURNACES) {
                provider.setEmc(INITIAL_EMC);
                var furnace = vanillaFurnace(test, block);
                var fuel = newFuel(player);
                test.assertTrue(AbstractFurnaceBlockEntity.isFuel(fuel), block + " recognises infinite fuel");
                test.assertTrue(furnace.canPlaceItem(1, fuel) && furnace.canPlaceItemThroughFace(1, fuel, Direction.NORTH),
                    block + " accepts fuel insertion");
                furnace.setItem(1, fuel);
                int period = vanillaPeriod(block);
                int elapsed = 0;
                int smelted = 0;
                for (int checkpoint : new int[]{20, 40, 200, 400, Math.max(401, period)}) {
                    smelted += smeltVanilla(test, furnace, block, checkpoint - elapsed);
                    elapsed = checkpoint;
                    assertDebit(test, provider, INITIAL_EMC, elapsed, period, block.toString());
                    assertFuel(test, furnace.getItem(1), player);
                    if (checkpoint == 40) {
                        var credit = credit(furnace.getItem(1));
                        furnace.loadWithComponents(furnace.saveWithoutMetadata(test.getLevel().registryAccess()),
                            test.getLevel().registryAccess());
                        test.assertTrue(credit.equals(credit(furnace.getItem(1))), "Block reload retains fractional credit");
                        assertDebit(test, provider, INITIAL_EMC, elapsed, period, "Loading does not charge");
                    }
                }
                test.assertTrue(smelted >= 2, block + " actually completes repeated smelts");
            }
            test.succeed();
        } finally {
            test.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void vanillaFurnacesStopBillingWhenInactive(GameTestHelper test) {
        var player = test.makeMockServerPlayerInLevel();
        try {
            var provider = PECapabilities.KNOWLEDGE_CAPABILITY.find(player);
            for (Block block : VANILLA_FURNACES) {
                provider.setEmc(INITIAL_EMC);
                var furnace = vanillaFurnace(test, block);
                furnace.setItem(1, newFuel(player));
                tickVanilla(test, furnace, 40);
                test.assertTrue(provider.getEmc().equals(INITIAL_EMC), "Idle furnace does not bill");
                furnace.setItem(0, new ItemStack(input(block), 64));
                furnace.setItem(2, new ItemStack(result(block), 64));
                tickVanilla(test, furnace, 40);
                test.assertTrue(provider.getEmc().equals(INITIAL_EMC) && furnace.getItem(0).getCount() == 64,
                    "Full output prevents both billing and cooking");
                furnace.setItem(2, ItemStack.EMPTY);
                tickVanilla(test, furnace, 20);
                assertDebit(test, provider, INITIAL_EMC, 20, vanillaPeriod(block), "Unblocked furnace resumes");

                var removed = furnace.removeItem(1, 1);
                BigInteger balance = provider.getEmc();
                int inputCount = furnace.getItem(0).getCount();
                tickVanilla(test, furnace, 240);
                test.assertTrue(provider.getEmc().equals(balance) && furnace.getItem(0).getCount() == inputCount
                    && furnace.getItem(2).isEmpty(), "Removing fuel stops cooking and billing");
                assertFuel(test, removed, player);
                furnace.setItem(1, removed);
                provider.setEmc(BigInteger.ZERO);
                // A zero balance can still spend the fraction already paid by this item.
                int prepaidTicks = (int) (credit(removed).rescale(vanillaPeriod(block)) / cost());
                smeltVanilla(test, furnace, block, prepaidTicks);
                inputCount = furnace.getItem(0).getCount();
                var remaining = credit(removed);
                tickVanilla(test, furnace, 240);
                test.assertTrue(provider.getEmc().signum() == 0 && furnace.getItem(0).getCount() == inputCount
                    && furnace.getItem(2).isEmpty(), "An unaffordable tick cannot keep cooking");
                test.assertTrue(remaining.equals(credit(removed)), "Failed payment does not consume credit");
                assertFuel(test, furnace.getItem(1), player);
            }
            test.succeed();
        } finally {
            test.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void matterFurnacesBillContinuously(GameTestHelper test) {
        var player = test.makeMockServerPlayerInLevel();
        try {
            var provider = PECapabilities.KNOWLEDGE_CAPABILITY.find(player);
            for (String id : MATTER_FURNACES) {
                provider.setEmc(INITIAL_EMC);
                var furnace = matterFurnace(test, id);
                var fuel = newFuel(player);
                test.assertTrue(SlotPredicates.FURNACE_FUEL.test(fuel), "Matter fuel predicate accepts infinite fuel");
                test.assertTrue(furnace.getSideHandler(Direction.NORTH).insertItem(0, fuel, false).isEmpty(),
                    "Filtered automation handler accepts infinite fuel");
                int elapsed = 0;
                int smelted = 0;
                for (int checkpoint : new int[]{20, 40, 400}) {
                    smelted += smeltMatter(test, furnace, checkpoint - elapsed);
                    elapsed = checkpoint;
                    assertDebit(test, provider, INITIAL_EMC, elapsed, Config.server.infiniteFuelBurnTime.get(), id);
                    assertFuel(test, furnace.getFuel().getStackInSlot(0), player);
                    if (checkpoint == 40) {
                        var credit = credit(furnace.getFuel().getStackInSlot(0));
                        furnace.loadWithComponents(furnace.saveWithoutMetadata(test.getLevel().registryAccess()),
                            test.getLevel().registryAccess());
                        test.assertTrue(credit.equals(credit(furnace.getFuel().getStackInSlot(0))), "Block reload retains credit");
                        assertDebit(test, provider, INITIAL_EMC, elapsed, Config.server.infiniteFuelBurnTime.get(), "Loading is free");
                    }
                }
                test.assertTrue(smelted >= 2, id + " produces repeated iron ingots without resetting progress");
            }
            test.succeed();
        } finally {
            test.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void matterFurnacesStopBillingWhenInactive(GameTestHelper test) {
        var player = test.makeMockServerPlayerInLevel();
        try {
            var provider = PECapabilities.KNOWLEDGE_CAPABILITY.find(player);
            for (String id : MATTER_FURNACES) {
                provider.setEmc(INITIAL_EMC);
                var furnace = matterFurnace(test, id);
                furnace.getSideHandler(Direction.NORTH).insertItem(0, newFuel(player), false);
                tickMatter(test, furnace, 40);
                test.assertTrue(provider.getEmc().equals(INITIAL_EMC), "Idle matter furnace does not bill");
                furnace.getInput().insertItem(0, new ItemStack(Items.IRON_ORE, 64), false);
                for (int slot = 0; slot < furnace.getOutput().getSlots(); slot++) {
                    furnace.getOutput().insertItem(slot, new ItemStack(Items.IRON_INGOT, 64), false);
                }
                tickMatter(test, furnace, 40);
                test.assertTrue(provider.getEmc().equals(INITIAL_EMC) && furnace.getInput().getStackInSlot(0).getCount() == 64,
                    "Blocked matter furnace does not bill or smelt");
                drainIron(test, furnace.getOutput());
                smeltMatter(test, furnace, 20);
                assertDebit(test, provider, INITIAL_EMC, 20, Config.server.infiniteFuelBurnTime.get(), id);

                var removed = furnace.getFuel().extractItem(0, 1, false);
                BigInteger balance = provider.getEmc();
                int inputCount = furnace.getInput().getStackInSlot(0).getCount();
                tickMatter(test, furnace, 240);
                test.assertTrue(provider.getEmc().equals(balance) && furnace.getInput().getStackInSlot(0).getCount() == inputCount
                    && drainIron(test, furnace.getOutput()) == 0, "Removing matter furnace fuel stops cooking");
                assertFuel(test, removed, player);
                furnace.getSideHandler(Direction.NORTH).insertItem(0, removed, false);
                provider.setEmc(BigInteger.ZERO);
                int prepaidTicks = (int) (credit(removed).rescale(Config.server.infiniteFuelBurnTime.get()) / cost());
                smeltMatter(test, furnace, prepaidTicks);
                inputCount = furnace.getInput().getStackInSlot(0).getCount();
                int progress = furnace.cookingProgress;
                var remaining = credit(furnace.getFuel().getStackInSlot(0));
                tickMatter(test, furnace, 240);
                test.assertTrue(provider.getEmc().signum() == 0 && furnace.cookingProgress == progress
                    && furnace.getInput().getStackInSlot(0).getCount() == inputCount && drainIron(test, furnace.getOutput()) == 0,
                    "Unaffordable matter furnace ticks cannot advance cooking");
                test.assertTrue(remaining.equals(credit(furnace.getFuel().getStackInSlot(0))), "Refused debit leaves credit unchanged");
                assertFuel(test, furnace.getFuel().getStackInSlot(0), player);
            }
            test.succeed();
        } finally {
            test.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void normalCoalKeepsVanillaBurnDuration(GameTestHelper test) {
        var player = test.makeMockServerPlayerInLevel();
        try {
            var provider = PECapabilities.KNOWLEDGE_CAPABILITY.find(player);
            provider.setEmc(INITIAL_EMC);
            var furnace = vanillaFurnace(test, Blocks.FURNACE);
            furnace.setItem(0, new ItemStack(Items.IRON_ORE, 64));
            furnace.setItem(1, new ItemStack(Items.COAL, 2));
            tickVanilla(test, furnace, 1);
            test.assertTrue(furnace.getItem(1).getCount() == 1, "Ordinary coal is still consumed");
            test.assertTrue(furnace.saveWithoutMetadata(test.getLevel().registryAccess()).getShort("BurnTime") == 1600,
                "Ordinary coal retains its vanilla 1600-tick burn");
            test.assertTrue(smeltVanilla(test, furnace, Blocks.FURNACE, 1599) == 8 && furnace.getItem(1).getCount() == 1,
                "One coal completes eight iron smelts");
            tickVanilla(test, furnace, 1);
            test.assertTrue(furnace.getItem(1).isEmpty(), "The next coal starts only when the previous burn expires");
            test.assertTrue(provider.getEmc().equals(INITIAL_EMC), "Ordinary fuel never bills the player");
            test.succeed();
        } finally {
            test.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void savedPrepaidBurnsAreNotChargedAgain(GameTestHelper test) {
        var player = test.makeMockServerPlayerInLevel();
        try {
            var provider = PECapabilities.KNOWLEDGE_CAPABILITY.find(player);
            provider.setEmc(BigInteger.ZERO);
            var furnace = vanillaFurnace(test, Blocks.FURNACE);
            furnace.setItem(0, new ItemStack(Items.IRON_ORE, 64));
            furnace.setItem(1, newFuel(player));
            CompoundTag saved = furnace.saveWithoutMetadata(test.getLevel().registryAccess());
            saved.putShort("BurnTime", (short) 201);
            furnace.loadWithComponents(saved, test.getLevel().registryAccess());
            test.assertTrue(smeltVanilla(test, furnace, Blocks.FURNACE, 200) == 1, "An old prepaid vanilla burn still smelts");
            test.assertTrue(smeltVanilla(test, furnace, Blocks.FURNACE, 240) == 0, "An old burn cannot renew without payment");
            assertFuel(test, furnace.getItem(1), player);
            for (String id : MATTER_FURNACES) {
                var matter = matterFurnace(test, id);
                matter.getInput().insertItem(0, new ItemStack(Items.IRON_ORE, 64), false);
                matter.getSideHandler(Direction.NORTH).insertItem(0, newFuel(player), false);
                saved = matter.saveWithoutMetadata(test.getLevel().registryAccess());
                saved.putInt("burn_time", 201);
                matter.loadWithComponents(saved, test.getLevel().registryAccess());
                test.assertTrue(smeltMatter(test, matter, 200) >= 2, id + " honours its saved prepaid burn");
                test.assertTrue(smeltMatter(test, matter, 240) == 0, id + " cannot renew an expired burn");
                assertFuel(test, matter.getFuel().getStackInSlot(0), player);
            }
            test.assertTrue(provider.getEmc().signum() == 0, "Loading and spending old burns neither bills nor overdraws EMC");
            test.succeed();
        } finally {
            test.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void fractionalCreditSurvivesItemSaveLoadAndCopy(GameTestHelper test) {
        var player = test.makeMockServerPlayerInLevel();
        try {
            var provider = PECapabilities.KNOWLEDGE_CAPABILITY.find(player);
            int fractionalPeriod = cost() == Integer.MAX_VALUE ? cost() - 1 : cost() + 1;
            for (int period : new int[]{fractionalPeriod, Integer.MAX_VALUE, 1}) {
                provider.setEmc(INITIAL_EMC);
                var fuel = newFuel(player);
                var item = (ItemInfiniteFuel) fuel.getItem();
                for (int ticks = 1; ticks <= 257; ticks++) {
                    test.assertTrue(item.consumeTick(fuel, period), "Funded fuel pays tick " + ticks);
                    assertDebit(test, provider, INITIAL_EMC, ticks, period, "Direct billing");
                    BigInteger numerator = debit(ticks, period).multiply(BigInteger.valueOf(period))
                        .subtract(BigInteger.valueOf(cost()).multiply(BigInteger.valueOf(ticks)));
                    test.assertTrue(credit(fuel).equals(new DataComponentTypes.FuelCredit(numerator.intValueExact(), period)),
                        "Prepaid fractions are exact, including large denominators");
                    if (ticks == 73 || ticks == 131) {
                        var previous = fuel;
                        fuel = ticks == 73
                            ? ItemStack.parse(test.getLevel().registryAccess(), fuel.save(test.getLevel().registryAccess())).orElseThrow()
                            : fuel.copy();
                        test.assertTrue(credit(previous).equals(credit(fuel)), "Item serialization and copying preserve credit");
                        assertFuel(test, fuel, player);
                    }
                }
            }
            test.succeed();
        } finally {
            test.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void creditRescalesWithoutIntegerOverflow(GameTestHelper test) {
        var player = test.makeMockServerPlayerInLevel();
        try {
            var provider = PECapabilities.KNOWLEDGE_CAPABILITY.find(player);
            provider.setEmc(INITIAL_EMC);
            var fuel = newFuel(player);
            var prepaid = new DataComponentTypes.FuelCredit(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
            int period = Integer.MAX_VALUE - 1;
            fuel.set(DataComponentTypes.FUEL_CREDIT.get(), prepaid);
            BigInteger scaled = BigInteger.valueOf(prepaid.numerator()).multiply(BigInteger.valueOf(period))
                .divide(BigInteger.valueOf(prepaid.denominator()));
            test.assertTrue(prepaid.rescale(period) == scaled.longValueExact(), "Rescaling uses a wide intermediate product");
            BigInteger charge = BigInteger.valueOf(cost()).subtract(scaled).max(BigInteger.ZERO)
                .add(BigInteger.valueOf(period - 1L)).divide(BigInteger.valueOf(period));
            test.assertTrue(((ItemInfiniteFuel) fuel.getItem()).consumeTick(fuel, period), "Moved fuel spends rescaled credit");
            test.assertTrue(provider.getEmc().equals(INITIAL_EMC.subtract(charge)), "Changing period does not reset prepaid EMC");
            int remainder = scaled.add(charge.multiply(BigInteger.valueOf(period))).subtract(BigInteger.valueOf(cost())).intValueExact();
            test.assertTrue(credit(fuel).equals(new DataComponentTypes.FuelCredit(remainder, period)), "Rescaled fraction is exact");
            test.succeed();
        } finally {
            test.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void unfundedUnboundAndOfflineFuelCannotPay(GameTestHelper test) {
        var player = test.makeMockServerPlayerInLevel();
        try {
            var provider = PECapabilities.KNOWLEDGE_CAPABILITY.find(player);
            var fuel = newFuel(player);
            var item = (ItemInfiniteFuel) fuel.getItem();
            fuel.set(DataComponentTypes.FUEL_CREDIT.get(), new DataComponentTypes.FuelCredit(1, 2));
            BigInteger insufficient = BigInteger.valueOf(cost() - 1L);
            provider.setEmc(insufficient);
            var before = fuel.copy();
            test.assertTrue(!item.consumeTick(fuel, 1), "Insufficient EMC refuses the next active tick");
            test.assertTrue(provider.getEmc().equals(insufficient) && ItemStack.matches(before, fuel), "Refused payment is atomic");
            for (int invalidPeriod : new int[]{0, -1}) {
                test.assertTrue(!item.consumeTick(fuel, invalidPeriod) && ItemStack.matches(before, fuel), "Invalid period is inert");
            }
            provider.setEmc(INITIAL_EMC);
            var unbound = fuel.copy();
            unbound.remove(DataComponentTypes.OWNER.get());
            var offline = fuel.copy();
            UUID absent = UUID.randomUUID();
            offline.set(DataComponentTypes.OWNER.get(), new DataComponentTypes.OwnerData(absent, "offline"));
            for (ItemStack unavailable : new ItemStack[]{unbound, offline}) {
                before = unavailable.copy();
                test.assertTrue(!item.consumeTick(unavailable, Integer.MAX_VALUE) && ItemStack.matches(before, unavailable),
                    "Unbound and offline fuel cannot spend existing credit");
            }
            test.assertTrue(provider.getEmc().equals(INITIAL_EMC), "Unavailable owners do not debit another account");
            test.succeed();
        } finally {
            test.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void multipleFuelsCannotOverdrawSharedAccount(GameTestHelper test) {
        var player = test.makeMockServerPlayerInLevel();
        try {
            var provider = PECapabilities.KNOWLEDGE_CAPABILITY.find(player);
            provider.setEmc(BigInteger.valueOf(3));
            ItemStack[] fuels = {newFuel(player), newFuel(player)};
            int[] paidTicks = {0, 0};
            int period = cost() <= (Integer.MAX_VALUE - 1) / 4 ? cost() * 4 + 1 : Integer.MAX_VALUE;
            boolean refused = false;
            for (int attempt = 0; attempt < 64; attempt++) {
                int index = attempt % fuels.length;
                var fuel = fuels[index];
                var before = fuel.copy();
                BigInteger balance = provider.getEmc();
                BigInteger nextCharge = debit(paidTicks[index] + 1, period).subtract(debit(paidTicks[index], period));
                boolean affordable = balance.compareTo(nextCharge) >= 0;
                test.assertTrue(((ItemInfiniteFuel) fuel.getItem()).consumeTick(fuel, period) == affordable,
                    "Each item checks the current balance, including prepaid zero-charge ticks");
                if (affordable) {
                    paidTicks[index]++;
                } else {
                    refused = true;
                    test.assertTrue(ItemStack.matches(before, fuel) && provider.getEmc().equals(balance), "Refused debit is atomic");
                }
                BigInteger expected = BigInteger.valueOf(3).subtract(debit(paidTicks[0], period)).subtract(debit(paidTicks[1], period));
                test.assertTrue(provider.getEmc().equals(expected) && provider.getEmc().signum() >= 0, "Shared account never overdraws");
            }
            test.assertTrue(refused && paidTicks[0] > 0 && paidTicks[1] > 0 && provider.getEmc().signum() == 0,
                "Both items work, exhaust the balance, and refuse unpaid work");
            test.succeed();
        } finally {
            test.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    private static int cost() {
        return Config.server.infiniteFuelCost.get();
    }

    private static BigInteger debit(int ticks, int period) {
        return BigInteger.valueOf(ticks).multiply(BigInteger.valueOf(cost())).add(BigInteger.valueOf(period - 1L))
            .divide(BigInteger.valueOf(period));
    }

    private static void assertDebit(GameTestHelper test, IKnowledgeProvider provider, BigInteger initial,
                                    int ticks, int period, String context) {
        BigInteger expected = initial.subtract(debit(ticks, period));
        test.assertTrue(provider.getEmc().equals(expected), context + " after " + ticks + " ticks: expected " + expected + ", got " + provider.getEmc());
    }

    private static ItemStack newFuel(ServerPlayer player) {
        var fuel = new ItemStack(cool.furry.mc.neoforge.projectexpansion.registries.Items.INFINITE_FUEL.get());
        fuel.set(DataComponentTypes.OWNER.get(), new DataComponentTypes.OwnerData(player.getUUID(), player.getName().getString()));
        return fuel;
    }

    private static DataComponentTypes.FuelCredit credit(ItemStack fuel) {
        return fuel.getOrDefault(DataComponentTypes.FUEL_CREDIT.get(), DataComponentTypes.FuelCredit.EMPTY);
    }

    private static void assertFuel(GameTestHelper test, ItemStack fuel, ServerPlayer player) {
        test.assertTrue(fuel.getItem() instanceof ItemInfiniteFuel && fuel.getCount() == 1, "Infinite fuel is not consumed");
        test.assertTrue(new DataComponentTypes.OwnerData(player.getUUID(), player.getName().getString()).equals(fuel.get(DataComponentTypes.OWNER.get())),
            "Owner data survives use and persistence");
    }

    private static int vanillaPeriod(Block block) {
        int duration = Config.server.infiniteFuelBurnTime.get();
        return block == Blocks.FURNACE ? duration : Math.max(1, duration / 2);
    }

    private static Item input(Block block) {
        return block == Blocks.SMOKER ? Items.BEEF : Items.IRON_ORE;
    }

    private static Item result(Block block) {
        return block == Blocks.SMOKER ? Items.COOKED_BEEF : Items.IRON_INGOT;
    }

    private static AbstractFurnaceBlockEntity vanillaFurnace(GameTestHelper test, Block block) {
        test.setBlock(POSITION, Blocks.AIR);
        test.setBlock(POSITION, block);
        return (AbstractFurnaceBlockEntity) test.getLevel().getBlockEntity(test.absolutePos(POSITION));
    }

    private static DMFurnaceBlockEntity matterFurnace(GameTestHelper test, String id) {
        test.setBlock(POSITION, Blocks.AIR);
        test.setBlock(POSITION, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("projecte", id)));
        return (DMFurnaceBlockEntity) test.getLevel().getBlockEntity(test.absolutePos(POSITION));
    }

    private static void tickVanilla(GameTestHelper test, AbstractFurnaceBlockEntity furnace, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            AbstractFurnaceBlockEntity.serverTick(test.getLevel(), furnace.getBlockPos(), test.getLevel().getBlockState(furnace.getBlockPos()), furnace);
        }
    }

    private static void tickMatter(GameTestHelper test, DMFurnaceBlockEntity furnace, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            DMFurnaceBlockEntity.tickServer(test.getLevel(), furnace.getBlockPos(), test.getLevel().getBlockState(furnace.getBlockPos()), furnace);
        }
    }

    private static int smeltVanilla(GameTestHelper test, AbstractFurnaceBlockEntity furnace, Block block, int ticks) {
        int smelted = 0;
        for (int tick = 0; tick < ticks; tick++) {
            if (furnace.getItem(0).getCount() < 2) furnace.setItem(0, new ItemStack(input(block), 64));
            tickVanilla(test, furnace, 1);
            ItemStack output = furnace.removeItemNoUpdate(2);
            test.assertTrue(output.isEmpty() || output.is(result(block)), "Vanilla furnace produces the recipe result");
            smelted += output.getCount();
        }
        return smelted;
    }

    private static int smeltMatter(GameTestHelper test, DMFurnaceBlockEntity furnace, int ticks) {
        int smelted = 0;
        for (int tick = 0; tick < ticks; tick++) {
            // Keep fast matter furnaces supplied and unblocked during all measured ticks.
            if (furnace.getInput().getStackInSlot(0).getCount() < 2) furnace.getInput().insertItem(0, new ItemStack(Items.IRON_ORE, 62), false);
            tickMatter(test, furnace, 1);
            smelted += drainIron(test, furnace.getOutput());
        }
        return smelted;
    }

    private static int drainIron(GameTestHelper test, IItemHandler output) {
        int count = 0;
        for (int slot = 0; slot < output.getSlots(); slot++) {
            var stack = output.extractItem(slot, Integer.MAX_VALUE, false);
            test.assertTrue(stack.isEmpty() || stack.is(Items.IRON_INGOT), "Matter furnace produces iron ingots");
            count += stack.getCount();
        }
        return count;
    }
}
