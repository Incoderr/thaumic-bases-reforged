package tb;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;
import thaumcraft.wand.WandItem;
import thaumcraft.wand.Wands;
import thaumcraft.wand.focus.FocusItem;
import thaumcraft.wand.focus.FocusUpgradeType;

final class ActivationFocus extends FocusItem {
    private static final String ID = "focus_activation";
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Bases.ID);
    private static final java.util.function.Supplier<DataComponentType<GlobalPos>> TARGET =
        COMPONENTS.register("activation_target", () -> DataComponentType.<GlobalPos>builder()
            .persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build());

    static void registerComponents(IEventBus bus) { COMPONENTS.register(bus); }

    ActivationFocus(Properties properties) { super(properties); }

    static void register(DeferredRegister.Items items) {
        items.registerItem(ID, ActivationFocus::new, FocusItem::properties);
    }

    @Override
    public int getFocusColor(ItemStack focus) { return 0xAAAAAA; }

    @Override
    public net.minecraft.resources.Identifier getFocusTexture(ItemStack focus) { return Bases.id("item/focus_activation"); }

    @Override
    public AspectList getVisCost(ItemStack focus) { return new AspectList().add(Aspect.ORDER, 5).add(Aspect.EARTH, 10); }

    @Override
    public FocusUpgradeType[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
        return new FocusUpgradeType[] { FocusUpgradeType.FRUGAL, FocusUpgradeType.POTENCY };
    }

    @Override
    public InteractionResult onFocusRightClick(ItemStack wand, Level level, Player player, HitResult hit) {
        var focus = Wands.getFocusItem(wand);
        if (focus.isEmpty() || !(wand.getItem() instanceof WandItem caster)) return InteractionResult.PASS;

        if (player.isShiftKeyDown()) {
            if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
            var bindingCost = new AspectList().add(Aspect.ORDER, 100);
            if (!caster.consumeAllVis(wand, player, bindingCost, !level.isClientSide(), false)) return InteractionResult.PASS;
            if (!level.isClientSide()) {
                focus.set(TARGET.get(), GlobalPos.of(level.dimension(), block.getBlockPos()));
                Wands.setFocus(wand, focus);
                player.sendSystemMessage(Component.translatable("item.thaumicbases.focus_activation.bound"));
            }
            return InteractionResult.SUCCESS;
        }

        var target = focus.get(TARGET.get());
        if (target == null) return InteractionResult.PASS;
        int range = 32 * (1 + Wands.getFocusPotency(wand));
        var center = Vec3.atCenterOf(target.pos());
        if (!target.dimension().equals(level.dimension()) || !level.hasChunkAt(target.pos())
            || !level.mayInteract(player, target.pos()) || player.distanceToSqr(center) > (double) range * range) {
            if (!level.isClientSide()) player.sendSystemMessage(Component.translatable("item.thaumicbases.focus_activation.unreachable"));
            return InteractionResult.SUCCESS;
        }

        var cost = getVisCost(focus);
        if (!caster.consumeAllVis(wand, player, cost, false, false)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            var direction = Direction.getApproximateNearest(player.getX() - center.x, player.getY() - center.y,
                player.getZ() - center.z);
            var block = level.getBlockState(target.pos());
            var remoteHit = new BlockHitResult(center, direction, target.pos(), false);
            if (block.useWithoutItem(level, player, remoteHit).consumesAction()) {
                caster.consumeAllVis(wand, player, cost, true, false);
            }
        }
        return InteractionResult.SUCCESS;
    }

    static void research() {
        String key = "TB.Foci.Activation";
        String id = Bases.id(ID).toString();
        var icon = TcStack.of(id);
        var fragment = TcIngredient.item("thaumcraft:knowledge_fragment");
        var fabric = TcIngredient.item("thaumcraft:enchanted_fabric");
        TcRecipes.register(new InfusionRecipe(id, key, icon, 0,
            new AspectList().add(Aspect.TRAVEL, 8).add(Aspect.SENSES, 8).add(Aspect.MOTION, 8),
            TcIngredient.item("thaumcraft:balanced_shard"), List.of(fragment, fragment, fabric, fabric)));
        var research = new AspectList().add(Aspect.TRAVEL, 4).add(Aspect.MOTION, 4).add(Aspect.SENSES, 4);
        ResearchCategories.addResearch(new BasesResearch(key, Bases.ID, research, -8, -3, 1, icon)
            .setParents("INFUSION", "MIRRORHAND")
            .setPages(ResearchPage.text("tc.research_page.TB.Foci.Activation.1"), ResearchPage.infusion(id)));
    }
}
