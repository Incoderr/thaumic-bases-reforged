package tb;

import java.util.Arrays;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.CrucibleRecipe;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.ShapelessArcaneRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.registry.ModEntities;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchManager;
import thaumcraft.research.ResearchPage;
import thaumcraft.research.Warp;
import thaumcraft.research.WarpType;

enum TobaccoBlend {
    PLAIN("pile", ""),
    ANGRY("fighting", "Angry"),
    MINING("mining", "Mining"),
    WISDOM("knowledge", "Wisdom"),
    TAINTED("tainted", "Tainted"),
    SANITY("sanity", "Sanity"),
    SATURATING("hunger", "Saturating"),
    ELDRITCH("eldritch", "Eldritch"),
    WISPY("wispy", "Wispy");

    private final String name;
    private final String research;

    TobaccoBlend(String name, String suffix) {
        this.name = "tobacco_" + name;
        this.research = "TB.Tobacco." + suffix;
    }

    static final class BlendItem extends Item {
        final TobaccoBlend blend;

        BlendItem(Properties properties, TobaccoBlend blend) {
            super(properties);
            this.blend = blend;
        }
    }

    static void register(DeferredRegister.Items items) {
        for (var blend : values()) {
            items.registerItem(blend.name, p -> new BlendItem(p, blend));
        }
    }

    void apply(ServerPlayer player, boolean silverwood) {
        var random = player.getRandom();
        switch (this) {
            case PLAIN -> {
                if (random.nextFloat() < (silverwood ? 0.3F : 0.1F)) clearWarp(player);
            }
            case ANGRY -> {
                effect(player, MobEffects.STRENGTH, 8000, silverwood ? 1 : 0);
                if (!silverwood && random.nextFloat() < 0.1F) effect(player, MobEffects.WEAKNESS, 8000, 0);
            }
            case MINING -> {
                effect(player, MobEffects.HASTE, 8000, silverwood ? 1 : 0);
                if (!silverwood && random.nextFloat() < 0.3F) effect(player, MobEffects.HUNGER, 8000, 0);
            }
            case WISDOM -> {
                var aspects = List.of(Aspect.values());
                int rolls = silverwood ? 20 : 10;
                for (int i = 0; i < rolls; i++) {
                    ResearchManager.addAspectPool(player, aspects.get(random.nextInt(aspects.size())), 1);
                }
                if (!silverwood) Warp.add(player, 1, WarpType.TEMPORARY);
            }
            case TAINTED -> {
                Warp.add(player, 1 + random.nextInt(3), WarpType.TEMPORARY);
                if (random.nextFloat() < 0.4F) Warp.add(player, 1, WarpType.STICKY);
            }
            case SANITY -> {
                if (random.nextFloat() < (silverwood ? 0.6F : 0.4F)) clearWarp(player);
                if (silverwood && random.nextFloat() < 0.1F && Warp.get(player, WarpType.STICKY) > 0) {
                    Warp.add(player, -1, WarpType.STICKY);
                }
            }
            case SATURATING -> {
                player.getFoodData().eat(6, 0.5F);
                if (!silverwood && random.nextFloat() < 0.4F) effect(player, MobEffects.HUNGER, 200, 0);
            }
            case ELDRITCH -> {
                effect(player, MobEffects.DARKNESS, 2000, 0);
                if (!silverwood) Warp.add(player, 3, WarpType.TEMPORARY);
            }
            case WISPY -> {
                var level = player.level();
                var wisp = ModEntities.WISP.get().create(level, EntitySpawnReason.TRIGGERED);
                if (wisp != null) {
                    var primals = Aspect.getPrimalAspects();
                    wisp.setAspect(primals.get(random.nextInt(primals.size())));
                    wisp.setPos(player.getX(), player.getEyeY(), player.getZ());
                    level.addFreshEntity(wisp);
                }
            }
        }
    }

    private static void clearWarp(ServerPlayer player) {
        if (Warp.get(player, WarpType.TEMPORARY) > 0) Warp.add(player, -1, WarpType.TEMPORARY);
    }

