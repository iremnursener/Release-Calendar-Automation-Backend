package com.example.ReleaseRadar.repositories;


import com.example.ReleaseRadar.entities.Color;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IColorRepository extends JpaRepository<Color, Long> {
    Optional<Color> findByColorHex(String hex);
}
