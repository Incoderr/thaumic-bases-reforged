package tb.mixin;

import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tb.MagicalAnvil;

@Mixin(AnvilBlock.class)
abstract class AnvilBlockMixin {
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private static void thaumicbases$damage(BlockState state, CallbackInfoReturnable<BlockState> result) {
        if (state.getBlock() instanceof MagicalAnvil anvil) {
            result.setReturnValue(anvil.damagedState(state));
        }
    }
}