    private static void effect(ServerPlayer player, Holder<MobEffect> effect, int duration, int amplifier) {
        player.addEffect(new MobEffectInstance(effect, duration, amplifier));
    }

    static void research() {
        ANGRY.crucible(-6, 6, new AspectList().add(Aspect.WEAPON, 4).add(Aspect.HUNGER, 4));
        MINING.crucible(-5, 7, new AspectList().add(Aspect.MINE, 4).add(Aspect.MOTION, 4));
        WISDOM.crucible(-4, 8, new AspectList().add(Aspect.MIND, 4).add(Aspect.AIR, 4));
        TAINTED.crucible(-3, 8, new AspectList().add(Aspect.TAINT, 4).add(Aspect.MIND, 4));
        SANITY.arcane(-2, 8, new AspectList().add(Aspect.ORDER, 10).add(Aspect.AIR, 10).add(Aspect.ENTROPY, 10));
        SATURATING.arcane(-1, 7, new AspectList().add(Aspect.EARTH, 10).add(Aspect.FIRE, 10).add(Aspect.WATER, 10), "minecraft:pumpkin_seeds");
        ELDRITCH.infusion(0, 6, new AspectList().add(Aspect.ELDRITCH, 4).add(Aspect.DEATH, 1),
            "minecraft:ghast_tear", "minecraft:feather");
        WISPY.infusion(-1, 5, new AspectList().add(Aspect.AURA, 4).add(Aspect.MAGIC, 6),
            "thaumcraft:salis_mundus", "thaumcraft:crystallized_essence");

        String pipe = Bases.id("silverwood_pipe").toString();
        String key = "TB.SilverwoodPipe";
        var cost = new AspectList().add(Aspect.AURA, 8).add(Aspect.ORDER, 32).add(Aspect.HEAL, 16);
        var salt = TcIngredient.item("thaumcraft:salis_mundus");
        var wood = TcIngredient.item("thaumcraft:silverwood_planks");
        TcRecipes.register(new InfusionRecipe(pipe, key, TcStack.of(pipe), 4, cost,
            TcIngredient.item(Bases.id("greatwood_pipe").toString()),
            List.of(salt, salt, salt, wood, wood, wood, wood, TcIngredient.item("thaumcraft:silverwood_sapling"))));
        ResearchCategories.addResearch(new BasesResearch(key, Bases.ID, cost, -1, 9, 2, TcStack.of(pipe))
            .setParents("TB.Tobacco", "INFUSION").setPages(ResearchPage.text("tc.research_page." + key + ".1"), ResearchPage.infusion(pipe)));
    }

    private void crucible(int x, int y, AspectList cost) {
        TcRecipes.register(new CrucibleRecipe(id(), research, TcStack.of(id()), base(), cost));
        entry(x, y, cost, ResearchPage.crucible(id()), "TB.Tobacco");
    }

    private void arcane(int x, int y, AspectList cost, String... extra) {
        var ingredients = new java.util.ArrayList<TcIngredient>();
        ingredients.add(base());
        for (String item : extra) ingredients.add(TcIngredient.item(item));
        TcRecipes.register(new ShapelessArcaneRecipe(id(), research, TcStack.of(id()), cost, ingredients));
        entry(x, y, cost, ResearchPage.arcane(id()), "TB.Tobacco");
    }

    private void infusion(int x, int y, AspectList cost, String... extra) {
        TcRecipes.register(new InfusionRecipe(id(), research, TcStack.of(id()), 1, cost, base(),
            Arrays.stream(extra).map(TcIngredient::item).toList()));
        entry(x, y, cost, ResearchPage.infusion(id()), "TB.Tobacco", "INFUSION");
    }

    private void entry(int x, int y, AspectList tags, ResearchPage recipe, String... parents) {
        ResearchCategories.addResearch(new BasesResearch(research, Bases.ID, tags, x, y, 2, TcStack.of(id()))
            .setParents(parents).setPages(ResearchPage.text("tc.research_page." + research + ".1"), recipe));
    }

    private String id() {
        return Bases.id(name).toString();
    }

    private static TcIngredient base() {
        return TcIngredient.item(PLAIN.id());
    }
}
