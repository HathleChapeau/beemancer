/**
 * ============================================================
 * [RailgunBeamRenderer.java]
 * Description: Rendu world-space du beam railgun (apparait puis retrecit en epaisseur)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | RenderLevelStageEvent   | Hook rendu world     | Rendu apres translucents       |
 * | RailgunItem             | Detection item actif | Constantes charge              |
 * | MagazineData            | Lecture fluide       | Couleur beam par fluide        |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement event bus)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.item.magazine.MagazineData;
import com.chapeau.apica.common.item.tool.RailgunItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Renderer world-space pour le beam du Railgun.
 * Le beam apparait instantanement a pleine epaisseur (+ flash initial x2.5),
 * puis retrecit lineairement sur 10 ticks. 2 quads billboard croises (X shape).
 * Couleur du beam determinee par le fluide du magazine.
 */
@OnlyIn(Dist.CLIENT)
public class RailgunBeamRenderer {

    private static final ResourceLocation BEAM_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/item/artifacts/mining_laser_beam.png");

    private static final float CORE_HALF_WIDTH = 0.06f;
    private static final float GLOW_HALF_WIDTH = 0.20f;
    private static final int BEAM_DURATION_TICKS = 10;

    private static long fireGameTime = -1;
    private static Vec3 beamOrigin = Vec3.ZERO;
    private static Vec3 beamDestination = Vec3.ZERO;
    private static float beamR = 1f, beamG = 1f, beamB = 1f;

    private static boolean wasUsing = false;
    private static int storedUseTicks = 0;
    private static ItemStack storedStack = ItemStack.EMPTY;
    private static InteractionHand storedHand = InteractionHand.MAIN_HAND;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        boolean isUsing = player.isUsingItem()
            && player.getUseItem().getItem() instanceof RailgunItem;

        if (wasUsing && !isUsing && storedUseTicks > 0) {
            ItemStack currentItem = player.getItemInHand(storedHand);
            if (currentItem.getItem() instanceof RailgunItem) {
                triggerBeam(player, mc);
            }
            storedUseTicks = 0;
        }

        if (isUsing) {
            storedUseTicks = player.getTicksUsingItem();
            storedStack = player.getUseItem().copy();
            storedHand = player.getUsedItemHand();
        }
        wasUsing = isUsing;

        if (fireGameTime < 0) return;

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        float elapsed = (player.level().getGameTime() - fireGameTime) + partialTick;
        if (elapsed >= BEAM_DURATION_TICKS) {
            fireGameTime = -1;
            return;
        }

        float progress = elapsed / BEAM_DURATION_TICKS;
        float widthMult;
        if (progress < 0.15f) {
            widthMult = 2.5f - (progress / 0.15f) * 1.5f;
        } else {
            widthMult = 1.0f - (progress - 0.15f) / 0.85f;
        }

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        renderBeamQuads(poseStack, bufferSource, camPos,
            CORE_HALF_WIDTH * widthMult, beamR, beamG, beamB, 1.0f);
        renderBeamQuads(poseStack, bufferSource, camPos,
            GLOW_HALF_WIDTH * widthMult, beamR, beamG, beamB, 0.4f);
        bufferSource.endBatch();

