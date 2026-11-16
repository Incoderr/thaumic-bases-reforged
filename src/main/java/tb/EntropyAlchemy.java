package tb;

import java.util.ArrayList;
import java.util.List;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.CrucibleRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;

final class EntropyAlchemy {
    static final String ADVANCED = "TB.AdvancedEntropy";
    static final String MASTER = "TB.MasterEntropy";
    static final String ORES = "TB.OreDestruction";
    private static final List<Material> MATERIALS = List.of(
        new Material("wool", TcIngredient.tag("minecraft:wool"), "minecraft:string"),
        new Material("sandstone", TcIngredient.item("minecraft:sandstone"), "minecraft:sand"),
        new Material("blaze_rod", TcIngredient.item("minecraft:blaze_rod"), "minecraft:blaze_powder")
    );
    private static final List<String> PRIMAL_NAMES = List.of("air", "fire", "water", "earth", "order", "entropy");
    private static final List<Aspect> PRIMALS = List.of(Aspect.AIR, Aspect.FIRE, Aspect.WATER, Aspect.EARTH, Aspect.ORDER, Aspect.ENTROPY);

    private record Material(String name, TcIngredient input, String result) {}

    static void research() {
        breakdown(ADVANCED, "CRUCIBLE", -1, -3, 1, 3,
            new AspectList().add(Aspect.ENTROPY, 3).add(Aspect.MAGIC, 3).add(Aspect.CRAFT, 3));
        breakdown(MASTER, ADVANCED, -2, -4, 3, 4,
            new AspectList().add(Aspect.ENTROPY, 8).add(Aspect.MAGIC, 4).add(Aspect.GREED, 2));

        String strong = "TB.StrongEntropy";
        var destructionCost = new AspectList().add(Aspect.ENTROPY, 1);
        var sand = TcStack.of("minecraft:sand");
        var glassPage = recipe(strong, "sand_from_glass", TcIngredient.tag("c:glass_blocks"), sand, destructionCost.copy());
        var gravelPage = recipe(strong, "sand_from_gravel", TcIngredient.item("minecraft:gravel"), sand, destructionCost.copy());
        var barsPage = recipe(strong, "nuggets_from_iron_bars", TcIngredient.item("minecraft:iron_bars"),
            TcStack.of("minecraft:iron_nugget", 3), destructionCost.copy());
        var strongResearch = new AspectList().add(Aspect.ENTROPY, 2).add(Aspect.WEAPON, 2);
        ResearchCategories.addResearch(new BasesResearch(strong, Bases.ID, strongResearch, -1, -4, 3, sand)
            .setParents(ADVANCED).setPages(ResearchPage.text("tc.research_page.TB.StrongEntropy.1"), glassPage, gravelPage, barsPage));

        var pages = new ArrayList<ResearchPage>();
        pages.add(ResearchPage.text("tc.research_page." + ORES + ".1"));
        for (int i = 0; i < PRIMAL_NAMES.size(); i++) {
            String primal = PRIMAL_NAMES.get(i);
            var cost = new AspectList().add(Aspect.ENTROPY, 2).add(Aspect.MAGIC, 1).add(PRIMALS.get(i), 3);
            pages.add(recipe(ORES, "shards_from_" + primal + "_ore", TcIngredient.item("thaumcraft:" + primal + "_infused_stone"),
                TcStack.of("thaumcraft:" + primal + "_shard", 8), cost));
        }
        var research = new AspectList().add(Aspect.ENTROPY, 8).add(Aspect.MAGIC, 4);
        for (var primal : PRIMALS) if (primal != Aspect.ENTROPY) research.add(primal, 1);
        ResearchCategories.addResearch(new BasesResearch(ORES, Bases.ID, research, -4, -4, 3, TcStack.of("thaumcraft:air_shard"))
            .setParents(MASTER).setPages(pages.toArray(ResearchPage[]::new)));
    }

    private static void breakdown(String key, String parent, int x, int y, int complexity, int count, AspectList research) {
        var pages = new ArrayList<ResearchPage>();
        pages.add(ResearchPage.text("tc.research_page." + key + ".1"));
        boolean master = key.equals(MASTER);
        for (var material : MATERIALS) {
            var cost = new AspectList().add(Aspect.ENTROPY, 1);
            if (master) cost.add(Aspect.CRAFT, 1);
            pages.add(recipe(key, (master ? "master_breakdown_" : "breakdown_") + material.name(),
                material.input(), TcStack.of(material.result(), count), cost));
        }
        ResearchCategories.addResearch(new BasesResearch(key, Bases.ID, research, x, y, complexity, TcStack.of("minecraft:blaze_powder"))
            .setParents(parent).setPages(pages.toArray(ResearchPage[]::new)));
    }

    private static ResearchPage recipe(String research, String name, TcIngredient input, TcStack result, AspectList cost) {
        String id = Bases.id(name).toString();
        TcRecipes.register(new CrucibleRecipe(id, research, result, input, cost));
        return ResearchPage.crucible(id);
    }
}
