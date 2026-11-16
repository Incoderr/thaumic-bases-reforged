package tb;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.ShapedArcaneRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;
import thaumcraft.wand.WandInteractable;

final class ThaumicRelocator extends Block implements EntityBlock, WandInteractable {
    private static final String PUSHER = "thaumic_relocator";
    private static final String PULLER = "thaumic_attractor";
    private static final String RESEARCH = "TB.Relocator";
    private static final int RANGE_PER_BLOCK = 10;
    private static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    private static java.util.function.Supplier<ThaumicRelocator> pusher;
    private static java.util.function.Supplier<ThaumicRelocator> puller;
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Bases.ID);
    private static final java.util.function.Supplier<BlockEntityType<RelocatorEntity>> TYPE = TYPES.register("relocator",
        () -> new BlockEntityType<>(RelocatorEntity::new, pusher.get(), puller.get()));

    private final boolean pulls;

    private ThaumicRelocator(Properties properties, boolean pulls) {
        super(properties);
        this.pulls = pulls;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP));
    }

    static void register(DeferredRegister.Blocks blocks, DeferredRegister.Items items) {
        var push = blocks.registerBlock(PUSHER, p -> new ThaumicRelocator(p, false), p -> p.strength(2.5F, 15).sound(SoundType.WOOD));
        var pull = blocks.registerBlock(PULLER, p -> new ThaumicRelocator(p, true), p -> p.strength(2.5F, 15).sound(SoundType.WOOD));
        pusher = push;
        puller = pull;
        items.registerSimpleBlockItem(push);
        items.registerSimpleBlockItem(pull);
    }

    static void registerEntities(net.neoforged.bus.api.IEventBus bus) {
        TYPES.register(bus);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public InteractionResult onWandUseOn(Level level, BlockPos pos, Player player, ItemStack stack, Direction face) {
        if (!level.isClientSide()) {
            Direction direction = player.isShiftKeyDown() ? face.getOpposite() : face;
            level.setBlockAndUpdate(pos, level.getBlockState(pos).setValue(FACING, direction));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RelocatorEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == TYPE.get() ? (world, pos, blockState, entity) -> moveEntities(world, pos, blockState) : null;
    }

    int range(Level level, BlockPos pos, Direction direction) {
        int limit = RANGE_PER_BLOCK;
        for (BlockPos behind = pos.relative(direction.getOpposite()); level.isLoaded(behind); behind = behind.relative(direction.getOpposite())) {
            if (!(level.getBlockState(behind).getBlock() instanceof ThaumicRelocator) || level.hasNeighborSignal(behind)) break;
            limit += RANGE_PER_BLOCK;
        }
        int distance = 0;
        while (distance < limit) {
            BlockPos next = pos.relative(direction, distance + 1);
            if (!level.isLoaded(next)) break;
            BlockState obstruction = level.getBlockState(next);
            if (obstruction.getBlock() instanceof ThaumicRelocator || obstruction.isSolidRender()) break;
            distance++;
        }
        return distance;
    }

    void moveEntities(Level level, BlockPos pos, BlockState state) {
        if (level.hasNeighborSignal(pos)) return;
        Direction face = state.getValue(FACING);
        int distance = range(level, pos, face);
        if (distance == 0) return;
        var beam = new AABB(pos.relative(face)).minmax(new AABB(pos.relative(face, distance)));
        Direction direction = pulls ? face.getOpposite() : face;
        Vec3 force = new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
        for (Entity entity : level.getEntitiesOfClass(Entity.class, beam, e -> e.isAlive() && !e.isSpectator() && !e.isPassenger()
            && (e.isPushable() || e instanceof net.minecraft.world.entity.item.ItemEntity
                || e instanceof net.minecraft.world.entity.animal.equine.AbstractHorse))) {
            Vec3 velocity = entity.getDeltaMovement();
            double speed = velocity.dot(force);
            if (entity.isShiftKeyDown()) {
                if (direction.getAxis().isVertical() && velocity.y < 0) {
                    entity.setDeltaMovement(velocity.x, velocity.y * 0.9, velocity.z);
                }
            } else if (speed < 0.35) {
                entity.setDeltaMovement(velocity.add(force.scale(Math.min(0.1, 0.35 - speed))));
            }
            if (direction.getAxis().isVertical()) entity.fallDistance = 0;
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Direction face = state.getValue(FACING);
        if (level.hasNeighborSignal(pos) || range(level, pos, face) == 0) return;
        Vec3 source = Vec3.atCenterOf(pos).relative(face, 0.65);
        Direction flow = pulls ? face.getOpposite() : face;
        level.addParticle(ParticleTypes.END_ROD, source.x, source.y, source.z,
            flow.getStepX() * 0.09, flow.getStepY() * 0.09, flow.getStepZ() * 0.09);
    }

    private static final class RelocatorEntity extends BlockEntity {
        RelocatorEntity(BlockPos pos, BlockState state) {
            super(TYPE.get(), pos, state);
        }
    }

    static void research() {
        recipe(PUSHER, "nitor");
        recipe(PULLER, "alumentum");
        String push = Bases.id(PUSHER).toString();
        String pull = Bases.id(PULLER).toString();
        ResearchCategories.addResearch(new BasesResearch(RESEARCH, Bases.ID,
            new AspectList().add(Aspect.AIR, 4).add(Aspect.FLIGHT, 4).add(Aspect.MOTION, 4),
            0, 8, 1, TcStack.of(push)).setParents("LEVITATOR")
            .setPages(ResearchPage.text("tc.research_page." + RESEARCH + ".1"),
                ResearchPage.arcane(push), ResearchPage.arcane(pull)));
    }

    private static void recipe(String name, String fuel) {
        String id = Bases.id(name).toString();
        var wood = TcIngredient.item("thaumcraft:greatwood_planks");
        var air = TcIngredient.item("thaumcraft:air_shard");
        var earth = TcIngredient.item("thaumcraft:earth_shard");
        var water = TcIngredient.item("thaumcraft:water_shard");
        var vis = new AspectList().add(Aspect.AIR, 5).add(Aspect.EARTH, 15).add(Aspect.WATER, 5);
        TcRecipes.register(new ShapedArcaneRecipe(id, RESEARCH, TcStack.of(id), vis, 3, 3,
            List.of(wood, air, wood, earth, TcIngredient.item("thaumcraft:" + fuel), earth, wood, water, wood)));
    }
}
