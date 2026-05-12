package zako.ticket.ticket;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "tickets")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Ticket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long userId;
    @Column(nullable = false) private Long tripId;
    @Column(nullable = false) private Long fromStationId;
    private String fromStationName;
    private String fromStationCity;
    private String fromStationCode;
    @Column(nullable = false) private Long toStationId;
    private String toStationName;
    private String toStationCity;
    private String toStationCode;
    private Integer seatNumber;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal price;
    @Enumerated(EnumType.STRING) @Column(nullable = false) @Builder.Default
    private TicketStatus status = TicketStatus.ACTIVE;
    @Enumerated(EnumType.STRING) @Column(nullable = false) @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    @Column(unique = true, nullable = false) private String ticketCode;
    @Builder.Default private LocalDateTime purchasedAt = LocalDateTime.now();
}
