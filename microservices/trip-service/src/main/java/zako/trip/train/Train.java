package zako.trip.train;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trains")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Train {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String trainNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrainType type;

    @Column(nullable = false)
    private Integer totalSeats;
}
