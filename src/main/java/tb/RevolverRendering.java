package tb;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Bases.ID, value = Dist.CLIENT)
final class RevolverRendering {
    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(Revolver.MENU.get(), RevolverScreen::new);
    }

    @SubscribeEvent
    static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RevolverBolt.TYPE.get(), NoopRenderer::new);
    }
}
