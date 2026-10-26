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

package de.adorsys.keycloak.config.util;

import org.apache.commons.lang3.StringUtils;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;

import java.util.Objects;

public class KeycloakUtil {
    private static final String MASTER_REALM = "master";
    private static final String MASTER_REALM_PROXY_CLIENT_SUFFIX = "-realm";
    private static final String REALM_MANAGEMENT_CLIENT_ID = "realm-management";

    private KeycloakUtil() {
    }

    /**
     * Mirrors Keycloak's own ClientPermissions.isInternal(): every realm's "realm-management" client,
     * and in the master realm specifically, any client whose id ends in "-realm" (its per-realm
     * admin-console proxy clients, e.g. "foliospace-realm"). Keycloak 26.7+ rejects management of
     * these clients - and of "realm-management" client roles - through the Admin REST API for any
     * caller, regardless of privileges.
     */
    public static boolean isInternalClient(String realmName, String clientId) {
        if (clientId == null) {
            return false;
        }
        return REALM_MANAGEMENT_CLIENT_ID.equals(clientId)
                || (MASTER_REALM.equals(realmName) && clientId.endsWith(MASTER_REALM_PROXY_CLIENT_SUFFIX));
    }

    private static boolean isDefaultResource(String prefix, String property1, String property2) {
        if (property1 == null || property2 == null) {
            return false;
        }

        return Objects.equals(
                String.format("${%s_%s}", prefix, property1),
                property2
        ) || Objects.equals(
                // offline_access is named '${role_offline-access}' by default
                String.format("${%s_%s}", prefix, property1.replace("_", "-")),
                property2
        );
    }

    public static boolean isDefaultRole(RoleRepresentation role) {
        if (StringUtils.startsWith(role.getName(), "default-roles-") && StringUtils.equals(role.getDescription(), "${role_default-roles}")) {
            return true;
        }

        return isDefaultResource("role", role.getName(), role.getDescription());
    }

    public static boolean isDefaultClient(ClientRepresentation client) {
        return isDefaultResource("client", client.getClientId(), client.getName());
    }
}
