package aurum.aurum.structures.utilities;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Un HolderSet especial que contiene un HolderSet base con las entradas originales y utiliza un segundo HolderSet como filtro sobre esas entradas.
 * Puedes copiar y pegar esta clase en tu proyecto. No necesitas hacer cambios aquí.
 * Simplemente úsala en el codec de tu estructura para reemplazar el campo HolderSet de biomas. Consulta OceanStructures.java para ver un ejemplo de uso.
 */
public class FilterHolderSet<T> implements HolderSet<T> {

    /**
     * Codec para serializar/deserializar FilterHolderSet.
     * Permite definir el conjunto base y el filtro en la configuración.
     */
    public static <T> MapCodec<FilterHolderSet<T>> codec(ResourceKey<? extends Registry<T>> registryKey, Codec<Holder<T>> holderCodec, boolean forceList) {
        return RecordCodecBuilder.mapCodec(
                builder -> builder
                        .group(
                                HolderSetCodec.create(registryKey, holderCodec, forceList).fieldOf("base").forGetter(FilterHolderSet::base),
                                HolderSetCodec.create(registryKey, holderCodec, forceList).fieldOf("filter").forGetter(FilterHolderSet::filter))
                        .apply(builder, FilterHolderSet::new));
    }

    private final HolderSet<T> base;   // Conjunto base de Holders
    private final HolderSet<T> filter; // Conjunto filtro de Holders

    private Set<Holder<T>> set = null; // Cache del conjunto filtrado
    private List<Holder<T>> list = null; // Cache de la lista filtrada

    /**
     * Devuelve el conjunto base.
     */
    public HolderSet<T> base() {
        return this.base;
    }

    /**
     * Devuelve el conjunto filtro.
     */
    public HolderSet<T> filter() {
        return this.filter;
    }

    /**
     * Constructor que recibe el conjunto base y el filtro.
     *
     * @param base   Conjunto base de Holders.
     * @param filter Conjunto filtro de Holders.
     */
    public FilterHolderSet(HolderSet<T> base, HolderSet<T> filter) {
        this.base = base;
        this.filter = filter;
    }

    /**
     * Crea un conjunto inmutable de Holders filtrados.
     * Solo incluye los Holders del conjunto base que no estén en el filtro.
     *
     * @return Conjunto filtrado de Holders.
     */
    protected Set<Holder<T>> createSet() {
        return this.base
                .stream()
                .filter(holder -> !this.filter.contains(holder))
                .collect(Collectors.toSet());
    }

    /**
     * Devuelve el conjunto filtrado, usando cache para optimizar.
     */
    public Set<Holder<T>> getSet() {
        Set<Holder<T>> thisSet = this.set;
        if (thisSet == null) {
            Set<Holder<T>> set = this.createSet();
            this.set = set;
            return set;
        } else {
            return thisSet;
        }
    }

    /**
     * Devuelve la lista filtrada, usando cache para optimizar.
     */
    public List<Holder<T>> getList() {
        List<Holder<T>> thisList = this.list;
        if (thisList == null) {
            List<Holder<T>> list = List.copyOf(this.getSet());
            this.list = list;
            return list;
        } else {
            return thisList;
        }
    }

    /**
     * Devuelve un stream de los Holders filtrados.
     */
    @Override
    public Stream<Holder<T>> stream() {
        return this.getList().stream();
    }

    /**
     * Devuelve el tamaño del conjunto filtrado.
     */
    @Override
    public int size() {
        return this.getList().size();
    }

    /**
     * Devuelve el conjunto filtrado como una lista (no como TagKey).
     */
    @Override
    public Either<TagKey<T>, List<Holder<T>>> unwrap() {
        return Either.right(this.getList());
    }

    /**
     * Devuelve un elemento aleatorio del conjunto filtrado.
     */
    @Override
    public Optional<Holder<T>> getRandomElement(RandomSource rand) {
        List<Holder<T>> list = this.getList();
        int size = list.size();
        return size > 0
                ? Optional.of(list.get(rand.nextInt(size)))
                : Optional.empty();
    }

    /**
     * Devuelve el Holder en la posición indicada de la lista filtrada.
     */
    @Override
    public Holder<T> get(int i) {
        return this.getList().get(i);
    }

    /**
     * Comprueba si el Holder está en el conjunto filtrado.
     */
    @Override
    public boolean contains(Holder<T> holder) {
        return this.getSet().contains(holder);
    }

    /**
     * Comprueba si se puede serializar en el HolderOwner dado.
     */
    @Override
    public boolean canSerializeIn(HolderOwner<T> holderOwner) {
        return this.base.canSerializeIn(holderOwner) && this.filter.canSerializeIn(holderOwner);
    }

    /**
     * No utiliza TagKey, por lo que siempre devuelve vacío.
     */
    @Override
    public Optional<TagKey<T>> unwrapKey() {
        return Optional.empty();
    }

    /**
     * Devuelve un iterador sobre la lista filtrada.
     */
    @Override
    public Iterator<Holder<T>> iterator() {
        return this.getList().iterator();
    }
}