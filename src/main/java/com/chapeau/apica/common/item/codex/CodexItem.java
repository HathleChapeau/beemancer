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
 * | ApicaSounds     | Sons d'interaction   | Son d'ouverture                |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ApicaItems (enregistrement)
 * - ApicaCreativeTabs (ajout à l'onglet créatif)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.codex;

import com.chapeau.apica.client.gui.screen.CodexScreen;
import com.chapeau.apica.core.registry.ApicaSounds;
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

        player.playSound(ApicaSounds.CODEX_OPEN.get(), 0.5F, 1.0F);

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private void openCodexScreen() {
        Minecraft.getInstance().setScreen(new CodexScreen());
    }
}
