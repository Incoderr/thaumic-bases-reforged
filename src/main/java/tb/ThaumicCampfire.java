package tb;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.ShapedArcaneRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.registry.ModBlocks;
import thaumcraft.essentia.EssentiaHandler;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;

final class ThaumicCampfire extends Block implements EntityBlock {
    private static final String ID = "thaumic_campfire";
    private static final String BRAZIER = "thaumic_brazier";
    private static final int TICK_INTERVAL = 10;
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Bases.ID);
    static final BooleanProperty HAS_LOGS = BooleanProperty.create("has_logs");
    private static final VoxelShape SHAPE = Block.box(8 - (15) / 2.0, 0, 8 - (15) / 2.0, 8 + (15) / 2.0, 2, 8 + (15) / 2.0);
    private static final VoxelShape LOADED_SHAPE = Block.box(8 - (15) / 2.0, 0, 8 - (15) / 2.0, 8 + (15) / 2.0, 10, 8 + (15) / 2.0);
    private static final VoxelShape BRAZIER_SHAPE = Block.box(8 - (14) / 2.0, 0, 8 - (14) / 2.0, 8 + (14) / 2.0, 9, 8 + (14) / 2.0);
    private static Supplier<ThaumicCampfire> block;
    private static Supplier<ThaumicCampfire> brazier;
    private final boolean automatic;
    private static final Supplier<BlockEntityType<Fire>> FIRE = ENTITIES.register(ID,
        () -> BlockEntityType.Builder.of(Fire::new, block.get(), brazier.get()).build(null));

    ThaumicCampfire(Properties properties, boolean automatic) {
        super(properties);
        this.automatic = automatic;
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.LIT, false).setValue(HAS_LOGS, false));
    }

    static void register(PortRegistration.Blocks blocks, PortRegistration.Items items) {
        var campfire = blocks.registerBlock(ID, p -> new ThaumicCampfire(p, false), p -> p.strength(1.0F)
            .noOcclusion().sound(SoundType.WOOD).lightLevel(s -> s.getValue(BlockStateProperties.LIT) ? 15 : 0));
        items.registerSimpleBlockItem(campfire);
        block = campfire;
        var automaticFire = blocks.registerBlock(BRAZIER, p -> new ThaumicCampfire(p, true),
            p -> p.strength(2.0F).noOcclusion().sound(SoundType.STONE)
                .lightLevel(s -> s.getValue(BlockStateProperties.LIT) ? 15 : 0));
        items.registerSimpleBlockItem(automaticFire);
        brazier = automaticFire;
    }

    static void registerEntities(IEventBus bus) { ENTITIES.register(bus); }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.LIT, HAS_LOGS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (automatic) return BRAZIER_SHAPE;
        return state.getValue(HAS_LOGS) || state.getValue(BlockStateProperties.LIT) ? LOADED_SHAPE : SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new Fire(pos, state); }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (automatic || !(level.getBlockEntity(pos) instanceof Fire fire)
            || !(stack.is(ItemTags.LOGS) || stack.getBurnTime(null) > 0)) return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level instanceof ServerLevel server) {
            if (!fire.add(stack, server)) return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            var remainderTemplate = stack.getCraftingRemainingItem();
            var remainder = remainderTemplate == null ? ItemStack.EMPTY : remainderTemplate.copy();
            stack.consume(1, player);
            if (!player.hasInfiniteMaterials() && !remainder.isEmpty() && !player.getInventory().add(remainder)) {
                player.drop(remainder, false);
            }
            fire.setChanged();
            updateModel(level, pos, state, fire);
            if (fire.lit()) level.scheduleTick(pos, this, TICK_INTERVAL);
        }
        return net.minecraft.world.ItemInteractionResult.SUCCESS;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (automatic && !level.isClientSide() && !oldState.is(this)) level.scheduleTick(pos, this, TICK_INTERVAL);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof Fire fire)) return;
        int previousFuel = fire.fuel;
        if (automatic) {
            fire.fuel = Math.max(0, fire.fuel - TICK_INTERVAL);
            if (fire.fuel == 0 && EssentiaHandler.drainEssentia(fire, Aspect.FIRE, null, 6)) fire.fuel = 1600;
        } else if (fire.lit()) {
            fire.logs = Math.max(0, fire.logs - TICK_INTERVAL);
            fire.fuel = fire.logs == 0 ? 0 : Math.max(0, fire.fuel - TICK_INTERVAL);
        }
        if (previousFuel != fire.fuel) fire.setChanged();
        updateModel(level, pos, state, fire);
        if (automatic || fire.lit()) level.scheduleTick(pos, this, TICK_INTERVAL);
        if (!fire.lit()) return;

        var center = Vec3.atCenterOf(pos);
        for (var mob : level.getEntitiesOfClass(PathfinderMob.class, new AABB(pos).inflate(12, 6, 12),
            entity -> entity instanceof Enemy && entity.isAlive())) {
            if (mob.getTarget() != null && random.nextInt(4) == 0) continue;
            var away = DefaultRandomPos.getPosAway(mob, 10, 4, center);
            if (away != null) mob.getNavigation().moveTo(away.x, away.y, away.z, 1.2);
        }
    }

    private static void updateModel(Level level, BlockPos pos, BlockState state, Fire fire) {
        var updated = state.setValue(BlockStateProperties.LIT, fire.lit()).setValue(HAS_LOGS, fire.logs > 0);
        if (updated != state) level.setBlockAndUpdate(pos, updated);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(BlockStateProperties.LIT) && random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 0.55, pos.getZ() + 0.5,
                0, 0.035, 0);
        }
    }

    static final class Fire extends BlockEntity {
        int logs;
        int fuel;

        Fire(BlockPos pos, BlockState state) { super(FIRE.get(), pos, state); }

        boolean lit() { return fuel > 0; }

        @Override
        public void onLoad() {
            super.onLoad();
            if (getLevel() instanceof ServerLevel server
                && (getBlockState().getValue(HAS_LOGS) != (logs > 0)
                    || getBlockState().getValue(BlockStateProperties.LIT) != lit())) {
                server.scheduleTick(getBlockPos(), getBlockState().getBlock(), 1);
            }
        }

        boolean add(ItemStack stack, ServerLevel level) {
            if (logs == 0 && stack.is(ItemTags.LOGS)) {
                logs = stack.is(ModBlocks.GREATWOOD_LOG.get().asItem()) || stack.is(ModBlocks.SILVERWOOD_LOG.get().asItem()) ? 6400 : 1600;
                return true;
            }
            if (logs == 0 || fuel > 0) return false;
            var params = new LootParams.Builder(level)
                .withParameter(LootContextParams.BLOCK_STATE, getBlockState())
                .withParameter(LootContextParams.BLOCK_ENTITY, this)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(getBlockPos()))
                .withParameter(LootContextParams.TOOL, stack)
                .create(LootContextParamSets.BLOCK);
            fuel = stack.getBurnTime(null);
            return fuel > 0;
        }

        @Override
        protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            var input = new PortNbt.Input(tag, registries);
            logs = input.getIntOr("logs", 0);
            fuel = input.getIntOr("fuel", 0);
        }

        @Override
        protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            var output = new PortNbt.Output(tag, registries);
            output.putInt("logs", logs);
            output.putInt("fuel", fuel);
        }
    }

    static void research() {
        String key = "TB.Campfire";
        String id = Bases.id(ID).toString();
        var stick = TcIngredient.item("minecraft:stick");
        var stone = TcIngredient.item("minecraft:cobblestone");
        var gravel = TcIngredient.item("minecraft:gravel");
        var result = TcStack.of(id);
        TcRecipes.register(new ShapedArcaneRecipe(id, key, result, new AspectList().add(Aspect.FIRE, 5).add(Aspect.ENTROPY, 3),
            3, 3, List.of(stick, stick, stick, stone, TcIngredient.tag("minecraft:coals"), stone, gravel, gravel, gravel)));
        var tags = new AspectList().add(Aspect.FIRE, 3).add(Aspect.MIND, 3).add(Aspect.ENTROPY, 3);
        ResearchCategories.addResearch(new BasesResearch(key, Bases.ID, tags, 3, 6, 1, result)
            .setPages(ResearchPage.text("tc.research_page.TB.Campfire.1"), ResearchPage.arcane(id)));

        String brazierKey = "TB.Brazier";
        String brazierId = Bases.id(BRAZIER).toString();
        var slab = TcIngredient.item("thaumcraft:arcane_stone_slab");
        var empty = TcIngredient.EMPTY;
        var brazierResult = TcStack.of(brazierId);
        TcRecipes.register(new ShapedArcaneRecipe(brazierId, brazierKey, brazierResult,
            new AspectList().add(Aspect.FIRE, 5).add(Aspect.ENTROPY, 3), 3, 3,
            List.of(slab, TcIngredient.tag("minecraft:coals"), slab, empty,
                TcIngredient.item("thaumcraft:arcane_stone"), empty, slab, slab, slab)));
        var brazierTags = tags.copy().add(Aspect.MAGIC, 3).add(Aspect.MOTION, 3);
        ResearchCategories.addResearch(new BasesResearch(brazierKey, Bases.ID, brazierTags, 3, 8, 1, brazierResult)
            .setParents(key).setPages(ResearchPage.text("tc.research_page.TB.Brazier.1"), ResearchPage.arcane(brazierId)));
    }
}
