package com.example.ReleaseRadar.migration;

import com.example.ReleaseRadar.entities.Color;
import com.example.ReleaseRadar.entities.User;
import com.example.ReleaseRadar.enums.CriticalityLevel;
import com.example.ReleaseRadar.enums.EventType;
import com.example.ReleaseRadar.repositories.IColorRepository;
import com.example.ReleaseRadar.repositories.IUserRepository;
import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;


@Component
@AllArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final IUserRepository userRepository;
    private final IColorRepository colorRepository;


    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            userRepository.saveAll(List.of(
                    new User(1L, "Ahmet", "Yılmaz", "IT", "ahmet.yilmaz@example.com", "5551112233", true, LocalDateTime.now(), null),
                    new User(2L, "Ayşe", "Kara", "HR", "ayse.kara@example.com", "5554445566", true, LocalDateTime.now(), null),
                    new User(3L, "Mehmet", "Demir", "Finance", "mehmet.demir@example.com", "5557778899", true, LocalDateTime.now(), null)
            ));
        }

        if (colorRepository.count() == 0) {
            colorRepository.saveAll(List.of(
                    new Color(1L, "#FFFF00", "SARI", CriticalityLevel.LOW, EventType.IMPORTANT, true, LocalDateTime.now(), null),
                    new Color(2L, "#92D050", "YEŞİL", CriticalityLevel.LOW, EventType.RELEASE, true,LocalDateTime.now(), null),
                    new Color(3L, "#FF0000", "KIRMIZI", CriticalityLevel.LOW, EventType.FREEZE, true, LocalDateTime.now(), null),
                    new Color(4L, "#00B0F0", "MAVİ", CriticalityLevel.LOW, EventType.BUSY_DAY, true, LocalDateTime.now(), null),
                    new Color(5L, "#972109", "AÇIK_KIRMIZI", CriticalityLevel.LOW, EventType.IMPORTANT, true, LocalDateTime.now(), null)
            ));
        }
    }
}

