package com.niyiment.agilebutler.decision.repository;

import com.niyiment.agilebutler.decision.model.Vote;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VoteRepository extends JpaRepository<Vote, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Vote v WHERE v.session.id = :sessionId AND v.user.id = :userId")
    List<Vote> findAllBySessionIdAndUserIdForUpdate(UUID sessionId, UUID userId);

    @Query("SELECT v FROM Vote v WHERE v.session.id = :sessionId")
    List<Vote> findAllBySessionId(UUID sessionId);

    @Query("SELECT v FROM Vote v WHERE v.session.id = :sessionId AND v.user.id = :userId")
    List<Vote> findAllBySessionIdAndUserId(UUID sessionId, UUID userId);

    @Query("""
            SELECT v FROM Vote v JOIN FETCH v.option
            WHERE v.session.id = :sessionId
                ORDER BY v.rankOrder ASC NULLS LAST
            """)
    List<Vote> findAllBySessionIdWithOptionForRanked(UUID sessionId);

    @Query("""
            SELECT v.option.id, COUNT(v)
            FROM Vote v
            WHERE v.session.id = :sessionId
            GROUP BY v.option.id
            """)
    List<Object[]> countVotesByOption(UUID sessionId);

    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM Vote v WHERE v.session.id = :sessionId AND v.user.id = :userId")
    boolean existsBySessionIdAndUserId(UUID sessionId, UUID userId);

    @Query("SELECT COUNT(v) FROM Vote v WHERE v.session.id = :sessionId")
    long countBySessionId(UUID sessionId);

    @Modifying
    @Query("""
            DElETE FROM Vote v WHERE v.session.id = :sessionId AND v.user.id = :userId
            """)
    void deleteAllBySessionIdAndUserId(UUID sessionId, UUID userId);
}
