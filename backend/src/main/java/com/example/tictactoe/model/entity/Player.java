package com.example.tictactoe.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "player",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_player_normalized_name",
            columnNames = "normalized_name"
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 50)
    private String normalizedName;

    public Player(String name, String normalizedName) {
        this.name = name;
        this.normalizedName = normalizedName;
    }
}
