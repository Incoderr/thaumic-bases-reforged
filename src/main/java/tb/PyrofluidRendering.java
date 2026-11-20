package tb;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = Bases.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
final class PyrofluidRendering {
    @SubscribeEvent
    static void register(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            public ResourceLocation getStillTexture() { return Bases.id("block/pyrofluid"); }
            public ResourceLocation getFlowingTexture() { return Bases.id("block/pyrofluid"); }
        }, MoreAlchemy.FLUID_TYPE.get());
    }
}
