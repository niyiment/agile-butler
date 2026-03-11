package com.niyiment.agilebutler.decision.repository;

import com.niyiment.agilebutler.decision.model.DecisionOption;
import com.niyiment.agilebutler.decision.model.DecisionSession;
import com.niyiment.agilebutler.decision.model.enums.SessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DecisionSessionRepository extends JpaRepository<DecisionSession, UUID> {

    @Query("""
            SELECT s FROM DecisionSession s
            JOIN FETCH s.createdByUser
            JOIN FETCH s.options
            WHERE s.id = :id
            """)
    Optional<DecisionSession> findByIdWithDetails(UUID id);

    @Query("SELECT s FROM DecisionSession s WHERE s.team.id = :teamId AND s.status = :status ORDER BY s.createdAt DESC")
    List<DecisionSession> findAllByTeamIdAndStatusOrderByCreatedAtDesc(
            UUID teamId, SessionStatus status);

    @Query("SELECT s FROM DecisionSession s WHERE s.team.id = :teamId ORDER BY s.createdAt DESC")
    Page<DecisionSession> findByTeamIdOrderByCreatedAtDesc(UUID teamId, Pageable pageable);

    @Query("""
            SELECT s FROM DecisionSession s
            LEFT JOIN FETCH s.options
            WHERE s.team.id = :teamId
            ORDER BY s.createdAt DESC
            """)
    List<DecisionSession> findByTeamIdWithDetails(UUID teamId);

    @Query("""
            SELECT s.id, o.id, COUNT(v)
            FROM DecisionSession s
            JOIN s.options o
            LEFT JOIN o.votes v
            WHERE s.team.id = :teamId
            GROUP BY s.id, o.id
            """)
    List<Object[]> findVoteCountsByTeamId(UUID teamId);

    @Query("""
            SELECT s FROM DecisionSession s
            WHERE s.sessionType = 'TIMED_POLL'
              AND s.status = 'ACTIVE'
              AND s.closesAt <= :now
            """)
    List<DecisionSession> findExpiredTimedPolls(Instant now);

    @Query("""
            SELECT s FROM DecisionSession s
            WHERE s.status = 'ACTIVE'
              AND s.closesAt <= :now
            """)
    List<DecisionSession> findAllExpiredActiveSessions(Instant now);

    @Query("""
            SELECT COUNT(DISTINCT v.user.id) FROM Vote v
            WHERE v.session.id = :sessionId
            """)
    int countParticipants(UUID sessionId);

    @Query("SELECT d FROM DecisionOption d WHERE d.session.id = :id ORDER BY d.displayOrder ASC")
    List<DecisionOption> findAllBySessionIdOrderByDisplayOrderAsc(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM DecisionSession s WHERE s.id = :id")
    Optional<DecisionSession> findByIdForUpdate(UUID id);
}
