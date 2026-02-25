package com.example.perftester.loki;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "loki_service_label")
@Getter
@Setter
@NoArgsConstructor
public class LokiServiceLabel {

    @Id
    private String name;

    public LokiServiceLabel(String name) {
        this.name = name;
    }
}
