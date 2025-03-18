package de.adorsys.keycloak.config.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import de.adorsys.keycloak.config.provider.KeycloakProvider;
import de.adorsys.keycloak.config.util.DoubleIndexCache;

@Service("roleRepositoryCacheWrapper")
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class RoleRepositoryCacheWrapper extends RoleRepository {
    private static final Logger logger = LoggerFactory.getLogger(RoleRepositoryCacheWrapper.class);

    private final Map<String, DoubleIndexCache<String, String, RoleRepresentation>> realmCaches = new HashMap<>();

    @Autowired(required = false)
    public RoleRepositoryCacheWrapper(
        RealmRepository realmRepository,
        @Qualifier("clientRepositoryCacheWrapper") ClientRepository clientRepository,
        UserRepository userRepository, KeycloakProvider keycloakProvider
    ) {
        super(realmRepository, clientRepository, userRepository, keycloakProvider);
    }

    @Override
    public Optional<RoleRepresentation> searchRealmRole(String realm, String roleName) {
        var cache = getCache(realm);
        return Optional.ofNullable(cache.getBySecondary(roleName))
            .map(role -> {
                logger.debug(roleName + " found in cache");
                return role;
            })
            .or(() -> super.searchRealmRole(realm, roleName)
                .map(role -> {
                    cache.put(role.getId(), roleName, role);
                    return role;
                })
            );
    }

    @Override
    public void deleteRealmRole(String realm, RoleRepresentation role) {
        super.deleteRealmRole(realm, role);
        getCache(realm).removeByPrimary(role.getId());
    }

    @Override
    public void updateRealmRole(String realm, RoleRepresentation role) {
        super.updateRealmRole(realm, role);
        getCache(realm).removeByPrimary(role.getId());
    }

    private DoubleIndexCache<String, String, RoleRepresentation> getCache(String realm) {
        return realmCaches.computeIfAbsent(realm, k -> new DoubleIndexCache<>());
    }
}
