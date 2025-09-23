package grpify.grpify.board.domain;

import grpify.grpify.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Board extends BaseTimeEntity {
    @Id @GeneratedValue
    @Column(name = "board_id")
    private Long id;

    @Column(name = "board_name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 50)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private boolean isDeleted = false;


    public void update(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void softDelete() {
        this.isDeleted = true;
    }
}
