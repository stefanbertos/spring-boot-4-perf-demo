package com.example.perftester.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InfraProfileRepository extends JpaRepository<InfraProfile, Long> {

    List<InfraProfile> findAllByOrderByNameAsc();

    boolean existsByNameAndIdNot(String name, Long id);
}
