package tb;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import thaumcraft.aspect.AspectHelper;
import thaumcraft.item.AspectTooltips;
import thaumcraft.entity.AspectOrb;
import thaumcraft.registry.ModItems;
import thaumcraft.research.ResearchManager;

@EventBusSubscriber(modid = Bases.ID)
final class EssenceEnchantments {
    private static final ResourceKey<Enchantment> ENCHANTMENT = ResourceKey.create(Registries.ENCHANTMENT, Bases.id("vaporising"));

    private static final ResourceKey<Enchantment> ELDER_WISDOM = ResourceKey.create(Registries.ENCHANTMENT, Bases.id("elder_wisdom"));

    private static final ResourceKey<Enchantment> MAGIC_TOUCH = ResourceKey.create(Registries.ENCHANTMENT, Bases.id("magic_touch"));

    @SubscribeEvent
    static void onDrops(LivingDropsEvent event) {
        var mob = event.getEntity();
        var source = event.getSource();
        if (!(mob.level() instanceof ServerLevel level) || !(source.getEntity() instanceof Player player)
            || source.getDirectEntity() != player) return;
        var mainHand = player.getMainHandItem();
        if (mainHand.isEmpty()) return;
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        int wisdom = enchantments.get(ELDER_WISDOM).map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, mainHand)).orElse(0);
        int rank = enchantments.get(ENCHANTMENT).map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, mainHand)).orElse(0);
        int magic = enchantments.get(MAGIC_TOUCH).map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, mainHand)).orElse(0);
        if (wisdom <= 0 && rank <= 0 && magic <= 0) return;
        var random = mob.getRandom();
        var aspects = AspectHelper.getEntityTags(mob);
        if (aspects == null) return;
        if (wisdom > 0 && random.nextInt(Math.max(1, 7 - wisdom)) == 0) {
            for (var aspect : aspects.getAspects()) {
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    ResearchManager.addAspectPool(serverPlayer, aspect, 1);
                }
                if (random.nextBoolean()) break;
            }
        }
        if (magic > 0) {
            var primals = AspectHelper.reduceToPrimals(aspects);
            for (int roll = 0; roll < magic; roll++) {
                for (var aspect : primals.getAspects()) {
                    int amount = primals.getAmount(aspect);
                    if (amount > 0 && random.nextBoolean()) {
                        var orb = AspectOrb.create(level, mob.getX(), mob.getY(), mob.getZ(), aspect, 1 + random.nextInt(amount));
                        if (orb != null) level.addFreshEntity(orb);
                    }
                }
                if (random.nextBoolean()) break;
            }
        }
        if (rank <= 0 || random.nextInt(Math.max(1, 5 - rank)) != 0) return;
        for (var aspect : aspects.getAspects()) {
            int amount = aspects.getAmount(aspect);
            if (amount > 0 && random.nextBoolean()) {
                var crystal = new ItemStack(ModItems.CRYSTALLIZED_ESSENCE.get());
                crystal.setCount(Math.min(crystal.getMaxStackSize(), Math.max(1, (1 + random.nextInt(amount)) / 2)));
                AspectTooltips.setSingle(crystal, aspect, 1);
                event.getDrops().add(new ItemEntity(level, mob.getX(), mob.getY() + mob.getEyeHeight(), mob.getZ(), crystal));
            }
            if (random.nextInt(2 + rank) == 0) break;
        }
    }

    private EssenceEnchantments() {}
}
