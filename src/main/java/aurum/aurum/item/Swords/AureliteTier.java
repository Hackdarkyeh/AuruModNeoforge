package aurum.aurum.item.Swords;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;


import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;

import java.util.List;

public class AureliteTier implements Tier {
    // Stats base (no usaremos getUses() porque la durabilidad viene de la energía)
    private static final int BASE_DURABILITY = 2500; // Solo como referencia
    private static final float BASE_ATTACK_DAMAGE = 8.0f; // Doble que netherite
    private static final int MINING_SPEED = 12;
    private static final int ENCHANTABILITY = 15;

    public AureliteTier() {
        super();
    }

    @Override
    public int getUses() {
        return BASE_DURABILITY; // Esto es ignorado, usamos energía oscura
    }

    @Override
    public float getSpeed() {
        return MINING_SPEED; // Velocidad de minería (no relevante para espadas)
    }

    @Override
    public float getAttackDamageBonus() {
        return BASE_ATTACK_DAMAGE; // Daño base adicional
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return BlockTags.INCORRECT_FOR_NETHERITE_TOOL; // Mismo que netherite
    }

    @Override
    public int getEnchantmentValue() {
        return ENCHANTABILITY; // Mismo que netherite
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.NETHERITE_INGOT); // Reparación con netherite
    }

    // Método nuevo en 1.21+ para propiedades de herramienta
    @Override
    public Tool createToolProperties(TagKey<Block> blocks) {
        return new Tool(
                List.of(Tool.Rule.minesAndDrops(blocks, getSpeed())),
                getUses(),
                (int) getSpeed()
        );
    }
}