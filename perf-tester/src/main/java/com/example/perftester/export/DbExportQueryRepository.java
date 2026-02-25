package com.example.perftester.export;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DbExportQueryRepository extends JpaRepository<DbExportQuery, Long> {

    List<DbExportQuery> findAllByOrderByDisplayOrderAscNameAsc();
}
