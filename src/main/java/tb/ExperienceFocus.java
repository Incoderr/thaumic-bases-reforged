package tb;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectHelper;
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
import thaumcraft.wand.focus.FocusUtils;

final class ExperienceFocus extends FocusItem {
    static final String ID = "focus_experience";
    private static final String KEY = "TB.Foci.Experience";
    private static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE,
        Identifier.fromNamespaceAndPath("c", "bosses"));
    static final FocusUpgradeType DECOMPOSING = new FocusUpgradeType(102, "tb_decomposing",
        Bases.id("textures/foci/decomposing.png"), new AspectList().add(Aspect.ENTROPY, 16).add(Aspect.EARTH, 16));
    static final FocusUpgradeType VAPORIZING = new FocusUpgradeType(103, "tb_vaporizing",
        Bases.id("textures/foci/vaporizing.png"), new AspectList().add(Aspect.ENTROPY, 16).add(Aspect.ORDER, 16).add(Aspect.AIR, 16));

    ExperienceFocus(Properties properties) { super(properties); }

    static void register(DeferredRegister.Items items) {
        items.registerItem(ID, ExperienceFocus::new, FocusItem::properties);
    }

    @Override
    public int getFocusColor(ItemStack focus) { return 16318428; }

    @Override
    public Identifier getFocusTexture(ItemStack focus) { return Bases.id("item/focus_experience"); }

    @Override
    public WandFocusAnimation getAnimation(ItemStack focus) { return WandFocusAnimation.CHARGE; }

    @Override
    public void appendCostText(ItemStack stack, java.util.function.Consumer<net.minecraft.network.chat.Component> builder) {
        builder.accept(net.minecraft.network.chat.Component.translatable("item.thaumicbases.focus_experience.cost"));
    }

    @Override
    public FocusUpgradeType[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
        if (rank == 3 || rank == 5) {
            if (isUpgradedWith(focus, DECOMPOSING)) return new FocusUpgradeType[] { FocusUpgradeType.POTENCY, FocusUpgradeType.FRUGAL, DECOMPOSING };
            if (isUpgradedWith(focus, VAPORIZING)) return new FocusUpgradeType[] { FocusUpgradeType.POTENCY, FocusUpgradeType.FRUGAL, VAPORIZING };
            return new FocusUpgradeType[] { FocusUpgradeType.POTENCY, FocusUpgradeType.FRUGAL, DECOMPOSING, VAPORIZING };
        }
        return new FocusUpgradeType[] { FocusUpgradeType.POTENCY, FocusUpgradeType.FRUGAL };
    }

    @Override
    public InteractionResult onFocusRightClick(ItemStack wand, Level level, Player player, HitResult ignored) {
        double range = player.blockInteractionRange();
        var blockHit = FocusUtils.getTargetBlock(level, player, true, range);
        var focus = Wands.getFocusItem(wand);
        int decomposing = getUpgradeLevel(focus, DECOMPOSING);
        if (decomposing > 0) {
            var eye = player.getEyePosition();
            var end = blockHit == null ? eye.add(player.getViewVector(1.0F).scale(range)) : blockHit.getLocation();
            var hit = ProjectileUtil.getEntityHitResult(player, eye, end,
                player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0),
                entity -> entity instanceof LivingEntity && entity.isPickable() && entity.isAlive(), eye.distanceToSqr(end));
            if (hit != null && hit.getEntity() instanceof LivingEntity target) {
                return level.isClientSide() || level instanceof ServerLevel server && decompose(wand, player, server, target, decomposing)
                    ? InteractionResult.SUCCESS : InteractionResult.PASS;
            }
        }
        if (blockHit == null) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        return player instanceof ServerPlayer serverPlayer && consumeBlock(wand, serverPlayer, blockHit.getBlockPos())
            ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    boolean consumeBlock(ItemStack wand, ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.level();
        var state = level.getBlockState(pos);
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness <= 0 || state.hasBlockEntity() || !level.mayInteract(player, pos)
            || player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())) return false;
        if (CommonHooks.fireBlockBreak(level, player.gameMode.getGameModeForPlayer(), player, pos, state).isCanceled()
            || !level.getBlockState(pos).equals(state)) return false;

        var cost = primalCost(Mth.floor(hardness * 100));
        var payer = findPayer(wand, player, cost);
        if (payer.isEmpty()) return false;
        int potency = Wands.getFocusPotency(wand);
        int nativeExperience = state.getBlock().getExpDrop(state, level, pos, null, player,
            FocusUtils.harvestTool(level, potency, false));
        int vaporizing = getUpgradeLevel(Wands.getFocusItem(wand), VAPORIZING);
        if (!level.destroyBlock(pos, false, player)) return false;
        ((WandItem) payer.getItem()).consumeAllVis(payer, player, cost, true, false);
        ExperienceOrb.award(level, Vec3.atCenterOf(pos), 2 * (nativeExperience > 0 ? nativeExperience : potency + 1));
        if (vaporizing > 1 || vaporizing == 1 && level.getRandom().nextBoolean()) {
            var aspects = AspectHelper.getObjectTags(new ItemStack(state.getBlock()));
            if (aspects != null) {
                for (var aspect : aspects.getAspects()) {
                    var crystal = new ItemStack(ModItems.CRYSTALLIZED_ESSENCE.get());
                    AspectTooltips.setSingle(crystal, aspect, aspects.getAmount(aspect));
                    net.minecraft.world.level.block.Block.popResource(level, pos, crystal);
                }
            }
        }
        return true;
    }

    boolean decompose(ItemStack wand, Player player, ServerLevel level, LivingEntity target, int rank) {
        if (target instanceof Player || target.getType().builtInRegistryHolder().is(BOSSES) || target instanceof thaumcraft.entity.boss.ThaumcraftBoss
            || target instanceof thaumcraft.entity.boss.TaintacleGiant || target instanceof thaumcraft.entity.boss.CultistPortal
            || target.isInvulnerable()
            || !level.mayInteract(player, target.blockPosition()) || player.isSpectator() || !target.isAlive()) return false;
        float health = target.getHealth();
        var cost = primalCost(Mth.floor(health * 100 / rank));
        var payer = findPayer(wand, player, cost);
        if (payer.isEmpty()) return false;
        if (!target.hurtServer(level, level.damageSources().playerAttack(player), Float.MAX_VALUE)) return false;
        ((WandItem) payer.getItem()).consumeAllVis(payer, player, cost, true, false);
        if (!target.isAlive()) ExperienceOrb.award(level, target.position(), Mth.floor(health * 3));
        return true;
    }

    private static AspectList primalCost(int amount) {
        var cost = new AspectList();
        for (var primal : Aspect.getPrimalAspects()) cost.add(primal, Math.max(1, amount));
        return cost;
    }

    private static ItemStack findPayer(ItemStack held, Player player, AspectList cost) {
        if (held.getItem() instanceof WandItem caster && caster.consumeAllVis(held, player, cost, false, false)) return held;
        var inventory = player.getInventory();
        for (int slot = Inventory.INVENTORY_SIZE - 1; slot >= 0; slot--) {
            var candidate = inventory.getItem(slot);
            if (candidate != held && candidate.getItem() instanceof WandItem caster
                && caster.consumeAllVis(candidate, player, cost, false, false)) return candidate;
        }
        return ItemStack.EMPTY;
    }

    static void research() {
        String recipe = Bases.id(ID).toString();
        var result = TcStack.of(recipe);
        var gold = TcIngredient.item("minecraft:gold_ingot");
        var emerald = TcIngredient.item("minecraft:emerald");
        var cost = new AspectList().add(Aspect.GREED, 32).add(Aspect.VOID, 32).add(Aspect.MIND, 16)
            .add(Aspect.HUNGER, 32).add(Aspect.MINE, 8);
        TcRecipes.register(new InfusionRecipe(recipe, KEY, result, 5, cost, TcIngredient.item("thaumcraft:focus_excavation"),
            List.of(gold, gold, emerald, emerald, TcIngredient.item("minecraft:iron_axe"),
                TcIngredient.item("minecraft:iron_pickaxe"), TcIngredient.item("minecraft:iron_shovel"), TcIngredient.item("minecraft:shears"))));
        var tags = new AspectList().add(Aspect.GREED, 16).add(Aspect.HUNGER, 16).add(Aspect.MINE, 2).add(Aspect.MIND, 8);
        ResearchCategories.addResearch(new BasesResearch(KEY, Bases.ID, tags, -10, -2, 1, result)
            .setParents("INFUSION", "FOCUSEXCAVATION")
            .setPages(ResearchPage.text("tc.research_page.TB.Foci.Experience.1"), ResearchPage.infusion(recipe),
                ResearchPage.text("tc.research_page.TB.Foci.Experience.2"), ResearchPage.text("tc.research_page.TB.Foci.Experience.3")));
    }
}
