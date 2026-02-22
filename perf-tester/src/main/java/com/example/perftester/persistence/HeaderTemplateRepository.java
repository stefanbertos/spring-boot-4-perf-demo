package com.example.perftester.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HeaderTemplateRepository extends JpaRepository<HeaderTemplate, Long> {

    List<HeaderTemplate> findAllByOrderByNameAsc();

    boolean existsByNameAndIdNot(String name, Long id);
}
