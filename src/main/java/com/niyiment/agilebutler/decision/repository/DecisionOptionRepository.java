package com.niyiment.agilebutler.decision.repository;

import com.niyiment.agilebutler.decision.model.DecisionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DecisionOptionRepository extends JpaRepository<DecisionOption, UUID> {

    @org.springframework.data.jpa.repository.Query("SELECT d FROM DecisionOption d WHERE d.session.id = :sessionId ORDER BY d.displayOrder ASC")
    List<DecisionOption> findAllBySessionIdOrderByDisplayOrderAsc(java.util.UUID sessionId);
}