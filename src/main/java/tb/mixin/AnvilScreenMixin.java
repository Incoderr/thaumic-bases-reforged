package tb.mixin;

import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(AnvilScreen.class)
abstract class AnvilScreenMixin extends ItemCombinerScreen<AnvilMenu> {
    AnvilScreenMixin(AnvilMenu menu, Inventory inventory, Component title, ResourceLocation menuResource) {
        super(menu, inventory, title, menuResource);
    }

    @ModifyConstant(method = "renderLabels", constant = @Constant(intValue = 40))
    private int thaumicbases$displayLimit(int vanilla) {
        if (title.getContents() instanceof TranslatableContents contents
            && contents.getKey().startsWith("block.thaumicbases.")
            && contents.getKey().endsWith("thaumium_anvil")) {
            return 60;
        }
        return vanilla;
    }
}
