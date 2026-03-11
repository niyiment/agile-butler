package com.niyiment.agilebutler.decision.repository;

import com.niyiment.agilebutler.decision.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    @Query("""
            SELECT c FROM Comment c
            JOIN FETCH c.user
            WHERE c.session.id = :sessionId
            ORDER BY c.createdAt ASC
            """)
    List<Comment> findAllBySessionIdWithUser(UUID sessionId);
}