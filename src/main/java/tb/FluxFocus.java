package tb;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.fx.Fx;
import thaumcraft.item.AspectTooltips;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModItems;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;
import thaumcraft.research.Warp;
import thaumcraft.research.WarpType;
import thaumcraft.wand.WandItem;
import thaumcraft.wand.Wands;
import thaumcraft.wand.focus.FocusItem;
import thaumcraft.wand.focus.FocusUpgradeType;

final class FluxFocus extends FocusItem {
    static final String ID = "focus_flux_scrubber";
    private static final String KEY = "TB.Foci.Flux";
    static final FocusUpgradeType WARPING = upgrade(104, "warping", Aspect.ENTROPY, Aspect.FIRE, Aspect.AIR);
    static final FocusUpgradeType CRYSTALLIZATION = upgrade(105, "crystallization", Aspect.ORDER, Aspect.EARTH, Aspect.WATER);
    static final FocusUpgradeType CALMING = upgrade(106, "calming", Aspect.WATER, Aspect.ORDER, Aspect.EARTH);

    FluxFocus(Properties properties) { super(properties); }

    private static FocusUpgradeType upgrade(int id, String name, Aspect... aspects) {
        var cost = new AspectList();
        for (var aspect : aspects) cost.add(aspect, 16);
        String texture = name.equals("crystallization") ? "crystalization" : name;
        return new FocusUpgradeType(id, "tb_" + name, Bases.id("textures/foci/" + texture + ".png"), cost);
    }

    static void register(DeferredRegister.Items items) {
        items.registerItem(ID, FluxFocus::new, FocusItem::properties);
    }

    @Override
    public int getFocusColor(ItemStack stack) { return 6567837; }

    @Override
    public Identifier getFocusTexture(ItemStack stack) { return Bases.id("item/" + ID); }

    @Override
    public AspectList getVisCost(ItemStack stack) { return new AspectList().add(Aspect.ORDER, 5); }

    @Override
    public boolean isVisCostPerTick(ItemStack stack) { return true; }

    @Override
    public WandFocusAnimation getAnimation(ItemStack stack) { return WandFocusAnimation.CHARGE; }

    @Override
    public FocusUpgradeType[] getPossibleUpgradesByRank(ItemStack stack, int rank) {
        if (rank == 5) {
            if (isUpgradedWith(stack, WARPING)) return new FocusUpgradeType[] {FocusUpgradeType.POTENCY, FocusUpgradeType.FRUGAL, CALMING};
            if (isUpgradedWith(stack, CRYSTALLIZATION)) return new FocusUpgradeType[] {FocusUpgradeType.POTENCY, FocusUpgradeType.FRUGAL, CRYSTALLIZATION};
        }
        if (rank == 3 || rank == 5) return new FocusUpgradeType[] {FocusUpgradeType.POTENCY, FocusUpgradeType.FRUGAL, WARPING, CRYSTALLIZATION};
        return new FocusUpgradeType[] {FocusUpgradeType.POTENCY, FocusUpgradeType.FRUGAL};
    }

    @Override
    public InteractionResult onFocusRightClick(ItemStack wand, Level level, Player player, HitResult hit) {
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUsingFocusTick(ItemStack wand, Player player, int remaining) {
        if (!(wand.getItem() instanceof WandItem caster)) return;
        var level = player.level();
        var focus = Wands.getFocusItem(wand);
        if (!caster.consumeAllVis(wand, player, getVisCost(focus), !level.isClientSide(), false)) {
            player.stopUsingItem();
            return;
        }
        int potency = Math.min(5, Wands.getFocusPotency(wand));
        var target = findFlux(level, player);
        if (target == null) return;
        if (level.isClientSide()) {
            Fx.get().blockBubble(level, target, 0xFF00FF);
        } else if (player instanceof ServerPlayer serverPlayer && (potency == 5 || player.tickCount % 10 == potency * 2)) {
            clean(serverPlayer, focus, target);
        }
    }

    static BlockPos findFlux(Level level, Player player) {
        var eye = player.getEyePosition();
        var end = eye.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));
        var collision = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return BlockGetter.traverseBlocks(eye, collision.getLocation(), level,
            (world, pos) -> isFlux(world.getBlockState(pos)) ? pos.immutable() : null, world -> null);
    }
    private static boolean isFlux(BlockState state) {
        return state.is(ModBlocks.FLUX_GOO.get()) || state.is(ModBlocks.FLUX_GAS.get());
    }

    boolean clean(ServerPlayer player, ItemStack focus, BlockPos pos) {
        ServerLevel level = player.level();
        if (!level.mayInteract(player, pos) || !isFlux(level.getBlockState(pos)) || !level.removeBlock(pos, false)) return false;
        var random = level.getRandom();
        if (isUpgradedWith(focus, WARPING)) {
            if (isUpgradedWith(focus, CALMING)) {
                Warp.add(player, -1, WarpType.TEMPORARY);
                if (random.nextFloat() < 0.4F) Warp.add(player, -1, WarpType.STICKY);
            } else {
                Warp.add(player, 1, WarpType.TEMPORARY);
                if (random.nextFloat() < 0.1F) Warp.add(player, 1, WarpType.STICKY);
            }
        } else {
            int crystallization = getUpgradeLevel(focus, CRYSTALLIZATION);
            if (crystallization > 0) {
                var aspect = Aspect.ENERGY;
                if (crystallization > 1) {
                    var aspects = Aspect.values();
                    aspect = aspects[random.nextInt(aspects.length)];
                }
                var crystal = new ItemStack(ModItems.CRYSTALLIZED_ESSENCE.get());
                AspectTooltips.setSingle(crystal, aspect, 1);
                Block.popResource(level, pos, crystal);
            }
        }
        return true;
    }

    static void research() {
        String recipe = Bases.id(ID).toString();
        var result = TcStack.of(recipe);
        var shard = TcIngredient.item("thaumcraft:balanced_shard");
        var cost = new AspectList().add(Aspect.WATER, 16).add(Aspect.VOID, 16).add(Aspect.ORDER, 16);
        TcRecipes.register(new InfusionRecipe(recipe, KEY, result, 3, cost, TcIngredient.item("minecraft:diamond"),
            List.of(shard, shard, TcIngredient.item("thaumcraft:flux_scrubber"), TcIngredient.item("thaumcraft:void_jar"))));
        var tags = new AspectList().add(Aspect.WATER, 4).add(Aspect.ORDER, 4).add(Aspect.VOID, 4);
        ResearchCategories.addResearch(new BasesResearch(KEY, Bases.ID, tags, -9, -1, 1, result)
            .setParents("INFUSION", "JARVOID", "FLUXSCRUB")
            .setPages(ResearchPage.text("tc.research_page.TB.Foci.Flux.1"), ResearchPage.infusion(recipe),
                ResearchPage.text("tc.research_page.TB.Foci.Flux.2"), ResearchPage.text("tc.research_page.TB.Foci.Flux.3"),
                ResearchPage.text("tc.research_page.TB.Foci.Flux.4")));
    }
}
