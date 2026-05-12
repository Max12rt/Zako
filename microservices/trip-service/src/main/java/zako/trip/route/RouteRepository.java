package zako.trip.route;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {

    @Query("""
            SELECT DISTINCT r FROM Route r
            JOIN r.stops fromStop
            JOIN r.stops toStop
            WHERE fromStop.station.id = :fromStationId
              AND toStop.station.id = :toStationId
              AND fromStop.stopOrder < toStop.stopOrder
            """)
    List<Route> findRoutesBetweenStations(@Param("fromStationId") Long fromStationId,
                                          @Param("toStationId") Long toStationId);
}
