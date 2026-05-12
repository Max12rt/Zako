package zako.trip.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import zako.trip.route.Route;
import zako.trip.route.RouteRepository;
import zako.trip.route.RouteStop;
import zako.trip.station.Station;
import zako.trip.station.StationRepository;
import zako.trip.train.Train;
import zako.trip.train.TrainRepository;
import zako.trip.train.TrainType;
import zako.trip.trip.Trip;
import zako.trip.trip.TripRepository;
import zako.trip.trip.TripStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    private final StationRepository stationRepo;
    private final TrainRepository trainRepo;
    private final RouteRepository routeRepo;
    private final TripRepository tripRepo;

    private static final int DAYS_AHEAD = 14;
    private final Random rand = new Random(42);

    @Override
    @Transactional
    public void run(String... args) {
        if (trainRepo.count() > 0)    return;
        if (stationRepo.count() == 0) return;

        log.info("Seeding demo data: trains, routes, trips for {} days...", DAYS_AHEAD);

        List<Train> trains = seedTrains();
        Map<String, Station> byCode = stationsByCode();

        int totalTrips = 0;
        totalTrips += seedRouteWithTrips("Warszawa – Trójmiasto", byCode, trains,
                List.of(s("WAW", 0), s("WWS", 5), s("BDG", 130), s("GDN", 230), s("SOP", 240), s("GDY", 250)),
                List.of(t(6, 0), t(10, 0), t(14, 0), t(18, 0)));

        totalTrips += seedRouteWithTrips("Warszawa – Kraków – Zakopane", byCode, trains,
                List.of(s("WAW", 0), s("CZA", 90), s("KTW", 120), s("KRK", 180), s("ZAK", 290)),
                List.of(t(5, 30), t(9, 30), t(13, 30), t(17, 30)));

        totalTrips += seedRouteWithTrips("Warszawa – Poznań – Wrocław", byCode, trains,
                List.of(s("WAW", 0), s("LDZ", 80), s("POZ", 200), s("WRO", 320)),
                List.of(t(6, 30), t(10, 30), t(14, 30), t(18, 30)));

        totalTrips += seedRouteWithTrips("Szczecin – Bydgoszcz – Gdańsk", byCode, trains,
                List.of(s("SZC", 0), s("BDG", 180), s("TRN", 220), s("GDN", 350)),
                List.of(t(7, 0), t(13, 0)));

        totalTrips += seedRouteWithTrips("Warszawa – Lublin – Rzeszów", byCode, trains,
                List.of(s("WWS", 0), s("LUB", 130), s("RZE", 280)),
                List.of(t(7, 30), t(11, 30), t(15, 30), t(19, 30)));

        totalTrips += seedRouteWithTrips("Warszawa – Białystok", byCode, trains,
                List.of(s("WAW", 0), s("BIA", 150)),
                List.of(t(8, 0), t(11, 0), t(14, 0), t(17, 0), t(20, 0)));

        totalTrips += seedRouteWithTrips("SKM Trójmiasto", byCode, trains,
                List.of(s("GDN", 0), s("SOP", 10), s("GDY", 20)),
                List.of(t(6, 0), t(8, 0), t(10, 0), t(12, 0), t(14, 0), t(16, 0), t(18, 0), t(20, 0)));

        log.info("Seeded {} trains, {} routes, {} trips",
                trains.size(), routeRepo.count(), totalTrips);
    }

    private record StopDef(String code, int minutes) {}

    private static StopDef s(String code, int minutes) { return new StopDef(code, minutes); }
    private static LocalTime t(int hour, int minute)   { return LocalTime.of(hour, minute); }

    private List<Train> seedTrains() {
        List<Train> trains = new ArrayList<>();
        trains.add(saveTrain("IC 1100",   TrainType.IC,  240));
        trains.add(saveTrain("IC 2200",   TrainType.IC,  240));
        trains.add(saveTrain("IC 3300",   TrainType.IC,  240));
        trains.add(saveTrain("TLK 11000", TrainType.TLK, 280));
        trains.add(saveTrain("TLK 12000", TrainType.TLK, 280));
        trains.add(saveTrain("EIC 8100",  TrainType.EIC, 200));
        trains.add(saveTrain("EIC 8200",  TrainType.EIC, 200));
        trains.add(saveTrain("R 5500",    TrainType.R,   320));
        return trains;
    }

    private Train saveTrain(String number, TrainType type, int seats) {
        return trainRepo.save(Train.builder()
                .trainNumber(number).type(type).totalSeats(seats).build());
    }

    private Map<String, Station> stationsByCode() {
        Map<String, Station> map = new HashMap<>();
        for (Station st : stationRepo.findAll()) map.put(st.getCode(), st);
        return map;
    }

    private int seedRouteWithTrips(String name,
                                   Map<String, Station> byCode,
                                   List<Train> trains,
                                   List<StopDef> stopDefs,
                                   List<LocalTime> departureTimes) {
        Route route = Route.builder().name(name).stops(new ArrayList<>()).build();

        int order = 0;
        for (StopDef sd : stopDefs) {
            Station st = byCode.get(sd.code());
            if (st == null) {
                log.warn("Skipping stop {} for route {} — station not seeded", sd.code(), name);
                continue;
            }
            route.getStops().add(RouteStop.builder()
                    .route(route)
                    .station(st)
                    .stopOrder(order++)
                    .minutesFromStart(sd.minutes())
                    .build());
        }

        if (route.getStops().size() < 2) {
            log.warn("Route {} has fewer than 2 valid stops — skipped", name);
            return 0;
        }

        route = routeRepo.save(route);

        int duration = route.getStops().get(route.getStops().size() - 1).getMinutesFromStart();

        List<Trip> trips = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int day = 0; day < DAYS_AHEAD; day++) {
            LocalDate date = today.plusDays(day);
            for (LocalTime time : departureTimes) {
                Train train = trains.get(rand.nextInt(trains.size()));
                int sold = rand.nextInt(60);
                int available = Math.max(train.getTotalSeats() - sold, 1);
                trips.add(Trip.builder()
                        .train(train)
                        .route(route)
                        .departureTime(date.atTime(time))
                        .arrivalTime(date.atTime(time).plusMinutes(duration))
                        .status(TripStatus.SCHEDULED)
                        .availableSeats(available)
                        .build());
            }
        }
        tripRepo.saveAll(trips);
        return trips.size();
    }
}
