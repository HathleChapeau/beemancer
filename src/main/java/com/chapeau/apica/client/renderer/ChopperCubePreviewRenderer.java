/**
 * ============================================================
 * [ChopperCubePreviewRenderer.java]
 * Description: Renderer pour le glow ambre, abeilles orbitantes et particules rune du Chopper Cube
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | ChopperCubeItem         | Scan buches          | findConnectedLogs()            |
 * | ChopperCubeLockHelper   | Etat lock            | Positions verrouillees + timing|
 * | ChopperCubeChoppingState| Warmup constant      | Delay avant particules         |
 * | AnimationTimer          | Temps fluide         | Pulsation + orbite             |
 * | BeeModel                | Modele abeille       | Rendu mini-abeilles            |
 * | ParticleEmitter         | Spawn particules     | Runes client-side configurables|
 * | ApicaParticles          | Registre particules  | RUNE particle type             |
 * | RenderLevelStageEvent   | Hook rendu           | Dessin outlines + abeilles     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement event)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer;

import com.chapeau.apica.client.animation.AnimationTimer;
import com.chapeau.apica.client.particle.ParticleEmitter;
import com.chapeau.apica.common.item.tool.ChopperCubeChoppingState;
import com.chapeau.apica.common.item.tool.ChopperCubeItem;
import com.chapeau.apica.common.item.tool.ChopperCubeLockHelper;
import com.chapeau.apica.core.registry.ApicaParticles;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.BeeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.Comparator;
import java.util.List;

/**
 * Dessine des contours ambre pulsants autour des buches detectees par le Chopper Cube.
 * En mode destruction, 2 mini-abeilles orbitent autour du bloc en cours.
 * Emet des particules rune au niveau du cube dans la main pendant la destruction active.
 * Les positions des abeilles sont lerpees vers les points d'orbite pour un mouvement fluide.
 */
@OnlyIn(Dist.CLIENT)
public class ChopperCubePreviewRenderer {

    // Couleur ambre/miel
    private static final float R = 1.0f;
    private static final float G = 0.78f;
    private static final float B = 0.15f;

    // Abeilles orbitantes
    private static final float BEE_SCALE = 0.45f;
    private static final double ORBIT_RADIUS = 1.65;
    private static final double ORBIT_SPEED = 0.40;
    private static final double LERP_FACTOR = 0.05;
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final ResourceLocation BEE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/bee/bee.png");

    // Particules rune: intervalle de spawn (en game ticks)
    private static final int RUNE_PARTICLE_INTERVAL = 3;

    // Particules rune: scale reduit
    private static final float RUNE_SCALE = 0.02f;

    // Offset de la main: hauteur relative aux pieds du joueur
    private static final double HAND_HEIGHT = 0.8;
    // Offset de la main: distance en avant du joueur
    private static final double HAND_FORWARD = 0.3;
    // Offset de la main: distance laterale
    private static final double HAND_SIDE = 0.35;

    // Cache live (evite recalcul chaque frame)
    private static BlockPos cachedTargetPos = null;
    private static List<BlockPos> cachedPositions = List.of();

    // BeeModel lazy-init
    private static BeeModel<?> beeModel;

    // Positions lerpees des abeilles
    private static Vec3 bee1Pos = null;
    private static Vec3 bee2Pos = null;

    // Dernier tick ou on a emis des particules (throttle)
    private static long lastParticleTick = -1;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        boolean hasItem = player.getMainHandItem().getItem() instanceof ChopperCubeItem
                       || player.getOffhandItem().getItem() instanceof ChopperCubeItem;

        if (!hasItem) {
            ChopperCubeLockHelper.reset();
            cachedTargetPos = null;
            cachedPositions = List.of();
            bee1Pos = null;
            bee2Pos = null;
            return;
        }

        boolean chopping = ChopperCubeLockHelper.isLocked();
        List<BlockPos> positions;

        if (chopping) {
            positions = ChopperCubeLockHelper.getLockedPositions();
        } else {
            positions = computeLivePositions(mc, player);
        }

        if (positions.isEmpty()) return;

        // Filtrer les blocs deja detruits
        Level level = player.level();
        List<BlockPos> remaining = positions.stream()
                .filter(p -> !level.getBlockState(p).isAir())
                .toList();

        if (remaining.isEmpty()) {
            if (chopping) ChopperCubeLockHelper.reset();
            bee1Pos = null;
            bee2Pos = null;
            return;
        }

