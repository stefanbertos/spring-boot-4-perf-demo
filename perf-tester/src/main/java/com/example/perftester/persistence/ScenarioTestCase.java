package com.example.perftester.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "scenario_test_case")
@Getter
@Setter
public class ScenarioTestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "scenario_test_case_seq")
    @SequenceGenerator(name = "scenario_test_case_seq", sequenceName = "scenario_test_case_seq",
            allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private TestScenario scenario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    private TestCase testCase;

    private int percentage;

    @Column(name = "display_order")
    private int displayOrder;
}
