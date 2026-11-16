package tb;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import thaumcraft.wand.Wands;
import thaumcraft.wand.WandKeys;
import thaumcraft.node.NodeBlockEntity;
import thaumcraft.client.render.WorldRendering;
import thaumcraft.client.geom.WandGeometry;
import thaumcraft.client.platform.ItemGeometry;
import thaumcraft.Thaumcraft;
import net.minecraft.data.AtlasIds;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@EventBusSubscriber(modid = Bases.ID, value = Dist.CLIENT)
final class BraceletHandRenderer {
    @SubscribeEvent
    static void renderHand(RenderHandEvent event) {
        var stack = event.getItemStack();
        if (event.getHand() != InteractionHand.MAIN_HAND || !isBracelet(stack)) return;

        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var avatar = event.getPlayerState().avatarRenderState;
        if (player == null || avatar == null || player.isInvisible() || !isBracelet(player.getMainHandItem())) return;
        var arm = player.getMainArm();
        boolean right = arm == HumanoidArm.RIGHT;
        float side = right ? 1 : -1;
        float swing = event.getSwingProgress();
        float swingRoot = Mth.sqrt(swing);
        float wave = Mth.sin(swingRoot * Math.PI);

        float useTicks = 0;
        float reach = 0;
        if (player.isUsingItem() && player.getUsedItemHand() == event.getHand() && isBracelet(player.getUseItem())) {
            float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            useTicks = player.getTicksUsingItem(partialTick);
            float progress = Mth.clamp(useTicks / 4.0F, 0.0F, 1.0F);
            reach = 1.0F - (1.0F - progress) * (1.0F - progress);
        }

        event.setCanceled(true);
        var poses = event.getPoseStack();
        var collector = event.getSubmitNodeCollector();
        int light = event.getPackedLight();
        var handBase = new Matrix4f(poses.last().pose());
        poses.pushPose();
        poses.translate(side * (0.585F - 0.3F * wave),
            -0.56F - 0.6F * event.getEquipProgress() + 0.4F * Mth.sin(swingRoot * 2 * Math.PI),
            -0.72F - 0.4F * Mth.sin(swing * Math.PI));
        // Keep the band across the sleeve as in the original first-person pose.
        poses.translate(-side * (0.5F / 3.0F) * reach, 0.16F * reach, -0.23F * reach);
        poses.rotateDegrees(Axis.ZP, side * (22.0F - 6.0F * reach));
        poses.rotateDegrees(Axis.XP, -12.0F * reach);
        poses.rotateDegrees(Axis.YP, side * (45 + 70 * wave));
        poses.rotateDegrees(Axis.ZP, -side * 20 * Mth.sin(swing * swing * Math.PI));
        poses.translate(-side, 3.6F, 3.5F);
        poses.rotateDegrees(Axis.ZP, side * 120);
        poses.rotateDegrees(Axis.XP, 200);
        poses.rotateDegrees(Axis.YP, -side * 135);
        poses.translate(side * 5.6F, 0, 0);

        var renderer = (AvatarRenderer<?>) minecraft.getEntityRenderDispatcher().getRenderer(avatar);
        var skin = avatar.skin.body().texturePath();
        if (right) renderer.renderRightHand(poses, collector, light, skin, avatar.showRightSleeve);
        else renderer.renderLeftHand(poses, collector, light, skin, avatar.showLeftSleeve);

        var playerModel = (net.minecraft.client.model.player.PlayerModel) renderer.getModel();
        var armPart = right ? playerModel.rightArm : playerModel.leftArm;
        armPart.translateAndRotate(poses);
        boolean slim = avatar.skin.model() == net.minecraft.world.entity.player.PlayerModelType.SLIM;
        boolean sleeve = right ? avatar.showRightSleeve : avatar.showLeftSleeve;
        float sleevePixels = sleeve ? 0.25F : 0.0F;
        float armHalfWidth = ((slim ? 1.5F : 2.0F) + sleevePixels) / 16.0F;
        float armHalfDepth = (2.0F + sleevePixels) / 16.0F;
        float bandDepthScale = 0.067F;
        // The OBJ opens toward negative Y. Seat the inner rim outside the arm in model-part space.
        float rimOffset = armHalfWidth + 0.003F - 0.10576F * bandDepthScale;
        float bandWidthScale = (armHalfDepth + 0.022F) / 3.178322F;
        float armCenterX = -side * (slim ? 0.03125F : 0.0625F);
        float wristHeight = 0.31F;
        poses.translate(armCenterX - side * rimOffset, wristHeight, 0);
        poses.rotateDegrees(Axis.YP, side * 90);
        poses.rotateDegrees(Axis.XP, -90);
        float bandLengthScale = 0.052F;
        poses.scale(bandWidthScale, bandDepthScale, bandLengthScale);
        poses.translate(0.5F, 0.5F, 0.5F);
        var model = new ItemStackRenderState();
        minecraft.getItemModelResolver().updateForLiving(model, stack, ItemDisplayContext.FIXED, player);
        model.submit(poses, collector, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0);
        // FIXED inherits the half-scale block transform and centers its quads before drawing.
        // Apply that same transform to the center of the pyramid's top face in bracelet.obj.
        float fixedScale = 0.5F;
        poses.scale(fixedScale, fixedScale, fixedScale);
        poses.translate(-0.5F, 3.144806F - 0.5F, -0.5F);
        var wrist = handBase.invert().mul(poses.last().pose()).transformPosition(new Vector3f());
        var focus = Wands.getFocusItem(stack);
        if (focus.getItem() instanceof thaumcraft.wand.focus.FocusItem focusItem) {
            poses.scale(1.0F / (bandWidthScale * fixedScale), 1.0F / (bandDepthScale * fixedScale),
                1.0F / (bandLengthScale * fixedScale));
            poses.translate(0, -0.002F, 0);
            poses.rotateDegrees(Axis.XP, 180);
            // Size the entire focus assembly relative to the bracelet width.
            float focusScale = bandWidthScale * 3.0F;
            poses.scale(focusScale, focusScale, focusScale);
            var atlas = minecraft.getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS);
            var ornament = focusItem.getOrnamentTexture(focus);
            var depth = focusItem.getDepthLayerTexture(focus);
            if (ornament != null) {
                var sprite = atlas.getSprite(ornament);
                ItemGeometry.solid(collector.order(0), poses, sprite.atlasLocation(), true, sink ->
                    WandGeometry.focusOrnament(sink, false, sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), light));
            }
            if (depth != null) {
                var sprite = atlas.getSprite(depth);
                ItemGeometry.solid(collector.order(1), poses, sprite.atlasLocation(), true, sink ->
                    WandGeometry.focusDepth(sink, false, sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), light));
            }
            int color = (depth == null ? 0xF2000000 : 0x99000000) | (focusItem.getFocusColor(focus) & 0xFFFFFF);
            float ticks = net.minecraft.util.Util.getMillis() / 50.0F;
            int glow = (int) (205.0F + Mth.sin(ticks / 3.0F) * 10.0F);
            int focusLight = Math.max(light & 0xFFFF, glow) | (light & 0xFFFF0000);
            ItemGeometry.solid(collector.order(2), poses, Thaumcraft.id("textures/model/wand.png"), true,
                sink -> WandGeometry.focus(sink, false, color, focusLight));
        }
        poses.popPose();
        var target = reach > 0 ? WandKeys.TARGET.get(player.getUseItem()) : null;
        if (target != null
            && player.level().getBlockEntity(target) instanceof NodeBlockEntity node && node.getDrainEntity() == player) {
            var camera = minecraft.gameRenderer.mainCamera();
            Vec3 towardsNode = Vec3.atCenterOf(target).subtract(camera.position());
            var view = new org.joml.Quaternionf(camera.rotation()).conjugate();
            var endpoint = view.transform(new Vector3f((float) towardsNode.x, (float) towardsNode.y, (float) towardsNode.z));
            WorldRendering.submitFloatyLine(collector, poses, BlockPos.ZERO, wrist.x, wrist.y, wrist.z,
                endpoint.x, endpoint.y, endpoint.z, node.getTargetColor(), Math.min(useTicks, 10) / 10, System.nanoTime());
        }
    }
    private static boolean isBracelet(net.minecraft.world.item.ItemStack stack) {
        return Wands.isWand(stack) && Wands.getParts(stack).rod().startsWith("tb_bracelet_");
    }
}

