package tb;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.CrucibleRecipe;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;

final class ObeliskAndRecycling {
    static void register(DeferredRegister.Blocks blocks, DeferredRegister.Items items) {
        items.registerSimpleBlockItem(blocks.registerBlock("crying_obelisk", Obelisk::new,
            p -> p.strength(50, 1200).requiresCorrectToolForDrops()));
    }

    static final class Obelisk extends Block {
        static final BooleanProperty ABOVE = BooleanProperty.create("above");
        static final BooleanProperty BELOW = BooleanProperty.create("below");

        Obelisk(Properties properties) {
            super(properties);
            registerDefaultState(defaultBlockState().setValue(ABOVE, false).setValue(BELOW, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(ABOVE, BELOW);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            var pos = context.getClickedPos();
            var level = context.getLevel();
            return defaultBlockState().setValue(ABOVE, level.getBlockState(pos.above()).is(this))
                .setValue(BELOW, level.getBlockState(pos.below()).is(this));
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                         BlockPos pos, Direction direction, BlockPos neighborPos,
                                         BlockState neighbor, RandomSource random) {
            if (direction == Direction.UP) return state.setValue(ABOVE, neighbor.is(this));
            if (direction == Direction.DOWN) return state.setValue(BELOW, neighbor.is(this));
            return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!state.getValue(ABOVE) && !state.getValue(BELOW)) return InteractionResult.PASS;
            var base = state.getValue(ABOVE) ? pos : pos.below();
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.setRespawnPosition(new ServerPlayer.RespawnConfig(
                    LevelData.RespawnData.of(level.dimension(), base, player.getYRot(), 0), false), true);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public Optional<ServerPlayer.RespawnPosAngle> getRespawnPosition(BlockState state, EntityType<?> type,
                                                                        LevelReader level, BlockPos pos, float angle) {
            if (!level.getBlockState(pos.above()).is(this)) return Optional.empty();
            for (var direction : Direction.Plane.HORIZONTAL) {
                var exit = pos.relative(direction);
                var floor = exit.below();
                if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) continue;
                if (!level.isEmptyBlock(exit) || !level.isEmptyBlock(exit.above())) continue;
                return Optional.of(ServerPlayer.RespawnPosAngle.of(Vec3.atBottomCenterOf(exit), pos, angle));
            }
            return Optional.empty();
        }
    }

    static void research() {
        String recycling = "TB.Backprocessing";
        var blaze = recycle(recycling, "recycle_blaze_powder", "blaze_powder", "blaze_rod",
            new AspectList().add(Aspect.ORDER, 6).add(Aspect.FIRE, 3).add(Aspect.MAGIC, 2)
                .add(Aspect.CRAFT, 8).add(Aspect.EXCHANGE, 6).add(Aspect.ENERGY, 3));
        var bone = recycle(recycling, "recycle_bone_meal", "bone_meal", "bone",
            new AspectList().add(Aspect.ORDER, 2).add(Aspect.SENSES, 6).add(Aspect.CRAFT, 2)
                .add(Aspect.EXCHANGE, 1).add(Aspect.DEATH, 4));
        var cane = recycle(recycling, "recycle_sugar", "sugar", "sugar_cane",
            new AspectList().add(Aspect.ORDER, 1).add(Aspect.LIFE, 2));
        var cactus = recycle(recycling, "recycle_green_dye", "green_dye", "cactus",
            new AspectList().add(Aspect.ORDER, 1).add(Aspect.LIFE, 2).add(Aspect.EARTH, 1));
        ResearchCategories.addResearch(new BasesResearch(recycling, Bases.ID,
            new AspectList().add(Aspect.ORDER, 4).add(Aspect.EXCHANGE, 4).add(Aspect.CRAFT, 4), -2, -2, 2,
            TcStack.of("minecraft:blaze_rod")).setParents("TB.AlchemyRestoration")
            .setPages(ResearchPage.text("tc.research_page.TB.Backprocessing.1"), blaze, bone, cane, cactus));

        String key = "TB.CryingObs";
        String id = Bases.id("crying_obelisk").toString();
        var icon = TcStack.of(id);
        var lapis = TcIngredient.item("thaumicbases:old_lapis_block");
        var cost = new AspectList().add(Aspect.TRAVEL, 16).add(Aspect.LIFE, 16).add(Aspect.SOUL, 16);
        TcRecipes.register(new InfusionRecipe(id, key, icon, 2, cost, TcIngredient.item("minecraft:obsidian"),
            List.of(TcIngredient.item("minecraft:diamond"), TcIngredient.tag("minecraft:beds"), lapis, lapis)));
        ResearchCategories.addResearch(new BasesResearch(key, Bases.ID,
            new AspectList().add(Aspect.TRAVEL, 8).add(Aspect.SOUL, 8), 9, -2, 2, icon).setParents("INFUSION")
            .setPages(ResearchPage.text("tc.research_page.TB.CryingObs.1"), ResearchPage.infusion(id)));
    }

    private static ResearchPage recycle(String key, String name, String input, String output, AspectList cost) {
        String id = Bases.id(name).toString();
        TcRecipes.register(new CrucibleRecipe(id, key, TcStack.of("minecraft:" + output), TcIngredient.item("minecraft:" + input), cost));
        return ResearchPage.crucible(id);
    }
}
