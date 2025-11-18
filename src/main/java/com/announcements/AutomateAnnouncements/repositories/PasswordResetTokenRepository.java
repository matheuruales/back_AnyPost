package com.announcements.AutomateAnnouncements.repositories;

import com.announcements.AutomateAnnouncements.entities.PasswordResetToken;
import com.announcements.AutomateAnnouncements.entities.UserProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    List<PasswordResetToken> findByUserProfileAndUsedFalse(UserProfile userProfile);

    Optional<PasswordResetToken> findTopByUserProfileAndCodeAndUsedFalseOrderByCreatedAtDesc(
            UserProfile userProfile,
            String code
    );
}
