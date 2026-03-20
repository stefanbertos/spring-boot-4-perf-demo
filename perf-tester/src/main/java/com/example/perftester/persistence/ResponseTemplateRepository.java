package com.example.perftester.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResponseTemplateRepository extends JpaRepository<ResponseTemplate, Long> {

    List<ResponseTemplate> findAllByOrderByNameAsc();

    boolean existsByNameAndIdNot(String name, Long id);

    Optional<ResponseTemplate> findByName(String name);
}
