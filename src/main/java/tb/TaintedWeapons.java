package tb;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import thaumcraft.effect.EffectHooks;
import thaumcraft.entity.TaintedMob;
import thaumcraft.node.NodeBlockEntity;
import thaumcraft.node.NodeType;

@EventBusSubscriber(modid = Bases.ID)
final class TaintedWeapons {
    private static final ResourceKey<Enchantment> TAINTED = ResourceKey.create(Registries.ENCHANTMENT, Bases.id("tainted"));

    @SubscribeEvent
    static void enchantAtNode(ItemExpireEvent event) {
        var item = event.getEntity();
        if (!(item.level() instanceof ServerLevel level) || !(level.getBlockEntity(item.blockPosition()) instanceof NodeBlockEntity node)
            || node.getNodeType() != NodeType.TAINTED) return;
        var stack = item.getItem();
        if (!stack.is(ItemTags.SWORDS)) return;

        var enchantment = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(TAINTED);
        var existing = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        if (existing.keySet().stream().anyMatch(other -> !Enchantment.areCompatible(enchantment, other))) return;
        stack.enchant(enchantment, 1 + level.getRandom().nextInt(3));
        item.setItem(stack.copy());
        event.setExtraLife(1000);
    }

    @SubscribeEvent
    static void onDamage(LivingIncomingDamageEvent event) {
        var target = event.getEntity();
        var source = event.getSource();
        if (!(target.level() instanceof ServerLevel level) || target instanceof TaintedMob
            || !(source.getEntity() instanceof Player player) || source.getDirectEntity() != player) return;
        var enchantment = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(TAINTED);
        int rank = EnchantmentHelper.getItemEnchantmentLevel(enchantment, player.getMainHandItem());
        if (rank <= 0) return;
        event.setAmount(event.getAmount() + 3F * rank);
        if (player.getRandom().nextInt(Math.max(1, 4 - rank)) == 0) {
            EffectHooks.applyTaintPoison(target, 200, rank - 1);
        }
    }

    private TaintedWeapons() {}
}
