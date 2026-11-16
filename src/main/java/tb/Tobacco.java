package tb;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.CrucibleRecipe;
import thaumcraft.crafting.ShapedArcaneRecipe;
import thaumcraft.crafting.ShapelessArcaneRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;

final class Tobacco {
    private static final String RESEARCH = "TB.Tobacco";

    static void register(DeferredRegister.Items items) {
        items.registerSimpleItem("tobacco_leaves");
        TobaccoBlend.register(items);
        items.registerItem("mortar", Mortar::new, p -> p.durability(256));
        items.registerItem("greatwood_pipe", p -> new Pipe(p, false), p -> p.stacksTo(1));
        items.registerItem("silverwood_pipe", p -> new Pipe(p, true), p -> p.stacksTo(1));
    }

    private static final class Mortar extends Item {
        Mortar(Properties properties) {
            super(properties);
        }

        @Override
        public ItemStackTemplate getCraftingRemainder(ItemInstance item) {
            var stack = item instanceof ItemStack original ? original : new ItemStack(this);
            int damage = stack.getDamageValue() + 1;
            if (damage >= stack.getMaxDamage()) return null;
            var remainder = stack.copyWithCount(1);
            remainder.setDamageValue(damage);
            return ItemStackTemplate.fromNonEmptyStack(remainder);
        }
    }

    private static final class Pipe extends Item {
        private final boolean silverwood;

        Pipe(Properties properties, boolean silverwood) {
            super(properties);
            this.silverwood = silverwood;
        }

        private static ItemStack tobacco(Player player) {
            for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
                if (stack.getItem() instanceof TobaccoBlend.BlendItem) return stack;
            }
            var offhand = player.getOffhandItem();
            return offhand.getItem() instanceof TobaccoBlend.BlendItem ? offhand : ItemStack.EMPTY;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (tobacco(player).isEmpty()) return InteractionResult.PASS;
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }

        @Override
        public int getUseDuration(ItemStack stack, LivingEntity user) {
            return 64;
        }

        @Override
        public ItemUseAnimation getUseAnimation(ItemStack stack) {
            return ItemUseAnimation.TOOT_HORN;
        }

        @Override
        public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
            if (level instanceof ServerLevel server && remaining <= 32 && remaining % 8 == 0) {
                smoke(server, user, 2);
            }
        }

        private void smoke(ServerLevel level, LivingEntity user, int count) {
            var origin = user.getEyePosition().add(user.getLookAngle().scale(0.3)).add(0, -0.15, 0);
            level.sendParticles(silverwood ? ParticleTypes.POOF : ParticleTypes.SMOKE,
                origin.x, origin.y, origin.z, count, 0.04, 0.04, 0.04, 0.02);
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
            if (level instanceof ServerLevel server && user instanceof ServerPlayer player) {
                var pile = tobacco(player);
                if (pile.getItem() instanceof TobaccoBlend.BlendItem tobacco) {
                    pile.consume(1, player);
                    tobacco.blend.apply(player, silverwood);
                    smoke(server, player, 18);
                }
            }
            return stack;
        }
    }

    static void research() {
        String seeds = id("tobacco_seeds");
        String mortar = id("mortar");
        String pipe = id("greatwood_pipe");
        String pile = id("tobacco_pile");
        var vis = new AspectList();
        for (Aspect aspect : Aspect.getPrimalAspects()) vis.add(aspect, 4);
        TcRecipes.register(new CrucibleRecipe(seeds, RESEARCH, TcStack.of(seeds), TcIngredient.item("minecraft:wheat_seeds"),
            new AspectList().add(Aspect.MIND, 4).add(Aspect.WATER, 4).add(Aspect.MAN, 4).add(Aspect.CROP, 4)));
        var slab = TcIngredient.item("minecraft:smooth_stone_slab");
        TcRecipes.register(new ShapelessArcaneRecipe(mortar, RESEARCH, TcStack.of(mortar), vis,
            List.of(TcIngredient.item("minecraft:bowl"), TcIngredient.item("minecraft:stick"), slab, slab, slab)));
        var wood = TcIngredient.item("thaumcraft:greatwood_planks");
        TcRecipes.register(new ShapedArcaneRecipe(pipe, RESEARCH, TcStack.of(pipe), vis.copy(), 3, 2,
            List.of(wood, TcIngredient.item("thaumcraft:salis_mundus"), TcIngredient.EMPTY,
                TcIngredient.EMPTY, wood, wood)));
        TcRecipes.register(new ShapelessArcaneRecipe(pile, RESEARCH, TcStack.of(pile),
            new AspectList().add(Aspect.ORDER, 1).add(Aspect.ENTROPY, 1),
            List.of(TcIngredient.item(mortar), TcIngredient.item(id("tobacco_leaves")))));
        ResearchCategories.addResearch(new BasesResearch(RESEARCH, Bases.ID,
            new AspectList().add(Aspect.CROP, 6).add(Aspect.HARVEST, 6).add(Aspect.MAN, 6).add(Aspect.MIND, 6),
            -3, 6, 2, TcStack.of(seeds)).setParents("TB.Plax")
            .setPages(ResearchPage.text("tc.research_page.TB.Tobacco.1"), ResearchPage.crucible(seeds),
                ResearchPage.arcane(mortar), ResearchPage.arcane(pile), ResearchPage.arcane(pipe)));
        TobaccoBlend.research();
    }

    private static String id(String name) {
        return Bases.id(name).toString();
    }
}
