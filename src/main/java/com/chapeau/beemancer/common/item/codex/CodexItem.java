/**
 * ============================================================
 * [CodexItem.java]
 * Description: Item livre qui ouvre le Codex GUI
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexScreen         | GUI du codex         | Ouverture côté client          |
 * | BeemancerSounds     | Sons d'interaction   | Son d'ouverture                |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - BeemancerItems (enregistrement)
 * - BeemancerCreativeTabs (ajout à l'onglet créatif)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.item.codex;

import com.chapeau.beemancer.client.gui.screen.CodexScreen;
import com.chapeau.beemancer.core.registry.BeemancerSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CodexItem extends Item {

    public CodexItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            openCodexScreen();
        }

        player.playSound(BeemancerSounds.CODEX_OPEN.get(), 0.5F, 1.0F);

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private void openCodexScreen() {
        Minecraft.getInstance().setScreen(new CodexScreen());
    }
}
