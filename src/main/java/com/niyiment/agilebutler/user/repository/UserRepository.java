package com.niyiment.agilebutler.user.repository;

import com.niyiment.agilebutler.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.team.id = :teamId")
    List<User> findAllByTeamId(UUID teamId);

    @Query("SELECT u FROM User u WHERE u.team.id = :teamId AND u.active = true")
    List<User> findAllByTeamIdAndActiveTrue(UUID teamId);

    @Query("""
            SELECT u FROM User u
                WHERE u.active = true AND u.fcmToken IS NOT NULL 
                    AND u.notificationTime BETWEEN :from AND :to
            """)
    List<User> findUsersWithNotificationTimeBetween(LocalTime from, LocalTime to);


    @Query("""
            SELECT u FROM User u
                JOIN FETCH u.team
                    WHERE u.id = :id
            """)
    Optional<User> findByIdWithTeam(UUID id);

    @Query("SELECT u FROM User u WHERE u.active = true AND u.notificationTime IS NOT NULL")
    List<User> findAllActiveUsersWithNotificationTime();
}
