package com.example.perftester.export;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "db_export_query")
@Getter
@Setter
public class DbExportQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "db_export_query_seq")
    @SequenceGenerator(name = "db_export_query_seq", sequenceName = "db_export_query_seq", allocationSize = 1)
    private Long id;

    private String name;

    @Column(name = "sql_query", columnDefinition = "TEXT")
    private String sqlQuery;

    private int displayOrder;
}
