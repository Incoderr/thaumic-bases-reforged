package tb;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.client.platform.GuiBlit;

final class RevolverScreen extends AbstractContainerScreen<RevolverMenu> {
    RevolverScreen(RevolverMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176; imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        GuiBlit.blit(graphics, Bases.id("textures/gui/revolver.png"), leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        GuiBlit.blit(graphics, Bases.id("textures/gui/revolver.png"), 8 + 18 * menu.selected(), 142, 240, 0, 16, 16, 256, 256);
    }
}
