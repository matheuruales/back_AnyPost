package com.announcements.AutomateAnnouncements.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.announcements.AutomateAnnouncements.entities.PostDraft;

@Repository
public interface PostDraftRepository extends JpaRepository<PostDraft, Integer> {
}