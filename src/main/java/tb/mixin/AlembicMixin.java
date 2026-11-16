package tb.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tb.ThaumiumFurnace;
import thaumcraft.alchemy.AlembicBlockEntity;

@Mixin(value = AlembicBlockEntity.class, remap = false)
abstract class AlembicMixin {
    @Inject(method = "isAboveFurnace", at = @At("RETURN"), cancellable = true)
    private void thaumicbases$recognizeFurnace(CallbackInfoReturnable<Boolean> result) {
        var alembic = (AlembicBlockEntity) (Object) this;
        var level = alembic.getLevel();
        if (!result.getReturnValue() && level != null
            && level.getBlockState(alembic.getBlockPos().below()).getBlock() instanceof ThaumiumFurnace) {
            result.setReturnValue(true);
        }
    }
}
