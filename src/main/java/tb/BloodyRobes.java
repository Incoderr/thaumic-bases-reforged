package tb;

import java.util.ArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.ShapedArcaneRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.item.gear.Repairable;
import thaumcraft.platform.ArmorSlot;
import thaumcraft.platform.Registration;
import thaumcraft.platform.TcArmorMaterial;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;
import thaumcraft.wand.VisDiscountGear;

final class BloodyRobes {
    private static final String KEY = "TB.BloodyRobes";
    private static final TagKey<Item> REPAIR = TagKey.create(Registries.ITEM, Bases.id("repairs_bloody_robes"));
    private static final TcArmorMaterial MATERIAL = new TcArmorMaterial(21, 2, 6, 5, 2, 21,
        SoundEvents.ARMOR_EQUIP_LEATHER, 0, 0, REPAIR, Bases.id("bloody_robes"));

    static void register(PortRegistration.Items items) {
        piece(items, "bloody_chestplate", ArmorSlot.CHESTPLATE, EquipmentSlotGroup.CHEST, 6, 4, Attributes.MAX_HEALTH, 0.2);
        piece(items, "bloody_leggings", ArmorSlot.LEGGINGS, EquipmentSlotGroup.LEGS, 5, 3, Attributes.ATTACK_KNOCKBACK, 0.5);
        piece(items, "bloody_boots", ArmorSlot.BOOTS, EquipmentSlotGroup.FEET, 2, 2, Attributes.MOVEMENT_SPEED, 0.1);
    }

    private static void piece(PortRegistration.Items items, String id, ArmorSlot armorSlot, EquipmentSlotGroup group,
                              int armor, int discount, Holder<Attribute> attribute, double bonus) {
        items.registerItem(id, p -> new Robe(p, discount), p -> Registration.armor(p, MATERIAL, armorSlot)
            .component(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR, new AttributeModifier(Bases.id(id + "_armor"), armor, AttributeModifier.Operation.ADD_VALUE), group)
                .add(attribute, new AttributeModifier(Bases.id(id + "_bonus"), bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), group)
                .build()));
    }

    private static final class Robe extends Item implements Repairable, VisDiscountGear {
        private final int discount;

        Robe(Properties properties, int discount) {
            super(properties);
            this.discount = discount;
        }

        @Override
        public int getVisDiscount(ItemStack stack, Player player, Aspect aspect) { return discount; }
    }

    static void research() {
        var chest = recipe("bloody_chestplate", Aspect.AIR, 5, "# #", "###", "###");
        var leggings = recipe("bloody_leggings", Aspect.WATER, 5, "###", "# #", "# #");
        var boots = recipe("bloody_boots", Aspect.EARTH, 3, "# #", "# #");
        ResearchCategories.addResearch(new BasesResearch(KEY, Bases.ID,
            new AspectList().add(Aspect.CLOTH, 4).add(Aspect.LIFE, 4).add(Aspect.ARMOR, 4), 5, -6, 2,
            TcStack.of(Bases.id("bloody_chestplate").toString())).setParents("TB.Spike.Iron")
            .setPages(ResearchPage.text("tc.research_page.TB.BloodyRobes.1"), chest, leggings, boots));
    }

    private static ResearchPage recipe(String name, Aspect aspect, int vis, String... rows) {
        String id = Bases.id(name).toString();
        var cloth = TcIngredient.item(Bases.id("bloody_cloth").toString());
        var grid = new ArrayList<TcIngredient>();
        for (String row : rows) for (char c : row.toCharArray()) grid.add(c == '#' ? cloth : TcIngredient.EMPTY);
        TcRecipes.register(new ShapedArcaneRecipe(id, KEY, TcStack.of(id), new AspectList().add(aspect, vis),
            3, rows.length, grid));
        return ResearchPage.arcane(id);
    }
}
