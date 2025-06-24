package aurum.aurum.item.ArmorItem;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public record ArmorTierData(
        int tierLevel,       // value1 - Nivel numérico del tier (0=basic, 1=advanced, etc.)
        boolean isUpgradable // value2 - Si puede mejorarse aún más
) {
    // Puedes añadir métodos útiles aquí
    public Component getDisplayName() {
        return switch(tierLevel) {
            case 1 -> Component.literal("Basic").withStyle(ChatFormatting.GRAY);
            case 2 -> Component.literal("Advanced").withStyle(ChatFormatting.GOLD);
            case 3 -> Component.literal("Elite").withStyle(ChatFormatting.GOLD);
            default -> Component.literal("Unknown").withStyle(ChatFormatting.RED);
        };
    }
    public static ArmorTierData defaultData() {
        return new ArmorTierData(0, true);
    }
}
