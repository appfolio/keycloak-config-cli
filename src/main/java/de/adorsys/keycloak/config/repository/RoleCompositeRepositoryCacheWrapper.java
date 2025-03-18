package de.adorsys.keycloak.config.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service("roleCompositeRepositoryCacheWrapper")
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class RoleCompositeRepositoryCacheWrapper extends RoleCompositeRepository {

    @Autowired(required = false)
    public RoleCompositeRepositoryCacheWrapper(
            @Qualifier("roleRepositoryCacheWrapper") RoleRepository roleRepository,
            @Qualifier("clientRepositoryCacheWrapper") ClientRepository clientRepository) {
        super(roleRepository, clientRepository);
    }
}
