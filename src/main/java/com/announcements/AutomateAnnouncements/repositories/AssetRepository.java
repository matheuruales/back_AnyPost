package com.announcements.AutomateAnnouncements.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.announcements.AutomateAnnouncements.entities.Asset;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Integer> {
}