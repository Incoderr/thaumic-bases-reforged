package tb;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.effect.EffectHooks;
import thaumcraft.entity.EldritchMob;
import thaumcraft.entity.TaintedMob;
import thaumcraft.entity.boss.ThaumcraftBoss;
import thaumcraft.research.ResearchManager;
import thaumcraft.research.ScanManager;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.infusion.InfusionKeys;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;

enum RevolverUpgrade {
    ACCURACY(3, 1, new AspectList().add(Aspect.ORDER, 8).add(Aspect.SENSES, 8),
        "minecraft:white_stained_glass", "minecraft:white_stained_glass",
        "minecraft:white_stained_glass", "minecraft:white_stained_glass",
        "minecraft:carrot", "thaumcraft:salis_mundus"),
    SPEED(5, 1, new AspectList().add(Aspect.MOTION, 8).add(Aspect.AIR, 8),
        "thaumcraft:salis_mundus", "thaumcraft:salis_mundus", "minecraft:feather"),
    EFFICIENCY(5, 1, new AspectList().add(Aspect.ORDER, 8).add(Aspect.EXCHANGE, 8),
        "thaumcraft:salis_mundus", "thaumcraft:salis_mundus", "thaumcraft:salis_mundus",
        "minecraft:blaze_powder", "minecraft:enchanted_book"),
    POWER(5, 2, new AspectList().add(Aspect.WEAPON, 16),
        "minecraft:gunpowder", "thaumcraft:salis_mundus", "thaumcraft:salis_mundus",
        "minecraft:fire_charge", "minecraft:firework_rocket"),
    HEAVY(5, 0, new AspectList().add(Aspect.WEAPON, 8).add(Aspect.METAL, 8),
        "thaumcraft:salis_mundus", "minecraft:iron_sword", "thaumcraft:salis_mundus"),
    VOID(3, 4, new AspectList().add(Aspect.VOID, 8).add(Aspect.TOOL, 8),
        "thaumcraft:void_metal_ingot", "thaumcraft:void_metal_ingot", "thaumcraft:void_metal_ingot",
        "thaumcraft:void_metal_ingot", "thaumcraft:void_metal_ingot"),
    SILVER(5, 0, new AspectList().add(Aspect.DEATH, 8).add(Aspect.BEAST, 8).add(Aspect.UNDEAD, 8),
        "thaumcraft:salis_mundus", "thaumcraft:quicksilver", "thaumcraft:primal_charm",
        "thaumcraft:quicksilver", "thaumcraft:salis_mundus"),
    ARTHROPODS(5, 1, new AspectList().add(Aspect.DEATH, 8).add(Aspect.BEAST, 8).add(Aspect.CLOTH, 8),
        "minecraft:spider_eye", "minecraft:fermented_spider_eye", "minecraft:spider_eye", "thaumcraft:salis_mundus"),
    ELDRITCH_BANE(5, 2, new AspectList().add(Aspect.DEATH, 8).add(Aspect.ELDRITCH, 8),
        "minecraft:ender_eye", "thaumcraft:salis_mundus", "minecraft:ender_pearl"),
    DUELING(5, 2, new AspectList().add(Aspect.DEATH, 8).add(Aspect.MAN, 8),
        "thaumcraft:salis_mundus", "minecraft:red_bed", "thaumcraft:salis_mundus",
        "minecraft:ghast_tear", "thaumcraft:salis_mundus"),
    ELDRITCH(2, 5, new AspectList().add(Aspect.ELDRITCH, 8).add(Aspect.VOID, 8),
        "thaumcraft:eldritch_eye", "thaumcraft:salis_mundus", "thaumcraft:eldritch_eye", "thaumcraft:salis_mundus"),
    KNOWLEDGE(5, 0, new AspectList().add(Aspect.MIND, 8).add(Aspect.AURA, 8),
        "thaumcraft:scribing_tools", "thaumcraft:salis_mundus", "minecraft:book",
        "thaumcraft:salis_mundus", "minecraft:paper"),
    TAINTED(3, 1, new AspectList().add(Aspect.TAINT, 8).add(Aspect.WEAPON, 8),
        "thaumcraft:tainted_goo", "thaumcraft:salis_mundus", "thaumcraft:taint_tendril", "thaumcraft:salis_mundus"),
    PIERCING(1, 12, new AspectList().add(Aspect.WEAPON, 32).add(Aspect.AIR, 64),
        "thaumcraft:air_arrow", "thaumcraft:fire_arrow", "thaumcraft:water_arrow", "minecraft:diamond_sword",
        "thaumcraft:earth_arrow", "thaumcraft:order_arrow", "thaumcraft:entropy_arrow"),
    PRIMAL(1, 12, primalCost(),
        "thaumcraft:salis_mundus", "thaumcraft:primordial_pearl", "minecraft:diamond",
        "thaumcraft:primordial_pearl", "thaumcraft:salis_mundus");

