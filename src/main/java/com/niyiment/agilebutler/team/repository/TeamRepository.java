package com.niyiment.agilebutler.team.repository;

import com.niyiment.agilebutler.team.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {
    Optional<Team> findByInviteCode(String inviteCode);

    boolean existsByName(String name);

    @Query("SELECT t FROM Team t WHERE t.active = true")
    List<Team> findAllActive();
}
