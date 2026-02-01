package com.example.perftester.persistence.repository;

import com.example.perftester.persistence.entity.PerfMessage;
import com.example.perftester.persistence.entity.PerfMessage.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PerfMessageRepository extends JpaRepository<PerfMessage, Long> {

    Optional<PerfMessage> findByMessageId(String messageId);

    List<PerfMessage> findByTestRunId(Long testRunId);

    List<PerfMessage> findByTestRunIdAndStatus(Long testRunId, MessageStatus status);

    @Query("SELECT COUNT(m) FROM PerfMessage m WHERE m.testRun.id = :testRunId AND m.status = :status")
    long countByTestRunIdAndStatus(@Param("testRunId") Long testRunId, @Param("status") MessageStatus status);

    @Modifying
    @Query("UPDATE PerfMessage m SET m.status = 'TIMEOUT' WHERE m.testRun.id = :testRunId AND m.status = 'SENT'")
    int markPendingMessagesAsTimeout(@Param("testRunId") Long testRunId);

    @Query("SELECT AVG(m.latencyMs) FROM PerfMessage m WHERE m.testRun.id = :testRunId AND m.status = 'RECEIVED'")
    Double calculateAverageLatency(@Param("testRunId") Long testRunId);
}