        renderOutlines(event, mc, remaining);

        if (chopping) {
            // Meme ordre que la destruction serveur: Y desc, X asc, Z asc
            BlockPos target = remaining.stream()
                    .min(Comparator.<BlockPos>comparingInt(BlockPos::getY).reversed()
                            .thenComparingInt(BlockPos::getX)
                            .thenComparingInt(BlockPos::getZ))
                    .orElse(null);
            if (target != null) {
                renderOrbitingBees(event, mc, target);
            }

            // Particules rune apres le warmup (quand les abeilles sont en haut)
            trySpawnRuneParticles(player, level);
        }
    }

    /**
     * Emet des particules rune au niveau de la main tenant le cube.
     * Ne spawne qu'apres le warmup (20 ticks) et avec throttle (toutes les 3 ticks).
     */
    private static void trySpawnRuneParticles(Player player, Level level) {
        long lockTime = ChopperCubeLockHelper.getLockGameTime();
        if (lockTime < 0) return;

        long currentTime = level.getGameTime();
        long elapsed = currentTime - lockTime;

        // Attendre la fin du warmup avant d'emettre
        if (elapsed < ChopperCubeChoppingState.WARMUP_TICKS) return;

        // Throttle: toutes les N ticks
        if (currentTime == lastParticleTick) return;
        if (currentTime % RUNE_PARTICLE_INTERVAL != 0) return;
        lastParticleTick = currentTime;

        // Calculer la position de la main tenant le cube
        Vec3 handPos = computeHandPosition(player);

        new ParticleEmitter(ApicaParticles.RUNE.get())
                .at(handPos.x, handPos.y, handPos.z)
                .count(1)
                .spread(0.05, 0.05, 0.05)
                .speed(0, 0.015, 0)
                .scale(RUNE_SCALE)
                .fadeOut()
                .fullBright()
                .spawn(level);
    }

    /**
     * Calcule la position world de la main tenant le Chopper Cube.
     * Prend en compte le yaw du joueur et la main (principale ou secondaire).
     */
    private static Vec3 computeHandPosition(Player player) {
        float radYaw = (float) Math.toRadians(player.getYRot());

        // Vecteur avant (plan XZ, ignorant le pitch)
        double fwdX = -Math.sin(radYaw);
        double fwdZ = Math.cos(radYaw);

        // Vecteur droit (perpendiculaire au forward dans le plan XZ)
        double rightX = -Math.cos(radYaw);
        double rightZ = -Math.sin(radYaw);

        // Determiner si le cube est en main principale (droite) ou secondaire (gauche)
        boolean isMainHand = player.getMainHandItem().getItem() instanceof ChopperCubeItem;
        double sideSign = isMainHand ? 1.0 : -1.0;

        return new Vec3(
                player.getX() + fwdX * HAND_FORWARD + rightX * sideSign * HAND_SIDE,
                player.getY() + HAND_HEIGHT,
                player.getZ() + fwdZ * HAND_FORWARD + rightZ * sideSign * HAND_SIDE
        );
    }

    /**
     * Calcule les positions en mode live (curseur sur une buche).
     */
    private static List<BlockPos> computeLivePositions(Minecraft mc, Player player) {
        HitResult hitResult = mc.hitResult;
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
            return List.of();
        }

        BlockHitResult blockHit = (BlockHitResult) hitResult;
        BlockPos targetPos = blockHit.getBlockPos();
        Level level = player.level();

        if (!level.getBlockState(targetPos).is(BlockTags.LOGS)) {
            return List.of();
        }

        if (targetPos.equals(cachedTargetPos)) {
            return cachedPositions;
        }

        List<BlockPos> result = ChopperCubeItem.findConnectedLogs(level, targetPos);
        cachedTargetPos = targetPos.immutable();
        cachedPositions = result;
        return result;
    }

    /**
     * Dessine des contours ambre epais avec alpha pulsante autour des buches.
     * Deux couches d'outline superposees pour l'epaisseur.
     */
    private static void renderOutlines(RenderLevelStageEvent event, Minecraft mc,
                                        List<BlockPos> positions) {
        PoseStack poseStack = event.getPoseStack();
        Vec3 camPos = event.getCamera().getPosition();
        float time = AnimationTimer.getRenderTime(
                event.getPartialTick().getGameTimeDeltaPartialTick(true));

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        var bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.lines());

        for (BlockPos pos : positions) {
            float alpha = 0.4f + 0.4f * (float) Math.sin(time * 0.12 + pos.getY() * 0.8);
            renderBlockOutline(poseStack, buffer, pos, alpha);
        }

        bufferSource.endBatch(RenderType.lines());
        poseStack.popPose();
    }

    /**
     * Dessine un contour ambre epais autour d'un bloc (2 couches superposees).
     */
    private static void renderBlockOutline(PoseStack poseStack, VertexConsumer buffer,
                                            BlockPos pos, float alpha) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();

        // Couche interieure
        double in = 0.002;
        LevelRenderer.renderLineBox(
            poseStack, buffer,
            x + in, y + in, z + in,
            x + 1 - in, y + 1 - in, z + 1 - in,
            R, G, B, alpha
        );

        // Couche exterieure (elargie, plus transparente = effet glow)
        double out = 0.015;
        LevelRenderer.renderLineBox(
            poseStack, buffer,
            x - out, y - out, z - out,
            x + 1 + out, y + 1 + out, z + 1 + out,
            R, G, B, alpha * 0.4f
        );
    }

    /**
     * Rend 2 petites abeilles dont les positions sont lerpees vers des points d'orbite.
     * Les points d'orbite tournent autour du bloc, les abeilles suivent avec un lerp a 0.75.
     * La rotation est directement basee sur l'angle d'orbite (pas lerpee).
     */
    private static void renderOrbitingBees(RenderLevelStageEvent event, Minecraft mc,
                                            BlockPos targetPos) {
        PoseStack poseStack = event.getPoseStack();
        Vec3 camPos = event.getCamera().getPosition();
        Vec3 center = Vec3.atCenterOf(targetPos);
        float time = AnimationTimer.getRenderTime(
                event.getPartialTick().getGameTimeDeltaPartialTick(true));

        // 0.5 blocs au-dessus du centre du bloc cible
        double beeY = center.y + 0.5;

        BeeModel<?> model = getOrCreateBeeModel();
        var bufferSource = mc.renderBuffers().bufferSource();
        RenderType beeRenderType = RenderType.entityCutout(BEE_TEXTURE);
        VertexConsumer vc = bufferSource.getBuffer(beeRenderType);

        for (int i = 0; i < 2; i++) {
            double angle = time * ORBIT_SPEED + i * Math.PI;

            // Point cible sur l'orbite
            Vec3 target = new Vec3(
                center.x + Math.cos(angle) * ORBIT_RADIUS,
                beeY,
                center.z + Math.sin(angle) * ORBIT_RADIUS
            );

            // Lerp position vers le point cible
            Vec3 beePos;
            if (i == 0) {
                if (bee1Pos == null) bee1Pos = target;
                bee1Pos = lerpVec3(bee1Pos, target, LERP_FACTOR);
                beePos = bee1Pos;
            } else {
                if (bee2Pos == null) bee2Pos = target;
                bee2Pos = lerpVec3(bee2Pos, target, LERP_FACTOR);
                beePos = bee2Pos;
            }

            poseStack.pushPose();
            poseStack.translate(beePos.x - camPos.x, beePos.y - camPos.y, beePos.z - camPos.z);

            // Rotation basee directement sur l'angle d'orbite (pas lerpee)
            float yRot = (float) Math.toDegrees(angle) + 270;
            poseStack.mulPose(Axis.YP.rotationDegrees(-yRot));

            // Retourner le modele (convention Minecraft entity models)
            poseStack.mulPose(Axis.XP.rotationDegrees(180));

            poseStack.scale(BEE_SCALE, BEE_SCALE, BEE_SCALE);

            model.renderToBuffer(poseStack, vc, FULL_BRIGHT, OverlayTexture.NO_OVERLAY);

            poseStack.popPose();
        }

        bufferSource.endBatch(beeRenderType);
    }

    /**
     * Interpole lineairement entre deux Vec3.
     */
    private static Vec3 lerpVec3(Vec3 from, Vec3 to, double factor) {
        return new Vec3(
            from.x + (to.x - from.x) * factor,
            from.y + (to.y - from.y) * factor,
            from.z + (to.z - from.z) * factor
        );
    }

    /**
     * Cree ou retourne le BeeModel en cache.
     */
    private static BeeModel<?> getOrCreateBeeModel() {
        if (beeModel == null) {
            beeModel = new BeeModel<>(
                Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.BEE)
            );
        }
        return beeModel;
    }
}
