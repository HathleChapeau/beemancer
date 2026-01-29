/**
 * ============================================================
 * [EssenceItem.java]
 * Description: Item d'essence pour améliorer les perks des abeilles
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation           |
 * |---------------------|----------------------|-----------------------|
 * | EssenceType         | Type d'essence       | Définition            |
 * | EssenceLevel        | Niveau d'essence     | Définition            |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - BeemancerItems.java (enregistrement)
 * - Essence Extractor (production)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.item.essence;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Item représentant une essence utilisable pour améliorer les perks des abeilles.
 */
public class EssenceItem extends Item {

    private final EssenceType type;
    private final EssenceLevel level;

    public EssenceItem(Properties properties, EssenceType type, EssenceLevel level) {
        super(properties);
        this.type = type;
        this.level = level;
    }

    public EssenceType getEssenceType() {
        return type;
    }

    public EssenceLevel getEssenceLevel() {
        return level;
    }

    /**
     * Retourne le niveau numérique (1-4) de l'essence.
     */
    public int getLevelValue() {
        return level.getValue();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        // Afficher le type et le niveau avec étoiles
        tooltip.add(Component.translatable("item.beemancer.essence.type." + type.getSerializedName())
                .withStyle(type.getColor()));
        tooltip.add(Component.literal(formatStars(level.getValue()))
                .withStyle(ChatFormatting.GRAY));
    }

    private String formatStars(int level) {
        return "\u2605".repeat(Math.max(0, level)) + "\u2606".repeat(Math.max(0, 4 - level));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // Les essences perfect brillent
        return level == EssenceLevel.PERFECT;
    }

    /**
     * Types d'essences disponibles.
     */
    public enum EssenceType implements StringRepresentable {
        DROP("drop", ChatFormatting.GOLD),
        SPEED("speed", ChatFormatting.AQUA),
        FORAGING("foraging", ChatFormatting.GREEN),
        TOLERANCE("tolerance", ChatFormatting.RED),
        DIURNAL("diurnal", ChatFormatting.DARK_AQUA),
        NOCTURNAL("nocturnal", ChatFormatting.DARK_PURPLE),
        INSOMNIA("insomnia", ChatFormatting.LIGHT_PURPLE);

        public static final StringRepresentable.EnumCodec<EssenceType> CODEC =
                StringRepresentable.fromEnum(EssenceType::values);

        private final String name;
        private final ChatFormatting color;

        EssenceType(String name, ChatFormatting color) {
            this.name = name;
            this.color = color;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public ChatFormatting getColor() {
            return color;
        }

        @Nullable
        public static EssenceType byName(String name) {
            for (EssenceType type : values()) {
                if (type.name.equals(name)) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * Niveaux d'essences (correspond aux niveaux de perks 1-4).
     */
    public enum EssenceLevel implements StringRepresentable {
        LESSER(1),
        NORMAL(2),
        GREATER(3),
        PERFECT(4);

        public static final StringRepresentable.EnumCodec<EssenceLevel> CODEC =
                StringRepresentable.fromEnum(EssenceLevel::values);

        private final int value;

        EssenceLevel(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }

        @Nullable
        public static EssenceLevel byValue(int value) {
            for (EssenceLevel level : values()) {
                if (level.value == value) {
                    return level;
                }
            }
            return null;
        }

        @Nullable
        public static EssenceLevel byName(String name) {
            for (EssenceLevel level : values()) {
                if (level.getSerializedName().equals(name)) {
                    return level;
                }
            }
            return null;
        }
    }
}
