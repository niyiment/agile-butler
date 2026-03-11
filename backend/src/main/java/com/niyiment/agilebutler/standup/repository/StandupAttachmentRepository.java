package com.niyiment.agilebutler.standup.repository;

import com.niyiment.agilebutler.standup.model.StandupAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StandupAttachmentRepository extends JpaRepository<StandupAttachment, UUID> {
    List<StandupAttachment> findAllByStandupIdOrderByCreatedAtAsc(UUID standupId);

    Optional<StandupAttachment> findByIdAndStandupId(UUID id, UUID standupId);
}
