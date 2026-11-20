package tb;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Integration checks included in development runs only, never in the release JAR. */
@EventBusSubscriber(modid = Bases.ID)
public final class PortSmokeTests {
    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        if (!Boolean.getBoolean("thaumicbases.smoke")) return;
        var server = event.getServer();
        server.execute(() -> {
            try {
                var registries = server.registryAccess();
                int recipes = 0;
                for (var path : server.getResourceManager().listResources("recipe", id -> id.getNamespace().equals(Bases.ID) && id.getPath().endsWith(".json")).keySet()) {
                    var id = ResourceLocation.fromNamespaceAndPath(Bases.ID, path.getPath().substring(7, path.getPath().length() - 5));
                    if (server.getRecipeManager().byKey(id).isEmpty()) throw new AssertionError("Recipe not loaded: " + id);
                    recipes++;
                }
                for (var variant : Bracelets.Variant.values()) {
                    if (!variant.available()) continue;
                    ItemStack bracelet = variant.stack().create();
                    if (!thaumcraft.wand.Wands.getParts(bracelet).rod().equals(variant.rodKey())) throw new AssertionError("Bracelet parts: " + variant);
                    var ops = registries.createSerializationContext(NbtOps.INSTANCE);
                    var encoded = ItemStack.CODEC.encodeStart(ops, bracelet).getOrThrow();
                    var decoded = ItemStack.CODEC.parse(ops, encoded).getOrThrow();
                    if (!ItemStack.isSameItemSameComponents(bracelet, decoded)) throw new AssertionError("Bracelet serialization: " + variant);
                }
                for (String name : new String[]{"overchanter", "node_linker", "entity_deconstructor", "thaumic_campfire"}) {
                    var block = BuiltInRegistries.BLOCK.get(Bases.id(name));
                    if (!(block instanceof net.minecraft.world.level.block.EntityBlock factory)) throw new AssertionError("Missing block entity: " + name);
                    var tile = factory.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
                    CompoundTag tag = tile.saveWithFullMetadata(registries);
                    var restored = BlockEntity.loadStatic(BlockPos.ZERO, block.defaultBlockState(), tag, registries);
                    if (restored == null || restored.getType() != tile.getType()) throw new AssertionError("Block entity serialization: " + name);
                }
                var campfire = new ThaumicCampfire.Fire(BlockPos.ZERO, BuiltInRegistries.BLOCK.get(Bases.id("thaumic_campfire")).defaultBlockState());
                CompoundTag fuel = campfire.saveWithFullMetadata(registries);
                fuel.putInt("logs", 1234);
                fuel.putInt("fuel", 321);
                campfire.loadWithComponents(fuel, registries);
                var fuelSaved = campfire.saveWithFullMetadata(registries);
                if (fuelSaved.getInt("logs") != 1234 || fuelSaved.getInt("fuel") != 321) throw new AssertionError("Campfire fuel did not survive loading");

                var overchanter = BuiltInRegistries.BLOCK.get(Bases.id("overchanter"));
                var table = (Overchanter.Table) ((net.minecraft.world.level.block.EntityBlock) overchanter).newBlockEntity(BlockPos.ZERO, overchanter.defaultBlockState());
                CompoundTag contents = table.saveWithFullMetadata(registries);
                contents.put("item", ItemStack.OPTIONAL_CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), new ItemStack(Items.DIAMOND)).getOrThrow());
                table.loadWithComponents(contents, registries);
                table.setRemoved(); // Chunk unload must never invoke block-destruction effects.
                table.clearRemoved();
                var afterUnload = table.saveWithFullMetadata(registries);
                var preserved = ItemStack.OPTIONAL_CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), afterUnload.get("item")).getOrThrow();
                if (!preserved.is(Items.DIAMOND)) throw new AssertionError("Overchanter lost its inventory on chunk unload");
                com.mojang.logging.LogUtils.getLogger().info("TB_PORT_SMOKE_PASSED: {} recipes, bracelets, fuel persistence and unload safety", recipes);
            } catch (Throwable failure) {
                failure.printStackTrace();
                com.mojang.logging.LogUtils.getLogger().error("TB_PORT_SMOKE_FAILED", failure);
            } finally {
                server.halt(false);
            }
        });
    }
}
