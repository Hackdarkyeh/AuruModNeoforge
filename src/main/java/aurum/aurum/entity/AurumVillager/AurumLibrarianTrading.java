package aurum.aurum.entity.AurumVillager;

import aurum.aurum.Aurum;
import aurum.aurum.init.ModAttachments;
import aurum.aurum.init.ModItems;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * El Bibliotecario reutiliza los comercios del bibliotecario de vanilla (VillagerTrades.TRADES), pero se paga
 * con la moneda del mod (yanyito, en lugar de esmeraldas), y además puede ofrecer libros propios del mod
 * (EXTRA_BOOKS). Tiene nivel y experiencia (AurumVillagerLevels) y se reabastece cada 12000 ticks.
 */
@EventBusSubscriber(modid = Aurum.MODID, bus = EventBusSubscriber.Bus.GAME)
public class AurumLibrarianTrading {

    // ── Valores ajustables ───────────────────────────────────────────────────
    /** Yanyitos por cada esmeralda de los comercios de vanilla (1 = mismo número que en vanilla). */
    private static final int EMERALD_TO_YANYITO = 1;
    /** Cada cuántos ticks se reabastecen las ofertas (vanilla: dos veces al día). */
    private static final long RESTOCK_INTERVAL = 12000L;
    /** Ofertas de vanilla que se añaden en cada nivel (escogidas al azar entre las de ese nivel). */
    private static final int VANILLA_OFFERS_PER_LEVEL = 2;

    /** Libro propio del mod que el bibliotecario ofrece al llegar a un nivel (se añade siempre, además de las de vanilla). */
    private record ExtraBook(int level, Supplier<ItemStack> result, int yanyitos, int maxUses, int xp) {}

    /**
     * Libros del mod. Está vacío hasta que decidas cuáles (objeto, nivel, precio). Ejemplo:
     *   new ExtraBook(2, () -> new ItemStack(ModItems.ALGUN_LIBRO.get()), 12, 4, 5)
     */
    private static final List<ExtraBook> EXTRA_BOOKS = List.of();

