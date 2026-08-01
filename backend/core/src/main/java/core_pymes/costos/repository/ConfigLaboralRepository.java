package core_pymes.costos.repository;

import core_pymes.costos.domain.ConfigLaboral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ConfigLaboralRepository extends JpaRepository<ConfigLaboral, UUID> {
}
