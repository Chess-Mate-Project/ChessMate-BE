package backend.chessmate.api.user.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

import org.springframework.data.annotation.CreatedDate;


@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lichess_id", nullable = false, unique = true)
    private String lichessId;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "description")
    private String description;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
