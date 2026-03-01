package com.example.perftester.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TestRunRepository extends JpaRepository<TestRun, Long> {

    List<TestRun> findAllByOrderByStartedAtDesc();

    Page<TestRun> findAllByOrderByStartedAtDesc(Pageable pageable);

    @Query("SELECT t FROM TestRun t WHERE t.tags LIKE :pattern ORDER BY t.startedAt DESC")
    Page<TestRun> findByTagsLike(@Param("pattern") String pattern, Pageable pageable);

    @Query("SELECT t.tags FROM TestRun t WHERE t.tags IS NOT NULL")
    List<String> findAllTagsJson();

    @Query("SELECT t FROM TestRun t WHERE t.status = 'COMPLETED' ORDER BY t.startedAt ASC")
    List<TestRun> findCompletedOrderByStartedAtAsc();
}
