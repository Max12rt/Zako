package zako.trip;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TripIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void stationsEndpointIsPublic() throws Exception {
        mvc.perform(get("/api/stations")).andExpect(status().isOk());
    }

    @Test
    void tripsEndpointIsPublic() throws Exception {
        mvc.perform(get("/api/trips")).andExpect(status().isOk());
    }
}
