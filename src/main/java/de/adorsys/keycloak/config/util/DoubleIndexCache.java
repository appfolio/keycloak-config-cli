package de.adorsys.keycloak.config.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.javers.common.collections.Pair;

public class DoubleIndexCache<TPKey, TSKey, TVal> {
    private final Map<TPKey, Pair<TSKey, TVal>> primaryCache = new HashMap<>();
    private final Map<TSKey, Pair<TPKey, TVal>> secondaryCache = new HashMap<>();

    public TVal getByPrimary(TPKey key) {
        return primaryCache.getOrDefault(
            key,
            new Pair<TSKey, TVal>(null, null)
        ).right();
    }

    public TVal getBySecondary(TSKey key) {
        return secondaryCache.getOrDefault(
            key,
            new Pair<TPKey, TVal>(null, null)
        ).right();
    }

    public void put(TPKey primaryKey, TSKey secondaryKey, TVal value) {
        primaryCache.put(
            primaryKey,
            new Pair<TSKey,TVal>(secondaryKey, value)
        );
        secondaryCache.put(
            secondaryKey,
            new Pair<TPKey,TVal>(primaryKey, value)
        );
    }

    public void removeByPrimary(TPKey key) {
        Optional.ofNullable(primaryCache.remove(key))
            .ifPresent(pair -> secondaryCache.remove(pair.left()));
    }

    public void removeBySecondary(TSKey key) {
        Optional.ofNullable(secondaryCache.remove(key))
            .ifPresent(pair -> primaryCache.remove(pair.left()));
    }
}
