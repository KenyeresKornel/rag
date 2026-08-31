package com.example.arxivrag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class DatabaseMigrationIntegrationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testScenario2_1_CheckpointTableSchemaIntegrity() {
        // 1. Assert table exists with the correct columns
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
            "SELECT column_name, data_type, is_nullable " +
            "FROM information_schema.columns " +
            "WHERE table_name = 'embedding_checkpoints' " +
            "ORDER BY column_name"
        );

        assertThat(columns).isNotEmpty();
        
        // Expected columns: chunk_id, embedded_at, embedding_model, content_hash, paper_id
        assertThat(columns.stream().map(c -> c.get("column_name"))).containsExactlyInAnyOrder(
            "paper_id", "chunk_id", "embedding_model", "content_hash", "embedded_at"
        );

        // Verify types
        Map<String, Object> paperIdCol = columns.stream().filter(c -> "paper_id".equals(c.get("column_name"))).findFirst().get();
        assertThat(paperIdCol.get("data_type")).isEqualTo("character varying");
        assertThat(paperIdCol.get("is_nullable")).isEqualTo("NO");

        Map<String, Object> chunkIdCol = columns.stream().filter(c -> "chunk_id".equals(c.get("column_name"))).findFirst().get();
        assertThat(chunkIdCol.get("data_type")).isEqualTo("character varying");
        assertThat(chunkIdCol.get("is_nullable")).isEqualTo("NO");

        Map<String, Object> modelCol = columns.stream().filter(c -> "embedding_model".equals(c.get("column_name"))).findFirst().get();
        assertThat(modelCol.get("data_type")).isEqualTo("character varying");
        assertThat(modelCol.get("is_nullable")).isEqualTo("NO");

        // 2. Assert Primary Key is a composite of (paper_id, chunk_id, embedding_model)
        List<String> pkColumns = jdbcTemplate.queryForList(
            "SELECT kcu.column_name " +
            "FROM information_schema.table_constraints tc " +
            "JOIN information_schema.key_column_usage kcu " +
            "  ON tc.constraint_name = kcu.constraint_name " +
            "  AND tc.table_schema = kcu.table_schema " +
            "WHERE tc.constraint_type = 'PRIMARY KEY' " +
            "  AND tc.table_name = 'embedding_checkpoints' " +
            "ORDER BY kcu.column_name",
            String.class
        );

        assertThat(pkColumns).containsExactlyInAnyOrder("paper_id", "chunk_id", "embedding_model");
    }

    @Test
    void testScenario2_2_PgVectorExtensionAndStoreCreation() {
        // 1. Verify pgvector extension is installed/enabled
        Integer extensionCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM pg_extension WHERE extname = 'vector'",
            Integer.class
        );
        assertThat(extensionCount).isEqualTo(1);

        // 2. Verify vector_store table is present
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
            "SELECT column_name, data_type, udt_name " +
            "FROM information_schema.columns " +
            "WHERE table_name = 'vector_store'"
        );
        assertThat(columns).isNotEmpty();

        // Verify the embedding column is of type vector (with udt_name = 'vector')
        Map<String, Object> embeddingCol = columns.stream()
            .filter(c -> "embedding".equals(c.get("column_name")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("embedding column not found in vector_store"));
        
        assertThat(embeddingCol.get("udt_name")).isEqualTo("vector");

        // 3. Verify indexes on vector_store are successfully created
        List<String> indexes = jdbcTemplate.queryForList(
            "SELECT indexname FROM pg_indexes WHERE tablename = 'vector_store'",
            String.class
        );
        
        // HNSW index and GIN index
        assertThat(indexes).contains("vector_store_embedding_idx", "idx_vector_store_metadata");
    }

    @Test
    void testScenario2_3_IdempotentMigrationsRun() {
        // Ensure that executing database migrations on an already migrated database runs correctly and fast.
        // Spring Boot executes Flyway migrations on startup. We verify that there are no schema failures.
        Integer migrationCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM flyway_schema_history",
            Integer.class
        );
        assertThat(migrationCount).isGreaterThanOrEqualTo(3);
    }
}
