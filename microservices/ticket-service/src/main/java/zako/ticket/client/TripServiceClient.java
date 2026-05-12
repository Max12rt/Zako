package zako.ticket.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import zako.ticket.exception.ResourceNotFoundException;
import zako.ticket.ticket.dto.StationDto;
import zako.ticket.ticket.dto.TripDto;

@Component
@RequiredArgsConstructor
public class TripServiceClient {

    private final RestClient tripRestClient;

    public TripDto getTrip(Long tripId) {
        try {
            return tripRestClient.get().uri("/api/trips/{id}", tripId).retrieve().body(TripDto.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Trip not found: " + tripId);
        }
    }

    public StationDto getStation(Long stationId) {
        try {
            return tripRestClient.get().uri("/api/stations/{id}", stationId).retrieve().body(StationDto.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Station not found: " + stationId);
        }
    }

    @Configuration
    static class RestClientConfig {
        @Bean
        RestClient tripRestClient(@Value("${trip-service.base-url:http://localhost:8082}") String baseUrl) {
            return RestClient.builder().baseUrl(baseUrl).build();
        }
    }
}
