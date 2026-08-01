package core_pymes.jpa;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractJpaTest {

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        var jdbcUrl = postgres.getJdbcUrl();
        var urlWithSchema = jdbcUrl.contains("?") ? jdbcUrl + "&currentSchema=core" : jdbcUrl + "?currentSchema=core";
        registry.add("spring.datasource.url", () -> urlWithSchema);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "core");
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration");
    }

    @Autowired
    protected TestEntityManager em;

    @Autowired
    protected JdbcTemplate jdbc;

    @BeforeEach
    void cleanDb() {
        jdbc.execute("DELETE FROM core.loan_payments");
        jdbc.execute("DELETE FROM core.loans");
        jdbc.execute("DELETE FROM core.operating_expenses");
        jdbc.execute("DELETE FROM core.daily_sales");
        jdbc.execute("DELETE FROM core.tenant_financial_metrics");
        jdbc.execute("DELETE FROM core.patrimony");
        jdbc.execute("DELETE FROM core.invoice_items");
        jdbc.execute("DELETE FROM core.invoices");
        jdbc.execute("DELETE FROM core.product_presentations");
        jdbc.execute("DELETE FROM core.products");
        jdbc.execute("DELETE FROM core.providers");
        jdbc.execute("DELETE FROM core.expense_analysis");
        jdbc.execute("DELETE FROM core.collaboradores");
        jdbc.execute("DELETE FROM core.gastos_fijos_recurrentes");
        jdbc.execute("DELETE FROM core.config_laboral");
        jdbc.execute("DELETE FROM core.tenant_setup");
    }
}
