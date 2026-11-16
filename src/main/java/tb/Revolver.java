package tb;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectKeys;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.essentia.FilledJarItem;
import thaumcraft.item.gear.WarpingGear;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;

final class Revolver extends Item implements WarpingGear {
    static final String ID = "revolver";
    private static final String KEY = "TB.Revolver";
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Bases.ID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Bases.ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Bases.ID);
    static final Supplier<MenuType<RevolverMenu>> MENU =
        MENUS.register(ID, () -> new MenuType<>(RevolverMenu::new, FeatureFlags.VANILLA_SET));

    private static final Supplier<DataComponentType<ItemStackTemplate>> JAR =
        COMPONENTS.register("revolver_jar", () -> DataComponentType.<ItemStackTemplate>builder()
            .persistent(ItemStackTemplate.CODEC).networkSynchronized(ItemStackTemplate.STREAM_CODEC).build());
    private static final Supplier<DataComponentType<Integer>> ROUNDS =
        COMPONENTS.register("revolver_rounds", () -> DataComponentType.<Integer>builder()
            .persistent(Codec.intRange(0, 7)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
    private static final Supplier<SoundEvent> SHOT = sound("revolver_shot");
    private static final Supplier<SoundEvent> RELOAD = sound("revolver_reload");
    private static final Supplier<SoundEvent> EMPTY = sound("revolver_no_ammo");

    Revolver(Properties properties) { super(properties); }

    private static Supplier<SoundEvent> sound(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(Bases.id(id)));
    }

    static void register(DeferredRegister.Items items) {
        items.registerItem(ID, Revolver::new, p -> p.stacksTo(1).durability(512).rarity(Rarity.RARE));
    }

    static void registerTypes(IEventBus bus) {
        COMPONENTS.register(bus);
        SOUNDS.register(bus);
        MENUS.register(bus);
        RevolverBolt.register(bus);
    }

    static ItemStack storedJar(ItemStack revolver) {
        var stored = revolver.get(JAR.get());
        return stored == null ? ItemStack.EMPTY : stored.create();
    }

    static boolean isTelumJar(ItemStack stack) {
        return stack.getItem() instanceof FilledJarItem
            && AspectKeys.ASPECTS.getOrDefault(stack, new AspectList()).getAmount(Aspect.WEAPON) > 0;
    }

    static void setJar(ItemStack revolver, ItemStack stack) {
        if (stack.isEmpty()) revolver.remove(JAR.get());
        else revolver.set(JAR.get(), ItemStackTemplate.fromNonEmptyStack(stack, 1));
    }

    static int rounds(ItemStack stack) {
        return stack.getOrDefault(ROUNDS.get(), 0);
    }

    static boolean reload(ItemStack stack) {
        if (rounds(stack) != 0) return false;
        var jar = storedJar(stack);
        if (!isTelumJar(jar)) return false;
        var contents = AspectKeys.ASPECTS.get(jar).copy();
        contents.reduce(Aspect.WEAPON, 1);
        AspectKeys.ASPECTS.set(jar, contents);
        setJar(stack, jar);
        stack.set(ROUNDS.get(), 2 - RevolverUpgrade.PRIMAL.level(stack) + RevolverUpgrade.EFFICIENCY.level(stack));
        return true;
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !(entity instanceof Player player) || !player.isShiftKeyDown()) return false;
        if (player instanceof ServerPlayer serverPlayer && player.getMainHandItem() == stack) {
            serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new RevolverMenu(id, inventory, stack), Component.translatable("container.thaumicbases.revolver")));
        }
        return true;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        var held = player.getItemInHand(hand);
        player.getCooldowns().addCooldown(held, 4);
        if (level instanceof ServerLevel server) {
            var stack = held;
            var sound = EMPTY.get();
            if (rounds(stack) > 0) {
                if (!RevolverBolt.fire(server, player, stack)) return InteractionResult.FAIL;
                stack.set(ROUNDS.get(), rounds(stack) - 1);
                var installedJar = storedJar(stack);
                stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                if (stack.isEmpty() && !installedJar.isEmpty() && !player.getInventory().add(installedJar)) {
                    player.drop(installedJar, false, net.minecraft.util.Prediction.PREDICTED);
                }
                sound = SHOT.get();
            } else if (reload(stack)) {
                sound = RELOAD.get();
            }
            server.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        var jar = storedJar(stack);
        int essentia = AspectKeys.ASPECTS.getOrDefault(jar, new AspectList()).getAmount(Aspect.WEAPON);
        builder.accept(Component.translatable("item.thaumicbases.revolver.ammo", rounds(stack), essentia));
        for (var upgrade : RevolverUpgrade.values()) {
            int level = upgrade.level(stack);
            if (level > 0) builder.accept(Component.translatable(upgrade.translationKey(), level));
        }
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        int damage = 5 + 2 * RevolverUpgrade.HEAVY.level(stack);
        return ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE,
            new AttributeModifier(BASE_ATTACK_DAMAGE_ID, damage, AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND).build();
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        int repair = RevolverUpgrade.VOID.level(stack);
        if (repair > 0 && stack.isDamaged() && owner instanceof LivingEntity && owner.tickCount % 100 < repair) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return RevolverUpgrade.VOID.level(stack);
    }

    static void research() {
        String id = Bases.id(ID).toString();
        var result = TcStack.of(id);
        var powder = TcIngredient.item("minecraft:gunpowder");
        var fireCharge = TcIngredient.item("minecraft:fire_charge");
        var wood = TcIngredient.item("thaumcraft:greatwood_log");
        var cost = new AspectList().add(Aspect.WEAPON, 256).add(Aspect.METAL, 32).add(Aspect.TREE, 16).add(Aspect.TOOL, 12)
            .add(Aspect.ENERGY, 32).add(Aspect.MECHANISM, 12).add(Aspect.VOID, 16).add(Aspect.FIRE, 32);
        TcRecipes.register(new InfusionRecipe(id, KEY, result, 5, cost, TcIngredient.item("minecraft:iron_block"),
            List.of(fireCharge, powder, TcIngredient.item("thaumcraft:primal_charm"), wood, powder,
                TcIngredient.item("thaumcraft:focus_fire"), fireCharge, powder, TcIngredient.item("thaumcraft:bone_bow"),
                wood, powder, TcIngredient.item("minecraft:redstone_torch"))));
        var tags = new AspectList().add(Aspect.WEAPON, 5).add(Aspect.FIRE, 5).add(Aspect.VOID, 4).add(Aspect.TOOL, 2).add(Aspect.ENERGY, 2);
        ResearchCategories.addResearch(new BasesResearch(KEY, Bases.ID, tags, 12, 0, 4, result)
            .setParents("INFUSION").setPages(ResearchPage.text("tc.research_page.TB.Revolver.1"), ResearchPage.infusion(id)));
        RevolverUpgrade.registerResearch(KEY, id);
    }
}
