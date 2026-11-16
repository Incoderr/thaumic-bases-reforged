package tb;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.registries.DeferredRegister;

final class RevolverBolt extends AbstractArrow {
    private static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(Bases.ID);
    static final java.util.function.Supplier<EntityType<RevolverBolt>> TYPE = ENTITIES.registerEntityType("revolver_bolt",
        RevolverBolt::new, MobCategory.MISC, p -> p.noSave().sized(0.12F, 0.12F).clientTrackingRange(8).updateInterval(1));

    private ItemStack source = ItemStack.EMPTY;
    private double shotDamage = 14.0 / 3.0;

    RevolverBolt(EntityType<? extends RevolverBolt> type, Level level) {
        super(type, level);
        setNoGravity(true);
        pickup = Pickup.DISALLOWED;
    }

    static void register(IEventBus bus) { ENTITIES.register(bus); }

    static boolean fire(ServerLevel level, Player player, ItemStack revolver) {
        var bolt = new RevolverBolt(TYPE.get(), level);
        bolt.setOwner(player);
        bolt.source = revolver.copy();
        Revolver.setJar(bolt.source, ItemStack.EMPTY);
        bolt.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
        boolean primal = RevolverUpgrade.PRIMAL.level(revolver) > 0;
        bolt.setNoPhysics(primal);
        float speed = (3.0F + 0.3F * RevolverUpgrade.SPEED.level(revolver)) * (primal ? 2.0F : 1.0F);
        float spread = (player.isShiftKeyDown() ? 0.25F : 1.5F) / (1 + RevolverUpgrade.ACCURACY.level(revolver));
        bolt.shotDamage = 14.0 / speed;
        bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, speed, spread);
        return level.addFreshEntity(bolt);
    }

    @Override
    protected ItemStack getDefaultPickupItem() { return ItemStack.EMPTY; }

    @Override
    public void tick() {
        if (!level().isClientSide() && isNoPhysics()) {
            var motion = getDeltaMovement();
            var hit = ProjectileUtil.getEntityHitResult(level(), this, position(), position().add(motion),
                getBoundingBox().expandTowards(motion).inflate(1), target -> target != getOwner() && canHitEntity(target));
            if (hit != null && !EventHooks.onProjectileImpact(this, hit)) {
                hitTargetOrDeflectSelf(hit);
                if (!isAlive()) return;
            }
        }
        super.tick();
        if (!level().isClientSide() && tickCount >= 30) discard();
        if (level().isClientSide()) {
            var movement = getDeltaMovement();
            for (int i = 0; i < 4; i++) {
                double fraction = i / 4.0;
                level().addParticle(isNoPhysics() ? ParticleTypes.ENCHANT : ParticleTypes.END_ROD, getX() - movement.x * fraction,
                    getY() - movement.y * fraction, getZ() - movement.z * fraction, 0, 0, 0);
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (!level().isClientSide()) discard();
    }

    @Override
    public byte getPierceLevel() {
        return RevolverUpgrade.PIERCING.level(source) > 0 ? Byte.MAX_VALUE : super.getPierceLevel();
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        var target = hit.getEntity() instanceof LivingEntity living ? living : null;
        float health = target == null ? 0 : target.getHealth();
        if (!level().isClientSide()) {
            setBaseDamage(shotDamage * RevolverUpgrade.damageMultiplier(source, hit.getEntity()));
        }
        super.onHitEntity(hit);
        if (!level().isClientSide()) {
            if (target != null && target.getHealth() < health) RevolverUpgrade.afterHit(source, target, getOwner());
            if (getPierceLevel() == 0) discard();
        }
    }
}
