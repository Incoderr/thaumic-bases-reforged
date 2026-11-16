package tb;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.item.AspectTooltips;
import thaumcraft.registry.ModItems;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;
import thaumcraft.wand.WandItem;
import thaumcraft.wand.Wands;
import thaumcraft.wand.focus.FocusItem;
import thaumcraft.wand.focus.FocusUpgradeType;

final class DrainFocus extends FocusItem {
    private static final String ID = "focus_drain";
    private static final String KEY = "TB.Foci.Drain";
    private static final String POCKET = "drained_fluid_pocket";
    private static final int POCKET_DURATION = 200;
    static final FocusUpgradeType AQUATIC = new FocusUpgradeType(100, "tb_aquatic",
        Bases.id("textures/foci/aquatic.png"), new AspectList().add(Aspect.WATER, 16).add(Aspect.ORDER, 16));
    static final FocusUpgradeType NETHERIC = new FocusUpgradeType(101, "tb_netheric",
        Bases.id("textures/foci/netheric.png"), new AspectList().add(Aspect.FIRE, 16).add(Aspect.ENTROPY, 16));

    DrainFocus(Properties properties) { super(properties); }

    static void register(DeferredRegister.Blocks blocks, DeferredRegister.Items items) {
        blocks.registerBlock(POCKET, Pocket::new, p -> p.noCollision().noOcclusion().noLootTable().strength(-1));
        items.registerItem(ID, DrainFocus::new, FocusItem::properties);
    }

    @Override
    public int getFocusColor(ItemStack focus) { return 0x5555FF; }

    @Override
    public Identifier getFocusTexture(ItemStack focus) { return Bases.id("item/focus_drain"); }

    @Override
    public AspectList getVisCost(ItemStack focus) { return new AspectList().add(Aspect.WATER, 5); }

    @Override
    public boolean isVisCostPerTick(ItemStack focus) { return true; }

    @Override
    public WandFocusAnimation getAnimation(ItemStack focus) { return WandFocusAnimation.CHARGE; }

    @Override
    public FocusUpgradeType[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
        return rank == 3
            ? new FocusUpgradeType[] { FocusUpgradeType.FRUGAL, AQUATIC, NETHERIC }
            : new FocusUpgradeType[] { FocusUpgradeType.FRUGAL };
    }

    @Override
    public InteractionResult onFocusRightClick(ItemStack wand, Level level, Player player, HitResult hit) {
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUsingFocusTick(ItemStack wand, Player player, int remaining) {
        if (!(wand.getItem() instanceof WandItem caster)) return;
        var focus = Wands.getFocusItem(wand);
        var cost = getVisCost(focus);
        if (!caster.consumeAllVis(wand, player, cost, !player.level().isClientSide(), false)) {
            player.stopUsingItem();
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) return;
        var eye = player.getEyePosition();
        var end = eye.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));
        var hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));
        if (hit.getType() == HitResult.Type.BLOCK) drain(wand, caster, player, level, hit.getBlockPos(), remaining);
    }

    boolean drain(ItemStack wand, WandItem caster, Player player, ServerLevel level, BlockPos pos, int remaining) {
        if (!level.mayInteract(player, pos)) return false;
        var state = level.getBlockState(pos);
        boolean lava = state.is(Blocks.LAVA);
        var focus = Wands.getFocusItem(wand);
        boolean aquatic = isUpgradedWith(focus, AQUATIC);
        if (!state.is(Blocks.WATER) && !(lava && isUpgradedWith(focus, NETHERIC))) return false;
        if ((lava || !aquatic) && remaining % 5 != 0) return false;
        var cost = new AspectList().add(Aspect.FIRE, 3);
        if (lava && !caster.consumeAllVis(wand, player, cost, false, false)) return false;
        var pocket = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(Bases.id(POCKET));
        if (!level.setBlockAndUpdate(pos, pocket.defaultBlockState())) return false;
        if (lava) caster.consumeAllVis(wand, player, cost, true, false);
        level.scheduleTick(pos, pocket, POCKET_DURATION);
        if (!lava && aquatic && state.getFluidState().isSource() && level.getRandom().nextFloat() < 0.4F) {
            var crystal = new ItemStack(ModItems.CRYSTALLIZED_ESSENCE.get());
            AspectTooltips.setSingle(crystal, level.getRandom().nextFloat() < 0.1F ? Aspect.POISON : Aspect.WATER, 1);
            Block.popResource(level, pos, crystal);
        }
        return true;
    }

    static final class Pocket extends Block {
        Pocket(Properties properties) { super(properties); }

        @Override
        protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) { return true; }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                         BlockPos pos, Direction direction, BlockPos neighborPos,
                                         BlockState neighbor, RandomSource random) {
            return neighbor.isSolid() ? Blocks.AIR.defaultBlockState() : state;
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            level.removeBlock(pos, false);
        }
    }

    static void research() {
        String id = Bases.id(ID).toString();
        var result = TcStack.of(id);
        var bucket = TcIngredient.item("minecraft:bucket");
        var jar = TcIngredient.item("thaumcraft:void_jar");
        TcRecipes.register(new InfusionRecipe(id, KEY, result, 0, new AspectList().add(Aspect.WATER, 3).add(Aspect.VOID, 6),
            TcIngredient.item("thaumcraft:focus_excavation"), List.of(bucket, bucket, jar, jar)));
        ResearchCategories.addResearch(new BasesResearch(KEY, Bases.ID, new AspectList().add(Aspect.WATER, 6).add(Aspect.VOID, 3),
            -9, -2, 1, result).setParents("INFUSION", "FOCUSEXCAVATION", "JARVOID")
            .setPages(ResearchPage.text("tc.research_page.TB.Foci.Drain.1"), ResearchPage.infusion(id),
                ResearchPage.text("tc.research_page.TB.Foci.Drain.2")));
    }
}
