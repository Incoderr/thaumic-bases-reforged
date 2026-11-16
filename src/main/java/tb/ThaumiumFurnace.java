package tb;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.alchemy.AlchemicalFurnaceBlock;
import thaumcraft.alchemy.AlchemicalFurnaceBlockEntity;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.ShapedArcaneRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.platform.Transfer;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;

public final class ThaumiumFurnace extends AlchemicalFurnaceBlock {
    private static final String NAME = "advanced_alchemical_furnace";
    private static final String RESEARCH = "TB.AdvAlc";
    private static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static java.util.function.Supplier<ThaumiumFurnace> block;
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Bases.ID);
    private static final java.util.function.Supplier<BlockEntityType<FurnaceEntity>> TYPE = TYPES.register(NAME,
        () -> new BlockEntityType<>(FurnaceEntity::new, block.get()));

    private ThaumiumFurnace(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    static void register(DeferredRegister.Blocks blocks, DeferredRegister.Items items) {
        var furnace = blocks.registerBlock(NAME, ThaumiumFurnace::new,
            p -> p.strength(3.5F).sound(SoundType.STONE).requiresCorrectToolForDrops().lightLevel(AlchemicalFurnaceBlock::lightLevel));
        block = furnace;
        items.registerSimpleBlockItem(furnace);
    }

    static void registerEntities(IEventBus bus) {
        TYPES.register(bus);
        Transfer.exposeItems(TYPE, AlchemicalFurnaceBlockEntity::exposedItems);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FurnaceEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != TYPE.get()) return null;
        return (world, pos, blockState, entity) ->
            AlchemicalFurnaceBlockEntity.serverTick(world, pos, blockState, (FurnaceEntity) entity);
    }

    private static final class FurnaceEntity extends AlchemicalFurnaceBlockEntity {
        FurnaceEntity(BlockPos pos, BlockState state) {
            super(TYPE.get(), pos, state);
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable(getBlockState().getBlock().getDescriptionId());
        }
    }

    static void research() {
        String id = Bases.id(NAME).toString();
        var result = TcStack.of(id);
        var ingot = TcIngredient.item("thaumcraft:thaumium_ingot");
        var furnace = TcIngredient.item("thaumcraft:alchemical_furnace");
        var bellows = TcIngredient.item("thaumcraft:arcane_bellows");
        TcRecipes.register(new ShapedArcaneRecipe(id, RESEARCH, result,
            new AspectList().add(Aspect.WATER, 20).add(Aspect.FIRE, 20).add(Aspect.AIR, 10), 3, 3,
            List.of(ingot, TcIngredient.EMPTY, ingot, ingot, furnace, ingot, bellows, bellows, bellows)));
        ResearchCategories.addResearch(new BasesResearch(RESEARCH, Bases.ID,
            new AspectList().add(Aspect.MOTION, 16).add(Aspect.AIR, 16).add(Aspect.FIRE, 8).add(Aspect.CRAFT, 4),
            7, 3, 2, result).setParents("THAUMIUM", "DISTILESSENTIA", "BELLOWS")
            .setPages(ResearchPage.text("tc.research_page." + RESEARCH + ".1"), ResearchPage.arcane(id)));
    }
}
