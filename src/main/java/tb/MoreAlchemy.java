package tb;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import java.util.Optional;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.CrucibleRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;

final class MoreAlchemy {
    private static final String BUCKET = "pyrofluid_bucket";
    private static final String PYROFLUID = "TB.Pyrofluid";

    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Bases.ID);
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Bases.ID);
    static final net.neoforged.neoforge.registries.DeferredHolder<FluidType, FluidType> FLUID_TYPE = TYPES.register("pyrofluid",
        () -> new FluidType(FluidType.Properties.create().density(3000).viscosity(6000).temperature(1300).lightLevel(15)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)));
    static final net.neoforged.neoforge.registries.DeferredHolder<Fluid, Pyrofluid> FLUID = FLUIDS.register("pyrofluid", () -> new Pyrofluid(true));
    static final net.neoforged.neoforge.registries.DeferredHolder<Fluid, Pyrofluid> FLOWING =
        FLUIDS.register("flowing_pyrofluid", () -> new Pyrofluid(false));

    private static final int CRYSTAL_DELAY = 30;
    private static java.util.function.Supplier<PyrofluidBlock> pyrofluidBlock;
    private static java.util.function.Supplier<LiquidBlock> flowingBlock;
    private static java.util.function.Supplier<net.minecraft.world.item.Item> pyrofluidBucket;

    static void registerFluids(IEventBus bus) {
        TYPES.register(bus);
        FLUIDS.register(bus);
    }

    private static BaseFlowingFluid.Properties fluidProperties() {
        return new BaseFlowingFluid.Properties(FLUID_TYPE, FLUID, FLOWING).bucket(pyrofluidBucket)
            .block(flowingBlock);
    }

    private static final class Pyrofluid extends BaseFlowingFluid {
        private final boolean source;

        Pyrofluid(boolean source) {
            super(fluidProperties());
            this.source = source;
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) { return source; }

        @Override
        public int getAmount(FluidState state) { return source ? 8 : state.getValue(LEVEL); }

        @Override
        protected BlockState createLegacyBlock(FluidState state) {
            return source ? pyrofluidBlock.get().defaultBlockState() : super.createLegacyBlock(state);
        }

        @Override
        public int getTickDelay(LevelReader level) { return fastLava(level) ? 10 : 30; }

        @Override
        public int getSlopeFindDistance(LevelReader level) { return fastLava(level) ? 4 : 2; }

        @Override
        public int getDropOff(LevelReader level) { return fastLava(level) ? 1 : 2; }

        private static boolean fastLava(LevelReader level) {
            return level.dimensionType().ultraWarm();
        }
    }

    static void register(PortRegistration.Blocks blocks, PortRegistration.Items items) {
        var pyrofluid = blocks.registerBlock("pyrofluid", PyrofluidBlock::new,
            p -> p.strength(0.6F).noCollission().noOcclusion().randomTicks()
                .lightLevel(state -> state.getValue(PyrofluidBlock.AGE) == PyrofluidBlock.MATURE ? 0 : 15));
        pyrofluidBlock = pyrofluid;
        flowingBlock = blocks.registerBlock("flowing_pyrofluid", p -> new LiquidBlock(FLUID.get(), p) {
            @Override
            protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
                super.entityInside(state, level, pos, entity);
                burn(level, entity);
            }
        }, p -> p.noCollission().noOcclusion().replaceable().noLootTable().lightLevel(state -> 12));
        pyrofluidBucket = items.registerItem(BUCKET, p -> new BucketItem(FLUID.get(), p.stacksTo(1).craftRemainder(Items.BUCKET)));
    }

    static final class PyrofluidBlock extends Block implements BucketPickup {
        static final int MATURE = 8;
        static final IntegerProperty AGE = IntegerProperty.create("age", 0, MATURE);

        PyrofluidBlock(Properties properties) {
            super(properties);
            registerDefaultState(defaultBlockState().setValue(AGE, 0));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(AGE);
        }

        @Override
        protected FluidState getFluidState(BlockState state) {
            return state.getValue(AGE) == MATURE ? Fluids.EMPTY.defaultFluidState() : FLUID.get().defaultFluidState();
        }

        @Override
        protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
            if (state.getValue(AGE) == MATURE || oldState.is(this)) return;
            level.scheduleTick(pos, FLUID.get(), FLUID.get().getTickDelay(level));
            level.scheduleTick(pos, this, CRYSTAL_DELAY);
        }

        @Override
        public ItemStack pickupBlock(Player user, LevelAccessor level, BlockPos pos, BlockState state) {
            if (state.getValue(AGE) == MATURE || !level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)) {
                return ItemStack.EMPTY;
            }
            return new ItemStack(pyrofluidBucket.get());
        }

        @Override
        public Optional<SoundEvent> getPickupSound() {
            return Optional.of(SoundEvents.BUCKET_FILL_LAVA);
        }

        @Override
        protected boolean isRandomlyTicking(BlockState state) {
            return state.getValue(AGE) < MATURE;
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
                level.scheduleTick(pos, this, CRYSTAL_DELAY);
                level.scheduleTick(pos, FLUID.get(), FLUID.get().getTickDelay(level));
            }
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            int age = state.getValue(AGE);
            if (age >= MATURE) return;
            level.setBlockAndUpdate(pos, state.setValue(AGE, age + 1));
            if (age + 1 < MATURE) {
                level.scheduleTick(pos, this, CRYSTAL_DELAY);
                level.scheduleTick(pos, FLUID.get(), FLUID.get().getTickDelay(level));
            }
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            if (state.getValue(AGE) != MATURE) burn(level, entity);
        }

        @Override
        protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
            return state.getValue(AGE) == MATURE ? super.getDestroyProgress(state, player, level, pos) : 0;
        }

        @Override
        protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
            if (state.getValue(AGE) != MATURE) return List.of();
            return List.of(new ItemStack(Items.BLAZE_POWDER, 5 + params.getLevel().getRandom().nextInt(32)));
        }
    }

    private static void burn(Level level, Entity entity) {
        if (!(level instanceof ServerLevel server) || entity.fireImmune()) return;
        entity.igniteForSeconds(15);
        entity.hurt(level.damageSources().lava(), 4);
    }

    static void research() {
        String duplicate = "TB.SimpleDublication";
        var arrow = recipe(duplicate, "duplicate_arrow", "minecraft:arrow", TcStack.of("minecraft:arrow", 2),
            new AspectList().add(Aspect.WEAPON, 1));
        var snowball = recipe(duplicate, "duplicate_snowball", "minecraft:snowball", TcStack.of("minecraft:snowball", 2),
            new AspectList().add(Aspect.COLD, 1));
        var redstone = recipe(duplicate, "duplicate_redstone", "minecraft:redstone", TcStack.of("minecraft:redstone", 2),
            new AspectList().add(Aspect.MECHANISM, 1).add(Aspect.ENERGY, 1));
        entry(duplicate, 2, -3, 1, TcStack.of("minecraft:arrow"),
            new AspectList().add(Aspect.EXCHANGE, 6).add(Aspect.ORDER, 6), arrow, snowball, redstone);

        String amber = "TB.Amber";
        var amberStack = TcStack.of("thaumcraft:amber");
        var amberPage = recipe(amber, "amber_from_spruce", "minecraft:spruce_log", amberStack, new AspectList().add(Aspect.TRAP, 2));
        entry(amber, 1, -3, 1, amberStack, new AspectList().add(Aspect.TRAP, 6).add(Aspect.ORDER, 6), amberPage);

        var bucket = TcStack.of(Bases.id(BUCKET).toString());
        var pyrofluid = recipe(PYROFLUID, BUCKET, "minecraft:lava_bucket", bucket,
            new AspectList().add(Aspect.FIRE, 24).add(Aspect.MAGIC, 8));
        entry(PYROFLUID, 3, -2, 4, bucket,
            new AspectList().add(Aspect.FIRE, 16).add(Aspect.MAGIC, 6).add(Aspect.ELDRITCH, 8).add(Aspect.GREED, 6), pyrofluid);
    }

    private static void entry(String key, int column, int row, int complexity, TcStack icon, AspectList aspects,
                              ResearchPage... recipes) {
        var pages = new ArrayList<ResearchPage>();
        pages.add(ResearchPage.text("tc.research_page." + key + ".1"));
        pages.addAll(List.of(recipes));
        ResearchCategories.addResearch(new BasesResearch(key, Bases.ID, aspects, column, row, complexity, icon)
            .setParents("CRUCIBLE").setPages(pages.toArray(ResearchPage[]::new)));
    }

    private static ResearchPage recipe(String key, String name, String input, TcStack output, AspectList cost) {
        String id = Bases.id(name).toString();
        TcRecipes.register(new CrucibleRecipe(id, key, output, TcIngredient.item(input), cost));
        return ResearchPage.crucible(id);
    }
}
