package tb.mixin;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import tb.MagicalAnvil;

@Mixin(AnvilMenu.class)
abstract class AnvilMenuMixin {
    @ModifyConstant(method = "createResult", constant = {
        @Constant(intValue = 40, ordinal = 1), @Constant(intValue = 40, ordinal = 2)
    }, require = 2)
    private int thaumicbases$costLimit(int vanilla) {
        return (Object) this instanceof MagicalAnvil.Menu menu && menu.getSlot(0).getItem().getCount() == 1
            ? menu.exclusiveCostLimit() : vanilla;
    }

    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 39))
    private int thaumicbases$renameLimit(int vanilla) {
        return thaumicbases$costLimit(vanilla + 1) - 1;
    }

    @Redirect(method = "createResult", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/ItemStack;getOrDefault(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;"))
    private <T> T thaumicbases$repairPenalty(ItemStack stack, DataComponentType<T> component, T fallback) {
        return (Object) this instanceof MagicalAnvil.Menu && component.equals(DataComponents.REPAIR_COST)
            ? fallback : stack.getOrDefault(component, fallback);
    }

}
