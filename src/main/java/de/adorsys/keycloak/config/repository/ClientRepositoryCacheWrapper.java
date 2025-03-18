package de.adorsys.keycloak.config.repository;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.representations.idm.ClientRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import de.adorsys.keycloak.config.util.DoubleIndexCache;

@Service("clientRepositoryCacheWrapper")
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class ClientRepositoryCacheWrapper extends ClientRepository {
    private static final Logger logger = LoggerFactory.getLogger(ClientRepositoryCacheWrapper.class);

    private final Map<String, DoubleIndexCache<String, String, ClientRepresentation>> realmCaches = new HashMap<>();

    @Autowired(required = false)
    public ClientRepositoryCacheWrapper(RealmRepository realmRepository) {
        super(realmRepository);
    }

    @Override
    public ClientRepresentation getByClientId(String realm, String clientId) {
        var client = getCache(realm).getBySecondary(clientId);
        if (client != null) {
            logger.debug(clientId + " found in cache");
            return client;
        }

        client = super.getByClientId(realm, clientId);
        getCache(realm).put(client.getId(), clientId, client);
        return client;
    }

    @Override
    public void remove(String realm, ClientRepresentation client) {
        super.remove(realm, client);
        getCache(realm).removeByPrimary(client.getId());
    }

    @Override
    public void update(String realm, ClientRepresentation client) {
        super.update(realm, client);
        getCache(realm).removeByPrimary(client.getId());
    }

    private DoubleIndexCache<String, String, ClientRepresentation> getCache(String realm) {
        return realmCaches.computeIfAbsent(realm, k -> new DoubleIndexCache<>());
    }
}
