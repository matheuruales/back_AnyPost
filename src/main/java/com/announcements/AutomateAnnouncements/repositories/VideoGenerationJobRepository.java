package com.announcements.AutomateAnnouncements.repositories;

import com.announcements.AutomateAnnouncements.entities.VideoGenerationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoGenerationJobRepository extends JpaRepository<VideoGenerationJob, Integer> {

    List<VideoGenerationJob> findByStatusIn(List<String> statuses);

    List<VideoGenerationJob> findByOwnerIdAndStatus(Integer ownerId, String status);

    @Query("SELECT j FROM VideoGenerationJob j WHERE j.status IN :statuses ORDER BY j.createdAt ASC")
    List<VideoGenerationJob> findPendingJobs(@Param("statuses") List<String> statuses);
}