package tb;

import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.CrucibleRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchManager;
import thaumcraft.research.Warp;
import thaumcraft.research.WarpType;
import thaumcraft.research.ResearchPage;

final class RosaMysteria {
    private static final String KEY = "TB.Knose";
    private static final String SPROUT = "rosa_mysteria_sprout";

    enum Fragment {
        AIR(Aspect.AIR), FIRE(Aspect.FIRE), WATER(Aspect.WATER), EARTH(Aspect.EARTH),
        ORDER(Aspect.ORDER), ENTROPY(Aspect.ENTROPY), MIXED(null), TAINTED(null);

        private final Aspect aspect;

        Fragment(Aspect aspect) { this.aspect = aspect; }

        private final String name = name().toLowerCase(Locale.ROOT);

        String item() { return name + "_mysteria_fragment"; }

        private static Fragment forSoil(BlockState soil) {
            var key = BuiltInRegistries.BLOCK.getKey(soil.getBlock());
            if (!key.getNamespace().equals(Bases.ID)) return null;
            for (var fragment : values()) {
                if (key.getPath().equals(fragment.name + "_crystal_block")) return fragment;
            }
            return null;
        }
    }

    static void register(DeferredRegister.Blocks blocks, DeferredRegister.Items items) {
        var crop = blocks.registerBlock("rosa_mysteria", MysteriaCrop::new,
            p -> p.noCollision().instabreak().randomTicks().sound(SoundType.CROP));
        items.registerItem(SPROUT, p -> new BlockItem(crop.get(), p));
        for (var fragment : Fragment.values()) items.registerItem(fragment.item(), p -> new FragmentItem(p, fragment));
    }

    private static final class FragmentItem extends Item {
        private final Fragment fragment;

        FragmentItem(Properties properties, Fragment fragment) {
            super(properties);
            this.fragment = fragment;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (player instanceof ServerPlayer serverPlayer) {
                switch (fragment) {
                    case MIXED -> {
                        for (var primal : Aspect.getPrimalAspects()) ResearchManager.addAspectPool(serverPlayer, primal, 2);
                    }
                    case TAINTED -> awardTaintedKnowledge(serverPlayer);
                    default -> ResearchManager.addAspectPool(serverPlayer, fragment.aspect, 8);
                }
                player.getItemInHand(hand).consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }

        private static void awardTaintedKnowledge(ServerPlayer player) {
            var random = player.getRandom();
            int awarded = 0;
            for (var aspect : Aspect.getCompoundAspects()) {
                if (aspect == Aspect.TAINT) {
                    ResearchManager.addAspectPool(player, aspect, 8);
                    Warp.add(player, 2, WarpType.TEMPORARY);
                    awarded++;
                    continue;
                }
                if (random.nextBoolean()) {
                    ResearchManager.addAspectPool(player, aspect, 1);
                    awarded++;
                }
                if (random.nextInt(24) == 0) break;
            }
            for (int i = 0; i < awarded; i++) {
                if (random.nextFloat() <= 0.1F) {
                    Warp.add(player, 1, random.nextFloat() <= 0.15F ? WarpType.TEMPORARY : WarpType.STICKY);
                }
            }
        }
    }

    static final class MysteriaCrop extends CropBlock {
        MysteriaCrop(Properties properties) { super(properties); }

        @Override
        public int getMaxAge() { return 3; }

        @Override
        protected ItemLike getBaseSeedId() { return BuiltInRegistries.ITEM.getValue(Bases.id(SPROUT)); }

        @Override
        protected boolean mayPlaceOn(BlockState soil, BlockGetter level, BlockPos pos) {
            return Fragment.forSoil(soil) != null;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.box(0.25, 0, 0.25, 0.75, (getAge(state) + 1) / 4.0, 0.75);
        }

        @Override
        protected int getBonemealAgeIncrease(Level level) { return 1; }

        @Override
        protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
            var pos = BlockPos.containing(params.getParameter(LootContextParams.ORIGIN));
            var soil = Fragment.forSoil(params.getLevel().getBlockState(pos.below()));
            var seed = new ItemStack(getBaseSeedId());
            if (isMaxAge(state) && soil != null) {
                return List.of(seed, new ItemStack(BuiltInRegistries.ITEM.getValue(Bases.id(soil.item()))));
            }
            return List.of(seed);
        }
    }

    static void research() {
        String sprout = Bases.id(SPROUT).toString();
        var cost = new AspectList().add(Aspect.MIND, 16).add(Aspect.LIFE, 8).add(Aspect.ENERGY, 2);
        TcRecipes.register(new CrucibleRecipe(sprout, KEY, TcStack.of(sprout), TcIngredient.item("minecraft:rose_bush"), cost));
        var pages = new java.util.ArrayList<ResearchPage>();
        pages.add(ResearchPage.text("tc.research_page.TB.Knose.1"));
        pages.add(ResearchPage.crucible(sprout));
        var result = TcStack.of("thaumcraft:knowledge_fragment");
        for (var fragment : Fragment.values()) {
            String item = Bases.id(fragment.item()).toString();
            String recipe = Bases.id("knowledge_from_" + fragment.name).toString();
            TcRecipes.register(new CrucibleRecipe(recipe, KEY, result, TcIngredient.item(item),
                new AspectList().add(Aspect.ORDER, 1).add(Aspect.EXCHANGE, 1)));
            pages.add(ResearchPage.crucible(recipe));
        }
        ResearchCategories.addResearch(new BasesResearch(KEY, Bases.ID, new AspectList().add(Aspect.LIFE, 6).add(Aspect.MIND, 6).add(Aspect.EARTH, 6), -6, 0, 1, TcStack.of(sprout))
            .setParents("TB.Aurelia", "TB.CrystalBlocks").setPages(pages.toArray(ResearchPage[]::new)));
    }
}
