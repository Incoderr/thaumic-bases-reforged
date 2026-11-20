package tb;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

final class RevolverMenu extends AbstractContainerMenu {
    private static final int HOTBAR_START = 28;
    private final int selected;
    private final ItemStack revolver;

    RevolverMenu(int id, Inventory inventory) {
        this(id, inventory, ItemStack.EMPTY);
    }

    RevolverMenu(int id, Inventory inventory, ItemStack revolver) {
        super(Revolver.MENU.get(), id);
        this.revolver = revolver;
        selected = inventory.selected;

        var chamber = new SimpleContainer(1) {
            @Override
            public void setChanged() {
                super.setChanged();
                if (!revolver.isEmpty()) Revolver.setJar(revolver, getItem(0));
            }
        };
        chamber.setItem(0, Revolver.storedJar(revolver));
        addSlot(new Slot(chamber, 0, 80, 32) {
            @Override
            public boolean mayPlace(ItemStack stack) { return Revolver.isTelumJar(stack); }

            @Override
            public int getMaxStackSize() { return 1; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
    }

    int selected() { return selected; }

    @Override
    public boolean stillValid(Player player) {
        return player.level().isClientSide() || (!revolver.isEmpty() && player.getMainHandItem() == revolver);
    }

    @Override
    public void clicked(int slot, int button, ClickType input, Player player) {
        if (!stillValid(player) || slot == HOTBAR_START + selected
            || (input == ClickType.SWAP && button == selected)) return;
        super.clicked(slot, button, input, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index == HOTBAR_START + selected) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem();
        var original = stack.copy();
        if (index == 0 ? !moveItemStackTo(stack, 1, slots.size(), true) : !moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }
}
