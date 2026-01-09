docker build -t test-keycloak-config . && \
docker run \
-e KEYCLOAK_URL="https://account-admin.qa.appfolio.com" \
-e KEYCLOAK_USER="*****" \
-e KEYCLOAK_PASSWORD="******" \
-e KEYCLOAK_AVAILABILITYCHECK_ENABLED=true \
-e KEYCLOAK_AVAILABILITYCHECK_TIMEOUT=120s \
-e IMPORT_FILES_LOCATIONS="/config/config-realm-foliospace.json" \
-e INT_APPF_SUFFIX="appf.io" \
-e EXT_APPF_SUFFIX="qa.appf.io" \
-e LOGGING_LEVEL_KCC=debug \
-e IMPORT_MANAGED_GROUP=no-delete \
-e IMPORT_MANAGED_AUTHENTICATION_FLOW=no-delete \
-e IMPORT_MANAGED_CLIENT_AUTHORIZATION_RESOURCES=no-delete \
-e IMPORT_MANAGED_CLIENT_AUTHORIZATION_POLICIES=no-delete \
-e IMPORT_MANAGED_CLIENT_AUTHORIZATION_SCOPES=no-delete \
-e IMPORT_CACHE_ENABLED=false \
-e FOLIOSPACE_CLIENT_BASE_URL="https://resident.qa.foliospace.com" \
-e FOLIOSPACE_BACKEND_BASE_URL="https://residentapi.qa.foliospace.com" \
-v /Users/anil.dhurjaty/src/keycloak-custom/config_import:/config \
test-keycloak-config:latest