        poseStack.popPose();
    }

    private static void triggerBeam(Player player, Minecraft mc) {
        fireGameTime = player.level().getGameTime();

        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 endPos = eyePos.add(look.scale(RailgunItem.MAX_RANGE));

        BlockHitResult blockHit = player.level().clip(new ClipContext(
            eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        beamDestination = blockHit.getType() == HitResult.Type.BLOCK
            ? blockHit.getLocation() : endPos;

        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = look.cross(up);
        if (right.lengthSqr() < 0.001) right = new Vec3(1, 0, 0);
        right = right.normalize();

        boolean isMainRight = mc.options.mainHand().get() == HumanoidArm.RIGHT;
        boolean isRightSide = (storedHand == InteractionHand.MAIN_HAND) == isMainRight;
        float sideSign = isRightSide ? 1.0f : -1.0f;

        if (mc.options.getCameraType() == CameraType.FIRST_PERSON) {
            Camera camera = mc.gameRenderer.getMainCamera();
            beamOrigin = camera.getPosition()
                .add(right.scale(sideSign * 0.35))
                .add(0, -0.15, 0)
                .add(look.scale(0.6));
        } else {
            beamOrigin = player.position().add(0, 1.2, 0)
                .add(right.scale(sideSign * 0.4))
                .add(look.scale(1.2));
        }

        String fluidId = storedStack.isEmpty() ? "" : MagazineData.getFluidId(storedStack);
        if (fluidId.contains("nectar")) { beamR = 0.7f; beamG = 0.3f; beamB = 1.0f; }
        else if (fluidId.contains("royal_jelly")) { beamR = 1.0f; beamG = 1.0f; beamB = 0.9f; }
        else { beamR = 1.0f; beamG = 0.85f; beamB = 0.3f; }
    }

    private static void renderBeamQuads(PoseStack poseStack, MultiBufferSource buffer,
                                         Vec3 camPos, float halfWidth,
                                         float r, float g, float b, float a) {
        if (halfWidth <= 0.001f) return;
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(BEAM_TEXTURE));
        PoseStack.Pose pose = poseStack.last();

        Vec3 beamDir = beamDestination.subtract(beamOrigin);
        double length = beamDir.length();
        if (length < 0.01) return;
        Vec3 dir = beamDir.normalize();

        Vec3 beamCenter = beamOrigin.add(beamDestination).scale(0.5);
        Vec3 toCam = camPos.subtract(beamCenter).normalize();

        Vec3 perp1 = dir.cross(toCam).normalize().scale(halfWidth);
        Vec3 perp2 = dir.cross(perp1.normalize()).normalize().scale(halfWidth);
        float vLen = (float) length;

        emitQuad(vc, pose, beamOrigin, beamDestination, perp1, r, g, b, a, vLen);
        emitQuad(vc, pose, beamOrigin, beamDestination, perp2, r, g, b, a, vLen);
    }

    private static void emitQuad(VertexConsumer vc, PoseStack.Pose pose,
                                  Vec3 origin, Vec3 destination, Vec3 perpOffset,
                                  float r, float g, float b, float a, float vLen) {
        int light = 15728880;
        int overlay = OverlayTexture.NO_OVERLAY;
        Vec3 norm = perpOffset.normalize();
        Vec3 p0 = origin.add(perpOffset), p1 = origin.subtract(perpOffset);
        Vec3 p2 = destination.subtract(perpOffset), p3 = destination.add(perpOffset);

        vc.addVertex(pose, (float) p0.x, (float) p0.y, (float) p0.z).setColor(r, g, b, a)
            .setUv(0, 0).setOverlay(overlay).setLight(light)
            .setNormal(pose, (float) norm.x, (float) norm.y, (float) norm.z);
        vc.addVertex(pose, (float) p1.x, (float) p1.y, (float) p1.z).setColor(r, g, b, a)
            .setUv(1, 0).setOverlay(overlay).setLight(light)
            .setNormal(pose, (float) norm.x, (float) norm.y, (float) norm.z);
        vc.addVertex(pose, (float) p2.x, (float) p2.y, (float) p2.z).setColor(r, g, b, a)
            .setUv(1, vLen).setOverlay(overlay).setLight(light)
            .setNormal(pose, (float) norm.x, (float) norm.y, (float) norm.z);
        vc.addVertex(pose, (float) p3.x, (float) p3.y, (float) p3.z).setColor(r, g, b, a)
            .setUv(0, vLen).setOverlay(overlay).setLight(light)
            .setNormal(pose, (float) norm.x, (float) norm.y, (float) norm.z);
    }
}