    private AurumLibrarianTrading() {}

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getTarget() instanceof AurumVillagerEntity villager)) {
            return;
        }
        if (villager.getProfession() != AurumVillagerEntity.Profession.LIBRARIAN || villager.isHostileForm()) {
            return;
        }

        Level level = event.getLevel();
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
        event.setCanceled(true);
        if (level.isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        LibrarianMerchant merchant = new LibrarianMerchant(villager, AurumBankerTrading.priceMultiplier(villager, player));
        if (merchant.getOffers().isEmpty()) {
            return; // sin comercios disponibles (no debería pasar)
        }
        // [COMPROBAR] openTradingScreen existe como método por defecto de Merchant en 1.21.1
        merchant.setTradingPlayer(player);
        merchant.openTradingScreen(player, Component.translatable("aurum.librarian.title"),
                AurumVillagerLevels.level(villager));
    }

    // ── Conversión de esmeraldas a yanyitos ──────────────────────────────────

    /** Cambia las esmeraldas de una oferta de vanilla (en lo que se paga y en lo que se recibe) por yanyitos. */
    private static MerchantOffer toYanyitos(MerchantOffer offer) {
        // [COMPROBAR] getters de MerchantOffer y constructor con demanda en 1.21.1
        ItemCost costA = convertCost(offer.getItemCostA());
        Optional<ItemCost> costB = offer.getItemCostB().map(AurumLibrarianTrading::convertCost);
        ItemStack result = offer.getResult();
        if (result.is(Items.EMERALD)) {
            result = new ItemStack(ModItems.YANYITO.get(), result.getCount() * EMERALD_TO_YANYITO);
        }
        return new MerchantOffer(costA, costB, result, offer.getUses(), offer.getMaxUses(), offer.getXp(),
                offer.getPriceMultiplier(), offer.getDemand());
    }

    private static ItemCost convertCost(ItemCost cost) {
        // [COMPROBAR] ItemCost es un record con item() y count()
        if (cost.item().value() == Items.EMERALD) {
            return new ItemCost(ModItems.YANYITO.get(), cost.count() * EMERALD_TO_YANYITO);
        }
        return cost;
    }

    /** Comerciante temporal que lee y guarda su estado en los attachments del aldeano. */
    private static class LibrarianMerchant implements Merchant {
        private final AurumVillagerEntity villager;
        private final MerchantOffers offers;
        private long lastRestock;
        @Nullable
        private Player tradingPlayer;

        LibrarianMerchant(AurumVillagerEntity villager, double priceMultiplier) {
            this.villager = villager;
            AurumLibrarianData stored = villager.getData(ModAttachments.LIBRARIAN_DATA);
            this.offers = stored.offers();
            this.lastRestock = stored.lastRestock();

            if (offers.isEmpty()) {
                addOffers(AurumVillagerLevels.level(villager));
            }
            restockIfNeeded();
            applyReputation(priceMultiplier);
            save();
        }

        private void save() {
            villager.setData(ModAttachments.LIBRARIAN_DATA, new AurumLibrarianData(offers, lastRestock));
        }

        /** Añade las ofertas de un nivel: algunas de vanilla escogidas al azar (pagadas en yanyitos) y los libros del mod. */
        private void addOffers(int forLevel) {
            // [COMPROBAR] VillagerTrades.TRADES y su tipo de clave en 1.21.1 (ResourceKey<VillagerProfession>)
            Int2ObjectMap<VillagerTrades.ItemListing[]> byLevel = VillagerTrades.TRADES.get(VillagerProfession.LIBRARIAN);
            if (byLevel != null) {
                VillagerTrades.ItemListing[] listings = byLevel.get(forLevel);
                if (listings != null) {
                    List<VillagerTrades.ItemListing> pool = new ArrayList<>(Arrays.asList(listings));
                    RandomSource random = villager.getRandom();
                    int added = 0;
                    while (added < VANILLA_OFFERS_PER_LEVEL && !pool.isEmpty()) {
                        VillagerTrades.ItemListing listing = pool.remove(random.nextInt(pool.size()));
                        MerchantOffer offer = listing.getOffer(villager, random);
                        if (offer != null) {
                            offers.add(toYanyitos(offer));
                            added++;
                        }
                    }
                }
            }

            for (ExtraBook book : EXTRA_BOOKS) {
                if (book.level() == forLevel) {
                    offers.add(new MerchantOffer(new ItemCost(ModItems.YANYITO.get(), book.yanyitos()),
                            Optional.empty(), book.result().get(), 0, book.maxUses(), book.xp(), 0.05F));
                }
            }
        }

        private void restockIfNeeded() {
            long now = villager.level().getGameTime();
            if (now < lastRestock || now - lastRestock >= RESTOCK_INTERVAL) {
                for (MerchantOffer offer : offers) {
                    offer.resetUses(); // [COMPROBAR] nombre del método
                }
                lastRestock = now;
            }
        }

        /** Recargo por haber matado aldeanos del pueblo: encarece lo que se paga con el primer objeto. */
        private void applyReputation(double multiplier) {
            for (MerchantOffer offer : offers) {
                offer.resetSpecialPriceDiff(); // [COMPROBAR] resetSpecialPriceDiff / addToSpecialPriceDiff
                if (multiplier > 1.0) {
                    int base = offer.getBaseCostA().getCount();
                    offer.addToSpecialPriceDiff((int) Math.ceil(base * (multiplier - 1.0)));
                }
            }
        }

        // ── Implementación de Merchant ───────────────────────────────────────
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
            offer.increaseUses();

            // Experiencia del aldeano: si sube de nivel, trae las ofertas de cada nivel nuevo
            int before = AurumVillagerLevels.level(villager);
            AurumVillagerLevels.addXp(villager, offer.getXp());
            int after = AurumVillagerLevels.level(villager);
            for (int newLevel = before + 1; newLevel <= after; newLevel++) {
                addOffers(newLevel);
            }

            // Un poco de experiencia para el jugador, como en vanilla
            if (offer.shouldRewardExp() && villager.level() instanceof ServerLevel serverLevel) {
                ExperienceOrb.award(serverLevel, villager.position().add(0.0, 0.5, 0.0),
                        3 + villager.getRandom().nextInt(4)); // [COMPROBAR] firma de ExperienceOrb.award
            }
            save();
        }

        @Override
        public void notifyTradeUpdated(ItemStack stack) {
            // nada
        }

        @Override
        public int getVillagerXp() {
            return AurumVillagerLevels.xp(villager);
        }

        @Override
        public void overrideXp(int newXp) {
            // la experiencia se gestiona en AurumVillagerLevels
        }

        @Override
        public boolean showProgressBar() {
            return true;
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
