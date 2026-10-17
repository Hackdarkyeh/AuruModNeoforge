package aurum.aurum.eventHandler;

import aurum.aurum.Soul.SoulData;
import aurum.aurum.client.gui.SoulModificationTableMenu.SoulAbilityData;
import aurum.aurum.init.ModAttachments;
import aurum.aurum.init.ModDataComponents;
import aurum.aurum.item.ArmorItem.ModArmorItem;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;

@EventBusSubscriber(modid = "aurum", bus = EventBusSubscriber.Bus.GAME)
public class SoulAbilityHandler {

    private static final int REAPPLY_INTERVAL = 20;

    private static final ResourceLocation ID_SPEED      = rl("soul_speed");
    private static final ResourceLocation ID_RESISTANCE = rl("soul_resistance");
    private static final ResourceLocation ID_JUMP       = rl("soul_jump");
    private static final ResourceLocation ID_REGEN      = rl("soul_regen");
    private static final ResourceLocation ID_MAGIC_DEF  = rl("soul_magic_def");

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath("aurum", path);
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        EquipmentSlot slot = event.getSlot();
        if (slot != EquipmentSlot.HEAD && slot != EquipmentSlot.CHEST &&
                slot != EquipmentSlot.LEGS && slot != EquipmentSlot.FEET) return;
        if (hasAuthorizedArmor(player)) applyAllAbilities(player);
        else removeAllModifiers(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (player.tickCount % REAPPLY_INTERVAL != 0) return;
        if (hasAuthorizedArmor(player)) applyAllAbilities(player);
    }

    public static boolean hasAuthorizedArmor(Player player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!isAuthorizedPiece(player.getItemBySlot(slot))) return false;
        }
        return true;
    }

    private static boolean isAuthorizedPiece(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof ModArmorItem) return true;
        if (stack.has(ModDataComponents.ARMOR_AUTHORIZED.get())) return true;
        return false;
    }

    public static void applyAllAbilities(Player player) {
        SoulData.PlayerSoulContainer container = player.getData(ModAttachments.ACTIVE_SOUL);
        ItemStack soulStack = container.soulStack();
        if (soulStack.isEmpty()) return;
        SoulData data = soulStack.get(ModDataComponents.SOUL_COMPONENT.get());
        if (data == null) return;
        removeAllModifiers(player);
        for (SoulAbilityData ability : data.abilities()) {
            if (ability.hasAbility() && ability.abilityId() != null) {
                applyAbility(player, ability.abilityId().getPath());
            }
        }
    }

    private static void applyAbility(Player player, String abilityId) {
        switch (abilityId) {
            case "expansion_super_speed" ->
                    addModifier(player, Attributes.MOVEMENT_SPEED, ID_SPEED,
                            0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            case "expansion_damage_resistance" ->
                    addModifier(player, Attributes.KNOCKBACK_RESISTANCE, ID_RESISTANCE,
                            0.5, AttributeModifier.Operation.ADD_VALUE);
            case "expansion_high_jump" ->
                    addModifier(player, Attributes.JUMP_STRENGTH, ID_JUMP,
                            0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            case "expansion_regeneration" ->
                    addModifier(player, Attributes.MAX_HEALTH, ID_REGEN,
                            4.0, AttributeModifier.Operation.ADD_VALUE);
            case "expansion_fire_immune" -> player.setRemainingFireTicks(-1);
            case "expansion_lava_immune" -> player.setRemainingFireTicks(-1);
            case "expansion_magic_shield" ->
                    addModifier(player, Attributes.ARMOR_TOUGHNESS, ID_MAGIC_DEF,
                            4.0, AttributeModifier.Operation.ADD_VALUE);
            case "expansion_dash", "expansion_explosion" -> {}
        }
    }

    public static void removeAllModifiers(Player player) {
        removeModifier(player, Attributes.MOVEMENT_SPEED,       ID_SPEED);
        removeModifier(player, Attributes.KNOCKBACK_RESISTANCE, ID_RESISTANCE);
        removeModifier(player, Attributes.JUMP_STRENGTH,        ID_JUMP);
        removeModifier(player, Attributes.MAX_HEALTH,           ID_REGEN);
        removeModifier(player, Attributes.ARMOR_TOUGHNESS,      ID_MAGIC_DEF);
    }

    private static void addModifier(Player player,
                                    Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                    ResourceLocation id, double value, AttributeModifier.Operation operation) {
        var instance = player.getAttribute(attribute);
        if (instance == null) return;
        if (instance.getModifier(id) != null) return; // ya existe, no duplicar
        instance.addPermanentModifier(new AttributeModifier(id, value, operation));
    }

    private static void removeModifier(Player player,
                                       Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                       ResourceLocation id) {
        var instance = player.getAttribute(attribute);
        if (instance == null) return;
        instance.removeModifier(id);
    }
}