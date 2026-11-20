package tb;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Loads real client models; excluded from the published artifact. */
@EventBusSubscriber(modid = Bases.ID, value = Dist.CLIENT)
public final class ClientPortSmoke {
    private static boolean checked;
    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (checked || !Boolean.getBoolean("thaumicbases.clientSmoke")) return;
        var mc = Minecraft.getInstance();
        if (mc.getOverlay() != null || mc.screen == null) return;
        checked = true;
        try {
            Class.forName("net.minecraft.client.gui.screens.inventory.AnvilScreen");
            int items = 0;
            for (var entry : BuiltInRegistries.ITEM.entrySet()) {
                if (!entry.getKey().location().getNamespace().equals(Bases.ID)) continue;
                var model = mc.getItemRenderer().getModel(new ItemStack(entry.getValue()), null, null, 0);
                if (model == mc.getModelManager().getMissingModel()) throw new AssertionError("Missing item model: " + entry.getKey().location());
                if (model.getParticleIcon(net.neoforged.neoforge.client.model.data.ModelData.EMPTY).contents().name().equals(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation())) {
                    throw new AssertionError("Missing item texture: " + entry.getKey().location());
                }
                items++;
            }
            for (var variant : Bracelets.Variant.values()) {
                if (!variant.available()) continue;
                var model = mc.getItemRenderer().getModel(variant.stack().create(), null, null, 0);
                if (model == mc.getModelManager().getMissingModel() || model.isCustomRenderer()) {
                    throw new AssertionError("Bracelet did not select its OBJ model: " + variant);
                }
            }
            com.mojang.logging.LogUtils.getLogger().info("TB_CLIENT_SMOKE_PASSED: {} item models and bracelet overrides", items);
        } catch (Throwable failure) {
            com.mojang.logging.LogUtils.getLogger().error("TB_CLIENT_SMOKE_FAILED", failure);
        } finally {
            mc.stop();
        }
    }
}
