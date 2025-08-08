package com.example.ReleaseRadar.migration;

import com.example.ReleaseRadar.entities.*;
import com.example.ReleaseRadar.enums.CriticalityLevel;
import com.example.ReleaseRadar.enums.EventType;
import com.example.ReleaseRadar.repositories.*;
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
    private final IEmailTemplateRepository emailTemplateRepository;
    private final IRecipientGroupRepository recipientGroupRepository;
    private final IRecipientGroupMemberRepository recipientGroupMemberRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            userRepository.saveAll(List.of(
                    new User(1L, "Ahmet", "Yılmaz", "irem.sener19@gmail.com",  "5551112233","DEV", true, LocalDateTime.now(), null),
                    new User(2L, "Ayşe", "Kara", "tunayagmurtb@gmail.com", "5554445566", "DEV", true, LocalDateTime.now(), null),
                    new User(3L, "Mehmet", "Demir", "ismail11burak35@gmail.com", "5557778899", "QA", true, LocalDateTime.now(), null)
            ));
        }

        if (colorRepository.count() == 0) {
            colorRepository.saveAll(List.of(
                    new Color(1L, "#FFFF00", "SARI", CriticalityLevel.LOW, EventType.IMPORTANT_PATCH, true, LocalDateTime.now(), null),
                    new Color(2L, "#92D050", "YEŞİL", CriticalityLevel.LOW, EventType.RELEASE, true,LocalDateTime.now(), null),
                    new Color(3L, "#FF0000", "KIRMIZI", CriticalityLevel.LOW, EventType.FREEZE, true, LocalDateTime.now(), null),
                    new Color(4L, "#00B0F0", "MAVİ", CriticalityLevel.LOW, EventType.BUSY_DAY, true, LocalDateTime.now(), null),
                    new Color(5L, "#972109", "AÇIK_KIRMIZI", CriticalityLevel.LOW, EventType.IMPORTANT_DRC, true, LocalDateTime.now(), null)
            ));
        }

        if (emailTemplateRepository.count() == 0) {
            emailTemplateRepository.saveAll(List.of(
                    EmailTemplate.builder()
                            .name("Freeze Template")
                            .subjectTpl("Yaklaşan Freeze Günü!")
                            .bodyHtmlTpl("<p>Değerli ekip, {{date}} tarihinde sistemlerimiz freeze moduna geçecektir.</p>")
                            .bodyTextTpl("Değerli ekip, {{date}} tarihinde sistemlerimiz freeze moduna geçecektir.")
                            .isActive(true)
                            .build(),

                    EmailTemplate.builder()
                            .name("Release Template")
                            .subjectTpl("Yaklaşan Release Günü!")
                            .bodyHtmlTpl("<p>{{date}} tarihinde yeni versiyon yayına alınacaktır.</p>")
                            .bodyTextTpl("{{date}} tarihinde yeni versiyon yayına alınacaktır.")
                            .isActive(true)
                            .build(),

                    EmailTemplate.builder()
                            .name("Busy Day Template")
                            .subjectTpl("Yaklaşan Yoğun İşlem Günü!")
                            .bodyHtmlTpl("<p>{{date}} tarihinde yoğun işlem yapılacaktır, dikkatli olunuz.</p>")
                            .bodyTextTpl("{{date}} tarihinde yoğun işlem yapılacaktır, dikkatli olunuz.")
                            .isActive(true)
                            .build(),

                    EmailTemplate.builder()
                            .name("Important Patch Template")
                            .subjectTpl("Unix/Windows Patch Geçişi Bildirimi")
                            .bodyHtmlTpl("<p>Unix / Windows Patch Geçişleri yapılacak olup, {{date}} tarihinde belirtilen sunucularda çalışma alınmayacaktır.</p>")
                            .bodyTextTpl("Unix / Windows Patch Geçişleri yapılacak olup, {{date}} tarihinde belirtilen sunucularda çalışma alınmayacaktır.")
                            .isActive(true)
                            .build(),

                    EmailTemplate.builder()
                            .name("Important DRC Template")
                            .subjectTpl("DRC Switch Bildirimi")
                            .bodyHtmlTpl("<p>DRC Switch çalışmaları yapılacaktır. {{date}} tarihine etkili çalışma alınmayacaktır.</p>")
                            .bodyTextTpl("DRC Switch çalışmaları yapılacaktır. {{date}} tarihine etkili çalışma alınmayacaktır.")
                            .isActive(true)
                            .build()
            ));
        }
        if (recipientGroupRepository.count() == 0) {
            // 1. Grup oluştur
            RecipientGroup devTeam = RecipientGroup.builder()
                    .groupName("DEV Team")
                    .description("All Developers")
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            RecipientGroup qaTeam = RecipientGroup.builder()
                    .groupName("QA Team")
                    .description("All QA Engineers")
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            RecipientGroup allStakeholders = RecipientGroup.builder()
                    .groupName("All Stakeholders")
                    .description("Everyone")
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            recipientGroupRepository.saveAll(List.of(devTeam, qaTeam, allStakeholders));

            // 2. Kullanıcıları gruplara ata
            List<User> allUsers = userRepository.findAll();

            for (User user : allUsers) {
                String role = user.getDepartment(); // "DEV", "QA"

                RecipientGroupMember member = RecipientGroupMember.builder()
                        .user(user)
                        .group(allStakeholders)
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .build();
                recipientGroupMemberRepository.save(member);

                if ("DEV".equalsIgnoreCase(role)) {
                    recipientGroupMemberRepository.save(RecipientGroupMember.builder()
                            .user(user)
                            .group(devTeam)
                            .isActive(true)
                            .createdAt(LocalDateTime.now())
                            .build());
                }

                if ("QA".equalsIgnoreCase(role)) {
                    recipientGroupMemberRepository.save(RecipientGroupMember.builder()
                            .user(user)
                            .group(qaTeam)
                            .isActive(true)
                            .createdAt(LocalDateTime.now())
                            .build());
                }
            }
        }

    }
}

