package com.niyiment.agilebutler.standup.repository;

import com.niyiment.agilebutler.standup.model.Standup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface StandupRepository extends JpaRepository<Standup, UUID> {
    Optional<Standup> findByUserIdAndStandupDate(UUID userId, LocalDate date);

    List<Standup> findAllByTeamIdAndStandupDate(UUID teamId, LocalDate date);

    @Query("""
            SELECT s FROM Standup s 
                JOIN FETCH s.user
                    WHERE s.team.id = :teamId AND s.standupDate = :date 
                    AND s.status = 'SUBMITTED' ORDER BY s.createdAt ASC
            """)
    List<Standup> findSubmittedByTeamAndDate(UUID teamId, LocalDate date);

    @Query("""
            SELECT s FROM Standup s 
                JOIN FETCH s.user
                    WHERE s.team.id = :teamId AND s.standupDate = :date 
                    AND s.blockerFlagged = true 
            """)
    List<Standup> findBlockersByTeamAndDate(UUID teamId, LocalDate date);

    @Query("""
            SELECT COUNT(s) FROM Standup s 
                    WHERE s.team.id = :teamId AND s.standupDate = :date 
                    AND s.status = 'SUBMITTED'
            """)
    long countSubmittedByTeamAndDate(UUID teamId, LocalDate date);

    Page<Standup> findByUserIdOrderByStandupDateDesc(UUID userId, Pageable pageable);

    boolean existsByUserIdAndStandupDate(UUID userId, LocalDate date);
}
