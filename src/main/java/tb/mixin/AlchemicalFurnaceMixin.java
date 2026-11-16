package tb.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tb.ThaumiumFurnace;
import thaumcraft.alchemy.AlchemicalFurnaceBlockEntity;

@Mixin(value = AlchemicalFurnaceBlockEntity.class, remap = false)
abstract class AlchemicalFurnaceMixin {
    @Shadow private int smeltTime;
    @Shadow private boolean speedBoost;

    @Inject(method = "canSmelt", at = @At("RETURN"))
    private void thaumicbases$boostAlumentum(CallbackInfoReturnable<Boolean> result) {
        var furnace = (AlchemicalFurnaceBlockEntity) (Object) this;
        if (result.getReturnValue() && speedBoost && furnace.getBlockState().getBlock() instanceof ThaumiumFurnace) {
            smeltTime = Math.max(1, smeltTime / 2);
        }
    }
}
