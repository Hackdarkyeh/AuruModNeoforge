package aurum.aurum.item.Swords;


import aurum.aurum.energy.DarkEnergyItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;
import java.util.UUID;

public class AureliteSword extends DarkEnergyItems {
    // Atributos de la espada
    private static final UUID ATTACK_DAMAGE_MODIFIER = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    private static final int ATTACK_DAMAGE = 8; // Daño base (Netherite: 4)
    private static final float ATTACK_SPEED = -2.4f; // Velocidad de ataque estándar para espadas
    private static final AureliteTier aureliteTier = new AureliteTier();

    public AureliteSword() {
        super(
                new Properties()
                        .durability(2500)
                        .rarity(Rarity.EPIC)
                        .attributes(createAttributes(aureliteTier, ATTACK_DAMAGE, ATTACK_SPEED)),
                10000 // Capacidad base
        );
    }

    // Método para crear los atributos del arma
    public static ItemAttributeModifiers createAttributes(Tier tier, int attackDamage, float attackSpeed) {
        return ItemAttributeModifiers.builder()
                .add(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(
                                BASE_ATTACK_DAMAGE_ID, (double)((float)attackDamage + tier.getAttackDamageBonus()), AttributeModifier.Operation.ADD_VALUE
                        ),
                        EquipmentSlotGroup.MAINHAND
                )
                .add(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_ATTACK_SPEED_ID, (double)attackSpeed, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND
                )
                .build();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        DarkEnergyData data = getEnergyData(stack);

        // Barra visual de energía
        String energyBar = createEnergyBar(data.currentEnergy(), data.maxCapacity());

        tooltipComponents.add(Component.literal("Energía Oscura: " + energyBar)
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltipComponents.add(Component.literal("Daño: " + ATTACK_DAMAGE)
                .withStyle(ChatFormatting.GRAY));

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }



    private String createEnergyBar(int current, int max) {
        int bars = (int) ((double) current / max * 10);
        return "[" + "✦".repeat(bars) + "§7" + "✦".repeat(10 - bars) + "§r]";
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return false; // No se puede reparar normalmente
    }
}