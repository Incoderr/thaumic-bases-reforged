package tb;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.block.InfusionStabiliser;
import thaumcraft.crafting.CraftingRecipeRef;
import thaumcraft.crafting.CrucibleRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;

final class CrystalBuilding {
    private static final String RESEARCH = "TB.CrystalBlocks";
    private static final List<String> LEGACY = List.of("gold_etched_obsidian", "old_bricks", "old_cobblestone", "old_mossy_cobblestone",
        "old_diamond_block", "old_gold_block", "old_iron_block", "old_lapis_block");

    private enum Crystal {
        AIR(Aspect.AIR), FIRE(Aspect.FIRE), WATER(Aspect.WATER), EARTH(Aspect.EARTH),
        ORDER(Aspect.ORDER), ENTROPY(Aspect.ENTROPY), MIXED(null), TAINTED(null);

        private final Aspect aspect;
        private final String name;

        Crystal(Aspect aspect) {
            this.aspect = aspect;
            this.name = name().toLowerCase(java.util.Locale.ROOT);
        }

        String block() {
            return name + "_crystal_block";
        }

        String slab() {
            return name + "_crystal_slab";
        }

        String catalyst() {
            return "thaumcraft:" + name + "_crystal_cluster";
        }

        AspectList cost() {
            var cost = primals(1).add(Aspect.CRAFT, 2);
            if (aspect != null) cost.add(aspect, 2);
            else cost.add(primals(1));
            return cost;
        }
    }

    static void register(DeferredRegister.Blocks blocks, DeferredRegister.Items items) {
        for (Crystal type : Crystal.values()) {
            items.registerSimpleBlockItem(blocks.registerBlock(type.block(), CrystalBlock::new, CrystalBuilding::crystalProperties));
            items.registerSimpleBlockItem(blocks.registerBlock(type.slab(), CrystalSlab::new, CrystalBuilding::crystalProperties));
        }
        items.registerSimpleBlockItem(blocks.registerSimpleBlock("gold_etched_obsidian",
            p -> p.strength(3, 6).requiresCorrectToolForDrops()));
        items.registerSimpleBlockItem(blocks.registerSimpleBlock("iron_greatwood",
            p -> p.strength(1).sound(SoundType.WOOD)));
        for (String parent : LEGACY) {
            items.registerSimpleBlockItem(blocks.registerBlock(legacySlab(parent), SlabBlock::new,
                p -> p.strength(3, 6).requiresCorrectToolForDrops()));
        }
    }

    private static BlockBehaviour.Properties crystalProperties(BlockBehaviour.Properties properties) {
        return properties.strength(1.5F).sound(SoundType.GLASS).requiresCorrectToolForDrops();
    }

    private static final class CrystalBlock extends Block implements InfusionStabiliser {
        CrystalBlock(Properties properties) {
            super(properties);
        }

        @Override
        public boolean canStabiliseInfusion(Level level, BlockPos pos) {
            return true;
        }
    }

    private static final class CrystalSlab extends SlabBlock implements InfusionStabiliser {
        CrystalSlab(Properties properties) {
            super(properties);
        }

        @Override
        public boolean canStabiliseInfusion(Level level, BlockPos pos) {
            return true;
        }
    }

    static void research() {
        restoration();
        var pages = new ArrayList<ResearchPage>();
        pages.add(ResearchPage.text("tc.research_page.TB.CrystalBlocks.1"));
        for (Crystal type : Crystal.values()) {
            if (type != Crystal.TAINTED) {
                String tainted = "tainted_crystal_from_" + type.name;
                TcRecipes.register(new CrucibleRecipe(id(tainted), RESEARCH, TcStack.of(id(Crystal.TAINTED.block())),
                    TcIngredient.item(type.catalyst()), type.cost().add(Aspect.TAINT, 2)));
                pages.add(ResearchPage.crucible(id(tainted)));
                TcRecipes.register(new CrucibleRecipe(id(type.block()), RESEARCH, TcStack.of(id(type.block())),
                    TcIngredient.item(type.catalyst()), type.cost()));
                pages.add(ResearchPage.crucible(id(type.block())));
            }
            pages.add(slabRecipe(type.slab(), RESEARCH));
        }
        ResearchCategories.addResearch(new BasesResearch(RESEARCH, Bases.ID, primals(4), 3, 0, 1,
            TcStack.of(id(Crystal.MIXED.block()))).setParents("TB.AlchemyRestoration").setPages(pages.toArray(ResearchPage[]::new)));

        String legacy = "TB.LegacySlabs";
        var legacyPages = new ArrayList<ResearchPage>();
        legacyPages.add(ResearchPage.text("tc.research_page.TB.LegacySlabs.1"));
        for (String name : List.of("gold_etched_obsidian", "iron_greatwood")) {
            int count = 5;
            TcRecipes.register(new CraftingRecipeRef(id(name), legacy, Bases.id(name), TcStack.of(id(name), count)));
            legacyPages.add(ResearchPage.crafting(id(name)));
        }
        for (String parent : LEGACY) legacyPages.add(slabRecipe(legacySlab(parent), legacy));
        ResearchCategories.addResearch(new BasesResearch(legacy, Bases.ID, new AspectList().add(Aspect.CRAFT, 2).add(Aspect.EARTH, 2),
            6, 0, 1, TcStack.of(id("old_brick_slab"))).setParents(RESEARCH).setPages(legacyPages.toArray(ResearchPage[]::new)));
    }

    private static void restoration() {
        String research = "TB.AlchemyRestoration";
        var bricks = restore("restore_stone_bricks", research, "minecraft:chiseled_stone_bricks", TcIngredient.tag("minecraft:stone_bricks"), Aspect.ORDER);
        var cobblestone = restore("restore_cobblestone", research, "minecraft:cobblestone",
            TcIngredient.item("minecraft:gravel"), Aspect.ORDER);
        var ice = restore("restore_packed_ice", research, "minecraft:packed_ice",
            TcIngredient.item("minecraft:ice"), Aspect.EARTH);
        ResearchCategories.addResearch(new BasesResearch(research, Bases.ID,
            new AspectList().add(Aspect.ORDER, 3).add(Aspect.MAGIC, 3).add(Aspect.CRAFT, 3),
            1, -2, 1, TcStack.of("minecraft:cobblestone")).setParents("CRUCIBLE")
            .setPages(ResearchPage.text("tc.research_page." + research + ".1"), bricks, cobblestone, ice));
    }

    private static ResearchPage restore(String name, String research, String output, TcIngredient catalyst, Aspect aspect) {
        TcRecipes.register(new CrucibleRecipe(id(name), research, TcStack.of(output), catalyst, new AspectList().add(aspect, 1)));
        return ResearchPage.crucible(id(name));
    }

    private static ResearchPage slabRecipe(String name, String research) {
        TcRecipes.register(new CraftingRecipeRef(id(name), research, Bases.id(name), TcStack.of(id(name), 3)));
        return ResearchPage.crafting(id(name));
    }

    private static AspectList primals(int amount) {
        var list = new AspectList();
        for (Aspect aspect : Aspect.getPrimalAspects()) list.add(aspect, amount);
        return list;
    }

    private static String legacySlab(String parent) {
        return parent.replace("_block", "").replace("bricks", "brick") + "_slab";
    }

    private static String id(String name) {
        return Bases.id(name).toString();
    }
}
