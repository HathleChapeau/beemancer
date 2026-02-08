/**
 * ============================================================
 * [IAnimatable.java]
 * Description: Interface pour les renderers qui utilisent le systeme d'animation
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | AnimationController           | Controller retourne  | getAnimationController()       |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - Tout renderer qui veut utiliser le systeme d'animation
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.animation;

/**
 * Interface marquant un renderer comme animable.
 * Fournit un AnimationController pour gerer les animations nommees et sequences.
 *
 * Usage typique dans un renderer:
 * <pre>
 * public class MyRenderer implements BlockEntityRenderer&lt;MyBE&gt;, IAnimatable {
 *     private final AnimationController controller = new AnimationController();
 *
 *     public MyRenderer(Context ctx) {
 *         controller.createAnimation("spin", RotateAnimation.builder()
 *             .axis(Axis.YP).startAngle(0).endAngle(360)
 *             .duration(40).timingEffect(TimingEffect.LOOP)
 *             .build());
 *     }
 *
 *     public void render(...) {
 *         controller.tick(gameTime + partialTick);
 *         poseStack.pushPose();
 *         controller.applyAnimation("spin", poseStack);
 *         // render...
 *         poseStack.popPose();
 *     }
 *
 *     public AnimationController getAnimationController() {
 *         return controller;
 *     }
 * }
 * </pre>
 */
public interface IAnimatable {
    AnimationController getAnimationController();
}
