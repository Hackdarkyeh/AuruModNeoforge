package aurum.aurum.entity.AurumVillager;

import aurum.aurum.Aurum;
import aurum.aurum.init.ModItems;
import aurum.aurum.item.Aurelite_ingot;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * El Banquero es el cambista: cambia materiales de aurelite por yanyitos y los yanyitos por fragmentos,
 * usando la GUI de comercio de vanilla (MerchantMenu / MerchantScreen).
 * Las ofertas se construyen al abrir, según el inventario del jugador (los lingotes tienen pureza propia).
 */
@EventBusSubscriber(modid = Aurum.MODID, bus = EventBusSubscriber.Bus.GAME)
public class AurumBankerTrading {

    // ── Tarifas (ajustables) ─────────────────────────────────────────────────
    /** Pureza mínima que acepta el banquero (por debajo, el lingote no se compra). */
    public static final int MIN_PURITY = 50;
    /** Pureza que se considera "completa" (100 %). [COMPROBAR] la escala real de la pureza de tus lingotes */
    public static final int FULL_PURITY = 100;
    /** Yanyitos que paga por un lingote de pureza completa; a menos pureza paga proporcionalmente menos. */
    public static final int YANYITOS_PER_INGOT_AT_FULL_PURITY = 8;
    /** Yanyitos que paga por un fragmento (los fragmentos no tienen pureza). */
    public static final int YANYITOS_PER_FRAGMENT = 1;
    /** Yanyitos que cuesta un fragmento al comprarlo (más caro que lo que paga: evita ganar dinero cambiando de ida y vuelta). */
    public static final int YANYITOS_COST_PER_FRAGMENT = 2;
    /** Veces que se puede repetir una oferta (en la práctica, sin límite). */
    private static final int MAX_USES = 9999;

    private AurumBankerTrading() {}

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getTarget() instanceof AurumVillagerEntity villager)) {
            return;
        }
        if (villager.getProfession() != AurumVillagerEntity.Profession.BANKER || villager.isHostileForm()) {
            return;
        }

        Level level = event.getLevel();
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
        event.setCanceled(true);
        if (level.isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        BankerMerchant merchant = new BankerMerchant(player, priceMultiplier(villager, player));
        if (merchant.skippedIngots > 0) {
            player.displayClientMessage(
                    Component.translatable("aurum.banker.low_purity", merchant.skippedIngots, MIN_PURITY), true);
        }
        // [COMPROBAR] openTradingScreen existe como método por defecto de Merchant en 1.21.1
        merchant.setTradingPlayer(player);
        merchant.openTradingScreen(player, Component.translatable("aurum.banker.title"), 1);
    }

    /** Recargo por reputación del pueblo del banquero (1.0 si no tiene ninguno). */
    public static double priceMultiplier(AurumVillagerEntity villager, ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return 1.0;
        }
        AurumVillageData data = AurumVillageData.get(level);
        UUID villageId = villager.getVillageId();
        if (villageId == null) {
            AurumVillageData.Village nearest = data.findNearest(villager.blockPosition());
            if (nearest == null) {
                return 1.0;
            }
            villageId = nearest.id;
        }
        return data.priceMultiplier(villageId, player.getUUID());
    }

    /** Comerciante temporal: solo existe mientras dura la ventana de comercio, en el servidor. */
    private static class BankerMerchant implements Merchant {
        private final MerchantOffers offers = new MerchantOffers();
        private int xp = 0;
        @Nullable
        private Player tradingPlayer;
        /** Lingotes del inventario que no llegan a la pureza mínima. */
        int skippedIngots = 0;

        BankerMerchant(ServerPlayer player, double multiplier) {
            buildOffers(player, multiplier);
        }

        private void buildOffers(ServerPlayer player, double multiplier) {
            // 1) Lingotes del inventario: una oferta por cada pureza distinta (los lingotes no se apilan)
            Map<CustomData, Integer> byData = new LinkedHashMap<>();
            for (ItemStack stack : player.getInventory().items) {
                if (!stack.is(ModItems.AURELITE_INGOT.get())) {
                    continue;
                }
                int purity = Aurelite_ingot.getPurity(stack);
                if (purity < MIN_PURITY) {
                    skippedIngots++;
                    continue;
                }
                CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                byData.put(data, purity);
            }
            List<Map.Entry<CustomData, Integer>> sorted = new ArrayList<>(byData.entrySet());
            sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue())); // la pureza más alta primero

            for (Map.Entry<CustomData, Integer> entry : sorted) {
                int yanyitos = (int) Math.floor(
                        YANYITOS_PER_INGOT_AT_FULL_PURITY * ((double) entry.getValue() / FULL_PURITY) / multiplier);
                if (yanyitos <= 0) {
                    continue;
                }
                // [COMPROBAR] ItemCost.withComponents(...) y DataComponentPredicate.Builder.expect(...)
                ItemCost cost = new ItemCost(ModItems.AURELITE_INGOT.get())
                        .withComponents(b -> b.expect(DataComponents.CUSTOM_DATA, entry.getKey()));
                offers.add(offer(cost, new ItemStack(ModItems.YANYITO.get(), yanyitos)));
            }

            // 2) Fragmentos -> yanyito (con recargo por reputación, siempre redondeando hacia arriba)
            int fragmentsPerYanyito = (int) Math.ceil(multiplier / YANYITOS_PER_FRAGMENT);
            offers.add(offer(new ItemCost(ModItems.AURELITE_FRAGMENT.get(), fragmentsPerYanyito),
                    new ItemStack(ModItems.YANYITO.get(), 1)));

            // 3) Yanyitos -> fragmento
            int costOfFragment = (int) Math.ceil(YANYITOS_COST_PER_FRAGMENT * multiplier);
            offers.add(offer(new ItemCost(ModItems.YANYITO.get(), costOfFragment),
                    new ItemStack(ModItems.AURELITE_FRAGMENT.get(), 1)));
        }

        private static MerchantOffer offer(ItemCost cost, ItemStack result) {
            // sin experiencia, sin multiplicador de demanda
            return new MerchantOffer(cost, Optional.empty(), result, 0, MAX_USES, 0, 0.0f);
        }

        // ── Implementación de Merchant ───────────────────────────────────────
        // [COMPROBAR] la lista exacta de métodos abstractos de Merchant en 1.21.1: el IDE te dirá si falta alguno.
        @Override
        public void setTradingPlayer(@Nullable Player player) {
            this.tradingPlayer = player;
        }

        @Nullable
        @Override
        public Player getTradingPlayer() {
            return tradingPlayer;
        }

        @Override
        public MerchantOffers getOffers() {
            return offers;
        }

        @Override
        public void overrideOffers(MerchantOffers newOffers) {
            // las ofertas se construyen siempre aquí
        }

        @Override
        public void notifyTrade(MerchantOffer offer) {
            // sin experiencia ni reabastecimiento
        }

        @Override
        public void notifyTradeUpdated(ItemStack stack) {
            // nada
        }

        @Override
        public int getVillagerXp() {
            return xp;
        }

        @Override
        public void overrideXp(int newXp) {
            this.xp = newXp;
        }

        @Override
        public boolean showProgressBar() {
            return false;
        }

        @Override
        public SoundEvent getNotifyTradeSound() {
            return SoundEvents.VILLAGER_YES;
        }

        @Override
        public boolean isClientSide() {
            return false;
        }
    }
}