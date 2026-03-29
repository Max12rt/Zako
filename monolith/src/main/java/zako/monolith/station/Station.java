package zako.monolith.station;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false)
    private String city;

    @Column(unique = true, nullable = false)
    private String code;

    private Double latitude;
    private Double longitude;
}
