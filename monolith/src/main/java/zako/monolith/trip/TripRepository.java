package zako.monolith.trip;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {

    @Query("""
            SELECT t FROM Trip t
            JOIN t.route.stops fromStop
            JOIN t.route.stops toStop
            WHERE fromStop.station.id = :fromStationId
              AND toStop.station.id = :toStationId
              AND fromStop.stopOrder < toStop.stopOrder
              AND t.departureTime >= :from
              AND t.departureTime < :to
              AND t.status = 'SCHEDULED'
              AND t.availableSeats > 0
            """)
    List<Trip> searchTrips(@Param("fromStationId") Long fromStationId,
                           @Param("toStationId") Long toStationId,
                           @Param("from") LocalDateTime from,
                           @Param("to") LocalDateTime to);
}
