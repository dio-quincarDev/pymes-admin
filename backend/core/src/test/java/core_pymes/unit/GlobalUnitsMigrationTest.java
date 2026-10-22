package core_pymes.unit;

import core_pymes.common.seed.SeedDataRunner;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

// ponytail: seed y V7 deben usar los mismos 8 IDs globales en cualquier ambiente (prod/stg/nuevo VPS)
class GlobalUnitsMigrationTest {

    @Test
    void v7_ContainsSameGlobalIds_AsSeed() throws Exception {
        String v7;
        try (var in = Objects.requireNonNull(
                getClass().getResourceAsStream("/db/migration/V7__normalize_units.sql"))) {
            v7 = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertThat(SeedDataRunner.GLOBAL_UNITS).hasSize(8);
        for (var g : SeedDataRunner.GLOBAL_UNITS) {
            assertThat(v7).contains(g[0]);
        }
    }
}
