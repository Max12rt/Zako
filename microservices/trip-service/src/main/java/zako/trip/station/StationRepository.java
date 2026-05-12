package zako.trip.station;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StationRepository extends JpaRepository<Station, Long> {
    Optional<Station> findByCode(String code);
    List<Station> findByCityContainingIgnoreCase(String city);
    List<Station> findByNameContainingIgnoreCase(String name);
}
