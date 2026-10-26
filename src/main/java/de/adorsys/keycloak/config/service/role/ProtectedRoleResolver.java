/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2021 adorsys GmbH & Co. KG @ https://adorsys.com
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

package de.adorsys.keycloak.config.service.role;

import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportProtectedRolesProperties;
import de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportProtectedRolesProperties.ProtectedRolesMode;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves whether a realm-level or client-level role is protected from update/delete/composite-sync
 * during import. This is a pure lookup - it consults no remote state and performs no I/O, so it is
 * safe to consult per-role, including from within parallel import loops.
 * <p>
 * A protected client role entry of {@code "*"} protects every role of that client, including roles
 * unknown at configuration time.
 * <p>
 * Besides the configured/default protected roles above, this also handles a Keycloak server rule where
 * Keycloak treats every client whose id ends in "-realm" in the master realm the same as "realm-management".
 * Role updates/deletes on them are rejected with HTTP 403 for any caller.
 * See {@code ClientPermissions.isInternal} and {@code AdminRoles.APP_SUFFIX} in the Keycloak server
 * source. Unlike the other built-in defaults, this one can't be expressed as a fixed client id in
 * configuration since the matching clients are named after whatever realms exist, so it's applied
 * directly here instead, gated the same way as the other built-ins (active in ADD mode, suppressed by
 * REPLACE).
 */
@Component
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class ProtectedRoleResolver {

    private static final Collection<String> BUILT_IN_PROTECTED_REALM_ROLES = List.of("admin", "create-realm");
    private static final String BUILT_IN_PROTECTED_CLIENT_ID = "realm-management";
    private static final String WILDCARD = "*";
    private static final String MASTER_REALM = "master";
    private static final String MASTER_REALM_PROXY_CLIENT_SUFFIX = "-realm";

    private final boolean isAddMode;
    private final Set<String> protectedRealmRoles;
    private final Map<String, Set<String>> protectedClientRoles;

    public ProtectedRoleResolver(ImportConfigProperties importConfigProperties) {
        ImportProtectedRolesProperties protectedRolesProperties = importConfigProperties.getProtectedRoles();
        Set<String> realmRoles = new HashSet<>();
        Map<String, Set<String>> clientRoles = new HashMap<>();

        this.isAddMode = protectedRolesProperties.getMode() == ProtectedRolesMode.ADD;
        if (isAddMode) {
            realmRoles.addAll(BUILT_IN_PROTECTED_REALM_ROLES);
            clientRoles.put(BUILT_IN_PROTECTED_CLIENT_ID, new HashSet<>(Set.of(WILDCARD)));
        }

        realmRoles.addAll(protectedRolesProperties.getRealmRoles());

        for (Map.Entry<String, Collection<String>> entry : protectedRolesProperties.getClientRoles().entrySet()) {
            clientRoles.computeIfAbsent(entry.getKey(), key -> new HashSet<>()).addAll(entry.getValue());
        }

        this.protectedRealmRoles = Set.copyOf(realmRoles);

        Map<String, Set<String>> immutableClientRoles = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : clientRoles.entrySet()) {
            immutableClientRoles.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        this.protectedClientRoles = Map.copyOf(immutableClientRoles);
    }

    public boolean isRealmRoleProtected(String roleName) {
        if (roleName == null) {
            return false;
        }
        return protectedRealmRoles.contains(roleName);
    }

    public boolean isClientRoleProtected(String realmName, String clientId, String roleName) {
        if (clientId == null || roleName == null) {
            return false;
        }
        if (isAddMode && isMasterRealmProxyClient(realmName, clientId)) {
            return true;
        }
        Set<String> roles = protectedClientRoles.get(clientId);
        if (roles == null) {
            return false;
        }
        return roles.contains(WILDCARD) || roles.contains(roleName);
    }

    private boolean isMasterRealmProxyClient(String realmName, String clientId) {
        return MASTER_REALM.equals(realmName) && clientId.endsWith(MASTER_REALM_PROXY_CLIENT_SUFFIX);
    }

    public List<RoleRepresentation> filterUnprotectedRealmRoles(List<RoleRepresentation> realmRoles) {
        return realmRoles.stream()
                .filter(role -> !isRealmRoleProtected(role.getName()))
                .toList();
    }

    public Map<String, List<RoleRepresentation>> filterUnprotectedClientRoles(String realmName, Map<String, List<RoleRepresentation>> clientRoles) {
        Map<String, List<RoleRepresentation>> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, List<RoleRepresentation>> client : clientRoles.entrySet()) {
            String clientId = client.getKey();
            List<RoleRepresentation> unprotectedRoles = client.getValue().stream()
                    .filter(role -> !isClientRoleProtected(realmName, clientId, role.getName()))
                    .toList();
            filtered.put(clientId, unprotectedRoles);
        }
        return filtered;
    }
}