    private static final String INPUT = "tb_revolver_input";
    private static final String PREFIX = "tb_revolver_";
    private static final List<RevolverUpgrade> HANDLING = List.of(ACCURACY, SPEED, EFFICIENCY);
    private static final List<RevolverUpgrade> DAMAGE = List.of(POWER, SILVER, ARTHROPODS, ELDRITCH_BANE, DUELING, ELDRITCH);
    private static final List<RevolverUpgrade> INFUSED = List.of(KNOWLEDGE, TAINTED, PRIMAL);
    private static final TagKey<net.minecraft.world.entity.EntityType<?>> BOSSES =
        TagKey.create(Registries.ENTITY_TYPE, net.minecraft.resources.ResourceLocation.parse("c:bosses"));
    private final int maxLevel;
    private final int instability;
    private final AspectList cost;
    private final List<TcIngredient> components;

    RevolverUpgrade(int maxLevel, int instability, AspectList cost, String... components) {
        this.maxLevel = maxLevel;
        this.instability = instability;
        this.cost = cost;
        this.components = Arrays.stream(components).map(TcIngredient::item).toList();
    }

    private static AspectList primalCost() {
        var aspects = new AspectList().add(Aspect.WEAPON, 64).add(Aspect.MAGIC, 64);
        for (var primal : Aspect.getPrimalAspects()) aspects.add(primal, 64);
        return aspects;
    }

    private String id() { return name().toLowerCase(Locale.ROOT); }

    String translationKey() { return "item.thaumicbases.revolver.upgrade." + id(); }

    int level(ItemStack stack) {
        return Integer.parseInt(InfusionKeys.TAGS.getOrDefault(stack, Map.of()).getOrDefault(PREFIX + id(), "0"));
    }

    private boolean matches(ItemStack stack, int previousLevel) {
        if (level(stack) != previousLevel) return false;
        var group = HANDLING.contains(this) ? HANDLING : DAMAGE.contains(this) ? DAMAGE : INFUSED;
        return !group.contains(this) || group.stream().noneMatch(other -> other != this && other.level(stack) > 0);
    }

    static double damageMultiplier(ItemStack stack, Entity target) {
        int tainted = TAINTED.level(stack);
        double taintFactor = tainted == 0 ? 1.0
            : target instanceof TaintedMob ? 1.0 / 1.6 : Math.pow(1.2, tainted);
        double piercingFactor = PIERCING.level(stack) > 0 ? 1.1 : 1.0;
        double primalFactor = PRIMAL.level(stack) > 0 ? 2.0 : 1.0;
        return taintFactor * piercingFactor * primalFactor * damageSpecialization(stack, target);
    }

    private static double damageSpecialization(ItemStack stack, Entity target) {
        var type = target.getType();
        if (type.builtInRegistryHolder().is(EntityTypeTags.UNDEAD) && SILVER.level(stack) > 0)
            return Math.pow(1.2, SILVER.level(stack));
        if (type.builtInRegistryHolder().is(EntityTypeTags.ARTHROPOD) && ARTHROPODS.level(stack) > 0)
            return Math.pow(1.2, ARTHROPODS.level(stack));
        if ((target instanceof EldritchMob || type == EntityType.ENDERMAN) && ELDRITCH_BANE.level(stack) > 0)
            return Math.pow(1.2, ELDRITCH_BANE.level(stack));
        if (target instanceof Player && DUELING.level(stack) > 0)
            return Math.pow(1.2, DUELING.level(stack));
        int eldritch = ELDRITCH.level(stack);
        if (eldritch > 0) {
            boolean boss = type.builtInRegistryHolder().is(BOSSES) || target instanceof ThaumcraftBoss;
            return boss ? Math.pow(1.3, eldritch) : 1.0 / 1.2;
        }
        return Math.pow(1.1, POWER.level(stack));
    }

