package zako.monolith.station;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StationSeeder implements CommandLineRunner {

    private final StationRepository stationRepository;

    @Override
    public void run(String... args) {
        if (stationRepository.count() > 0) return;

        List<Station> stations = List.of(
                station("Warszawa Centralna",   "Warszawa",    "WAW", 52.2287, 21.0034),
                station("Warszawa Wschodnia",   "Warszawa",    "WWS", 52.2517, 21.0488),
                station("Kraków Główny",        "Kraków",      "KRK", 50.0682, 19.9447),
                station("Gdańsk Główny",        "Gdańsk",      "GDN", 54.3556, 18.6438),
                station("Gdynia Główna",        "Gdynia",      "GDY", 54.5226, 18.5306),
                station("Poznań Główny",        "Poznań",      "POZ", 52.4022, 16.9119),
                station("Wrocław Główny",       "Wrocław",     "WRO", 51.0989, 17.0367),
                station("Łódź Fabryczna",       "Łódź",        "LDZ", 51.7689, 19.4633),
                station("Łódź Kaliska",         "Łódź",        "LDK", 51.7592, 19.4214),
                station("Katowice",             "Katowice",    "KTW", 50.2581, 19.0167),
                station("Lublin",               "Lublin",      "LUB", 51.2342, 22.5688),
                station("Szczecin Główny",      "Szczecin",    "SZC", 53.4169, 14.5550),
                station("Białystok",            "Białystok",   "BIA", 53.1359, 23.1664),
                station("Bydgoszcz Główna",     "Bydgoszcz",   "BDG", 53.1346, 17.9919),
                station("Olsztyn Główny",       "Olsztyn",     "OLS", 53.7806, 20.4872),
                station("Rzeszów Główny",       "Rzeszów",     "RZE", 50.0419, 22.0023),
                station("Toruń Główny",         "Toruń",       "TRN", 53.0136, 18.6079),
                station("Częstochowa",          "Częstochowa", "CZA", 50.7989, 19.0814),
                station("Zakopane",             "Zakopane",    "ZAK", 49.2992, 19.9496),
                station("Sopot",                "Sopot",       "SOP", 54.4416, 18.5604)
        );

        stationRepository.saveAll(stations);
    }

    private Station station(String name, String city, String code, double lat, double lon) {
        return Station.builder()
                .name(name)
                .city(city)
                .code(code)
                .latitude(lat)
                .longitude(lon)
                .build();
    }
}
