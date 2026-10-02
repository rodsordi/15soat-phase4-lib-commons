package br.com.fiap.phase4.commons.postgres;

import br.com.fiap.phase4.commons.postgres.entity.BaseJpaEntity;
import br.com.fiap.phase4.commons.postgres.repository.PageRequestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PostgreSQL Commons Tests")
class PostgresCommonsTest {

    static class TestEntity extends BaseJpaEntity {
        TestEntity() {
            super();
        }
        TestEntity(UUID id) {
            super(id);
        }
    }

    @Nested
    @DisplayName("BaseJpaEntity Tests")
    class EntityTests {

        @Test
        @DisplayName("Should maintain UUID equality")
        void shouldMaintainEquality() {
            UUID id = UUID.randomUUID();
            var e1 = new TestEntity(id);
            var e2 = new TestEntity(id);

            assertThat(e1).isEqualTo(e2);
            assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
        }
    }

    @Nested
    @DisplayName("PageRequestUtils Tests")
    class PaginationTests {

        @Test
        @DisplayName("Should create page request with defaults when nulls provided")
        void shouldCreateDefaultPage() {
            Pageable pageable = PageRequestUtils.of(null, null, null, null);

            assertThat(pageable.getPageNumber()).isEqualTo(0);
            assertThat(pageable.getPageSize()).isEqualTo(20);
            assertThat(pageable.getSort().getOrderFor("createdAt")).isNotNull();
            assertThat(pageable.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.ASC);
        }

        @Test
        @DisplayName("Should cap page size to MAX_SIZE (100)")
        void shouldCapPageSize() {
            Pageable pageable = PageRequestUtils.of(1, 500, "updatedAt", "desc");

            assertThat(pageable.getPageNumber()).isEqualTo(1);
            assertThat(pageable.getPageSize()).isEqualTo(100);
            assertThat(pageable.getSort().getOrderFor("updatedAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        }
    }
}