    static void afterHit(ItemStack stack, LivingEntity target, Entity owner) {
        if (TAINTED.level(stack) > 0 && target.isAlive() && target.getRandom().nextDouble() < 0.1) {
            EffectHooks.applyTaintPoison(target, 200, 1);
        }
        int knowledge = KNOWLEDGE.level(stack);
        if (knowledge == 0 || target.isAlive() || !(owner instanceof ServerPlayer player)
            || player.getRandom().nextInt(7 - knowledge) != 0) return;
        var aspects = ScanManager.entityAspects(target);
        if (aspects == null) return;
        for (var aspect : aspects.getAspects()) {
            ResearchManager.addAspectPool(player, aspect, 1);
            if (player.getRandom().nextBoolean()) break;
        }
    }

    private record Layout(String suffix, int column, int row) {}

    private Layout layout() {
        return switch (this) {
            case ACCURACY -> new Layout("Accuracy", -9, -19);
            case SPEED -> new Layout("Speed", -5, -24);
            case EFFICIENCY -> new Layout("Efficiency", -9, -22);
            case POWER -> new Layout("Power", -7, -24);
            case HEAVY -> new Layout("Heavy", -9, -23);
            case VOID -> new Layout("Void", -5, -25);
            case SILVER -> new Layout("Silver", -6, -24);
            case ARTHROPODS -> new Layout("Atropods", -9, -20);
            case ELDRITCH_BANE -> new Layout("EldritchBane", -10, -20);
            case DUELING -> new Layout("Dueling", -9, -21);
            case ELDRITCH -> new Layout("Eldritch", -10, -22);
            case KNOWLEDGE -> new Layout("Knowledge", -8, -24);
            case TAINTED -> new Layout("Tainted", -4, -24);
            case PIERCING -> new Layout("Piercing", -7, -25);
            case PRIMAL -> new Layout("Primal", -10, -25);
        };
    }

    static void registerResearch(String revolverResearch, String item) {
        TcStack.registerDataMatcher(INPUT, (stack, value) -> {
            var parts = value.split("/", 2);
            return valueOf(parts[0]).matches(stack, Integer.parseInt(parts[1]));
        });
        for (var upgrade : values()) {
            var layout = upgrade.layout();
            String research = revolverResearch + "." + layout.suffix();
            var pages = new java.util.ArrayList<ResearchPage>();
            pages.add(ResearchPage.text(upgrade.translationKey() + ".description"));
            for (int level = 1; level <= upgrade.maxLevel; level++) {
                String recipe = Bases.id("revolver_" + upgrade.id() + "_" + level).toString();
                var input = TcIngredient.of(TcStack.withData(item, 1, Map.of(INPUT, upgrade.name() + "/" + (level - 1))));
                var result = TcStack.withData(item, 1, Map.of(PREFIX + upgrade.id(), Integer.toString(level)));
                var aspects = new AspectList();
                for (var aspect : upgrade.cost.getAspects()) {
                    aspects.add(aspect, upgrade.cost.getAmount(aspect) * level);
                }
                TcRecipes.register(new InfusionRecipe(recipe, research, result, upgrade.instability + level - 1,
                    aspects, input, upgrade.components));
                pages.add(ResearchPage.infusion(recipe));
            }

            var tags = new AspectList();
            for (var aspect : upgrade.cost.getAspects()) {
                tags.add(aspect, Math.max(1, Math.min(8, upgrade.cost.getAmount(aspect) / 4)));
            }
            var entry = new BasesResearch(research, Bases.ID, tags, layout.column(), layout.row(), 1,
                Bases.id("textures/thaumonomicon/revolver/" + upgrade.id() + ".png"))
                .setParents(revolverResearch).setConcealed().setPages(pages.toArray(ResearchPage[]::new));
            if (upgrade == PRIMAL) entry.setSpecial();
            ResearchCategories.addResearch(entry);
        }
    }
}
