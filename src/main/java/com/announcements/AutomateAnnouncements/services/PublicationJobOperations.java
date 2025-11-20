package com.announcements.AutomateAnnouncements.services;

import java.util.List;
import java.util.Optional;

import com.announcements.AutomateAnnouncements.dtos.request.PublicationJobRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.response.PublicationJobResponseDTO;

public interface PublicationJobOperations {

    List<PublicationJobResponseDTO> getAll();

    PublicationJobResponseDTO create(PublicationJobRequestDTO dto);

    Optional<PublicationJobResponseDTO> getById(Integer id);

    boolean delete(Integer id);
}
