package com.mercadosolidario;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = MercadoSolidarioApplication.class)
@ActiveProfiles("migration-test")
class MigrationIntegrationTest {

    private static final List<String> EXPECTED_VERSIONS =
            List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20");
    private static final List<String> EXPECTED_SCRIPTS = List.of(
            "V1__initial_schema.sql",
            "V2__insert_initial_roles.sql",
            "V3__priority_configuration_and_history.sql",
            "V4__auditable_stock_balances.sql",
            "V5__donation_workflows_and_pix_settings.sql",
            "V6__multi_item_distributions.sql",
            "V7__public_service_requests.sql",
            "V8__align_public_service_request_contract.sql",
            "V9__service_request_workflow.sql",
            "V10__previous_beneficiary_tracking.sql",
            "V11__service_request_updated_at.sql",
            "V12__user_password_setup_tokens.sql",
            "V13__historical_beneficiaries_and_product_notes.sql",
            "V14__user_first_access_password_change.sql",
            "V15__family_phone_address.sql",
            "V16__welcome_availability.sql",
            "V17__enum_columns_to_varchar.sql",
            "V18__staff_tasks.sql",
            "V19__basket_quantity.sql",
            "V20__default_priority_rules.sql"
    );

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void appliesEveryMigrationSequentiallyAndRemainsStableOnRepeatedMigrate() {
        assertAppliedVersions();
        assertSchemaAndSeedData();
        assertActivePriorityUniqueness();

        flyway.migrate();
        flyway.migrate();

        assertAppliedVersions();
        assertSchemaAndSeedData();
    }

    private void assertAppliedVersions() {
        MigrationInfo[] migrations = flyway.info().all();
        List<String> discoveredVersions = Arrays.stream(migrations)
                .filter(migration -> migration.getVersion() != null)
                .map(MigrationInfo::getVersion)
                .map(version -> version.getVersion())
                .toList();
        List<String> discoveredScripts = Arrays.stream(migrations)
                .filter(migration -> migration.getVersion() != null)
                .map(MigrationInfo::getScript)
                .toList();
        List<String> appliedVersions = Arrays.stream(migrations)
                .filter(migration -> migration.getVersion() != null)
                .filter(migration -> migration.getState() == MigrationState.SUCCESS)
                .map(MigrationInfo::getVersion)
                .map(version -> version.getVersion())
                .toList();

        assertThat(discoveredVersions).doesNotHaveDuplicates().containsExactlyElementsOf(EXPECTED_VERSIONS);
        assertThat(discoveredScripts).containsExactlyElementsOf(EXPECTED_SCRIPTS);
        assertThat(appliedVersions).containsExactlyElementsOf(EXPECTED_VERSIONS);
    }

    private void assertSchemaAndSeedData() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = 'public' AND table_name IN " +
                        "('roles', 'families', 'stock_movements', 'donations', " +
                        "'distributions', 'distribution_items', 'public_service_requests', " +
                        "'public_service_request_history', 'user_password_setup_tokens')",
                        Integer.class)).isEqualTo(9);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = 'public' AND table_name = 'public_service_requests' " +
                        "AND column_name IN ('full_name', 'location', 'contact_value', " +
                        "'contact_preference', 'household_size', 'assistance_type', 'consent_accepted', 'updated_at')",
                Integer.class)).isEqualTo(8);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = 'public' AND table_name = 'families' " +
                        "AND column_name IN ('previous_beneficiary', 'previous_beneficiary_years', " +
                        "'previous_beneficiary_notes')",
                Integer.class)).isEqualTo(3);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM roles", Integer.class)).isEqualTo(3);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = 'public' AND table_name = 'public_service_requests' " +
                        "AND column_name = 'updated_at' AND is_nullable = 'NO'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = 'public' AND table_name = 'public_service_request_history'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints " +
                        "WHERE constraint_name = 'uq_priority_configuration_single_active' " +
                        "AND constraint_type = 'UNIQUE'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints " +
                "WHERE table_schema = 'public' AND table_name = 'public_service_requests' " +
                "AND constraint_name = 'fk_service_request_assigned_user' " +
                "AND constraint_type = 'FOREIGN KEY'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.indexes " +
                "WHERE table_schema = 'public' AND index_name IN " +
                "('idx_service_request_history_request', 'idx_service_requests_status_created')",
                Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints " +
                        "WHERE table_schema = 'public' AND table_name = 'user_password_setup_tokens' " +
                        "AND constraint_name IN ('fk_user_password_setup_token_user', " +
                        "'uq_user_password_setup_token_user', 'uq_user_password_setup_token_hash')",
                Integer.class)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.indexes " +
                        "WHERE table_schema = 'public' AND index_name = 'idx_user_password_setup_tokens_expires_at'",
                Integer.class)).isEqualTo(1);
    }

    private void assertActivePriorityUniqueness() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update("UPDATE priority_configurations SET active = FALSE WHERE active = TRUE");
        jdbcTemplate.update("""
                INSERT INTO priority_configurations
                    (code, name, version_number, explanation, active, created_by, updated_by)
                VALUES (?, ?, 1, 'migration test', FALSE, 'test', 'test')
                """, "MIGRATION-INACTIVE-A-" + suffix, "Inactive A");
        jdbcTemplate.update("""
                INSERT INTO priority_configurations
                    (code, name, version_number, explanation, active, created_by, updated_by)
                VALUES (?, ?, 1, 'migration test', FALSE, 'test', 'test')
                """, "MIGRATION-INACTIVE-B-" + suffix, "Inactive B");
        jdbcTemplate.update("""
                INSERT INTO priority_configurations
                    (code, name, version_number, explanation, active, created_by, updated_by)
                VALUES (?, ?, 1, 'migration test', TRUE, 'test', 'test')
                """, "MIGRATION-ACTIVE-A-" + suffix, "Active A");

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO priority_configurations
                    (code, name, version_number, explanation, active, created_by, updated_by)
                VALUES (?, ?, 1, 'migration test', TRUE, 'test', 'test')
                """, "MIGRATION-ACTIVE-B-" + suffix, "Active B"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
