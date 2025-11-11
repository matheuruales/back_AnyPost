package com.announcements.AutomateAnnouncements.repositories;

import com.announcements.AutomateAnnouncements.entities.UserProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Integer> {
    Optional<UserProfile> findByAuthUserId(String authUserId);
    Optional<UserProfile> findByEmail(String email);
    Optional<UserProfile> findByEmailIgnoreCase(String email);
}
