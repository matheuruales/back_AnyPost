package com.announcements.AutomateAnnouncements.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.announcements.AutomateAnnouncements.entities.PublicationJob;

@Repository
public interface PublicationJobRepository extends JpaRepository<PublicationJob, Integer> {
}