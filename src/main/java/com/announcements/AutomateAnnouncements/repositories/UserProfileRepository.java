package com.announcements.AutomateAnnouncements.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.announcements.AutomateAnnouncements.entities.UserProfile;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Integer> {
}