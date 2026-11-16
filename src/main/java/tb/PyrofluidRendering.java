package tb;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;

@EventBusSubscriber(modid = Bases.ID, value = Dist.CLIENT)
final class PyrofluidRendering {
    @SubscribeEvent
    static void register(RegisterFluidModelsEvent event) {
        var texture = new Material(Bases.id("block/pyrofluid"));
        event.register(new FluidModel.Unbaked(texture, texture, null, null), MoreAlchemy.FLUID, MoreAlchemy.FLOWING);
    }
}
