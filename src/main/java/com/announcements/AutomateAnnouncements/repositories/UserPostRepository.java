package com.announcements.AutomateAnnouncements.repositories;

import com.announcements.AutomateAnnouncements.entities.UserPost;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPostRepository extends JpaRepository<UserPost, UUID> {

    @EntityGraph(attributePaths = { "publications", "tags", "targetPlatforms" })
    List<UserPost> findByOwnerAuthUserIdOrderByCreatedAtDesc(String authUserId);

    @EntityGraph(attributePaths = { "publications", "tags", "targetPlatforms" })
    List<UserPost> findByOwnerIdOrderByCreatedAtDesc(Integer ownerId);

    @EntityGraph(attributePaths = { "publications", "tags", "targetPlatforms" })
    List<UserPost> findByOwnerEmailIgnoreCaseOrderByCreatedAtDesc(String email);
}
