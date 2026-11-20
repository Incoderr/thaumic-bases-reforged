package tb;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.aspect.AspectSync;
import thaumcraft.crafting.CrucibleRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;

final class Aurelia {
    static void register(PortRegistration.Blocks blocks, PortRegistration.Items items) {
        var fallen = blocks.registerBlock("fallen_aurelia_petal", Petal::new,
            p -> p.replaceable().noCollission().instabreak().sound(SoundType.GRASS));
        items.registerSimpleBlockItem(blocks.registerBlock("aurelia", p -> new Flower(p, fallen),
            p -> p.noCollission().instabreak().randomTicks().sound(SoundType.GRASS)));
        var petal = items.registerSimpleItem("aurelia_petal");
        AspectSync.addRegistrar(registry -> registry.register(petal.get(), new AspectList().add(Aspect.AURA, 2)));
    }

    private static class Petal extends BushBlock {
        Petal(Properties properties) { super(properties); }
        @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BushBlock> codec() { return simpleCodec(Petal::new); }

        @Override
        protected boolean mayPlaceOn(BlockState soil, BlockGetter level, BlockPos pos) {
            return soil.is(BlockTags.DIRT) || soil.is(Blocks.GRASS_BLOCK);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Block.box(8 - (8) / 2.0, 0, 8 - (8) / 2.0, 8 + (8) / 2.0, 1, 8 + (8) / 2.0);
        }
    }

    static final class Flower extends Petal {
        static final BooleanProperty OPEN = BooleanProperty.create("open");
        private final java.util.function.Supplier<? extends Block> fallen;

        Flower(Properties properties, java.util.function.Supplier<? extends Block> fallen) {
            super(properties);
            this.fallen = fallen;
            registerDefaultState(defaultBlockState().setValue(OPEN, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(OPEN);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Block.box(8 - (state.getValue(OPEN) ? 12 : 8) / 2.0, 0, 8 - (state.getValue(OPEN) ? 12 : 8) / 2.0, 8 + (state.getValue(OPEN) ? 12 : 8) / 2.0, 13, 8 + (state.getValue(OPEN) ? 12 : 8) / 2.0);
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            long time = Math.floorMod(level.getDayTime(), 24000);
            boolean open = level.dimension().equals(Level.OVERWORLD) && time >= 13000 && time < 23000
                && level.getMoonBrightness() >= 1 && level.canSeeSky(pos.above());
            if (state.getValue(OPEN) != open) level.setBlockAndUpdate(pos, state.setValue(OPEN, open));
            if (!open) return;
            var target = pos.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
            if (!level.hasChunkAt(target) || !level.isEmptyBlock(target)) return;
            var petal = fallen.get().defaultBlockState();
            if (petal.canSurvive(level, target)) level.setBlockAndUpdate(target, petal);
        }
    }

    static void research() {
        String key = "TB.Aurelia";
        String flower = Bases.id("aurelia").toString();
        var cost = new AspectList().add(Aspect.PLANT, 8).add(Aspect.ELDRITCH, 2).add(Aspect.AURA, 2);
        TcRecipes.register(new CrucibleRecipe(flower, key, TcStack.of(flower), TcIngredient.item("minecraft:pink_tulip"), cost));
        ResearchCategories.addResearch(new BasesResearch(key, Bases.ID,
            new AspectList().add(Aspect.PLANT, 6).add(Aspect.AURA, 6).add(Aspect.ELDRITCH, 4),
            -7, 0, 2, TcStack.of(flower)).setParents("TB.BasicPlants")
            .setPages(ResearchPage.text("tc.research_page.TB.Aurelia.1"), ResearchPage.crucible(flower)));
    }
}
