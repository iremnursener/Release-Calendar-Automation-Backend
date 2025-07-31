package com.example.ReleaseRadar.repositories;

import com.example.ReleaseRadar.entities.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface IEventRepository extends JpaRepository<Event, Long> {

    List<Event> findByDate(LocalDate date);
}
