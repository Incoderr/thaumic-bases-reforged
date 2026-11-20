package tb;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.DynamicArcaneRecipe;
import thaumcraft.research.ResearchGate;
import thaumcraft.wand.WandCap;
import thaumcraft.wand.WandKeys;
import thaumcraft.wand.WandParts;
import thaumcraft.wand.WandRegistry;
import thaumcraft.wand.Wands;

final class BraceletRecap implements DynamicArcaneRecipe {
    private static final int LEFT = 3;
    private static final int BRACELET = 4;
    private static final int RIGHT = 5;

    private WandCap cap(List<ItemStack> grid) {
        var center = grid.get(BRACELET);
        if (!Wands.isWand(center) || !Wands.getRod(center).tag().startsWith("tb_bracelet_")) return null;
        for (int i = 0; i < grid.size(); i++) {
            if (i != LEFT && i != BRACELET && i != RIGHT && !grid.get(i).isEmpty()) return null;
        }
        var left = grid.get(LEFT);
        var right = grid.get(RIGHT);
        if (left.isEmpty() || right.isEmpty() || !left.is(right.getItem())) return null;
        for (var cap : WandRegistry.caps()) {
            if (left.is(BuiltInRegistries.ITEM.get(cap.item()))) return cap;
        }
        return null;
    }

    @Override
    public boolean matches(List<ItemStack> grid, Player player) {
        var cap = cap(grid);
        return cap != null && ResearchGate.isComplete(player, cap.research());
    }

    @Override
    public ItemStack result(List<ItemStack> grid) {
        var cap = cap(grid);
        if (cap == null) return ItemStack.EMPTY;
        var result = grid.get(BRACELET).copyWithCount(1);
        var old = Wands.getParts(result);
        var discounted = WandRegistry.findCap("tb_bracelet_" + cap.tag());
        WandKeys.PARTS.set(result, new WandParts(discounted == null ? cap.tag() : discounted.tag(), old.rod(), false));
        return result;
    }

    @Override
    public AspectList aspects(List<ItemStack> grid) {
        var cap = cap(grid);
        var cost = new AspectList();
        if (cap != null) {
            for (var aspect : Aspect.getPrimalAspects()) cost.add(aspect, cap.craftCost());
        }
        return cost;
    }
}
