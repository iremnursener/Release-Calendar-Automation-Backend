package com.example.ReleaseRadar.migration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import com.example.ReleaseRadar.entities.*;
import com.example.ReleaseRadar.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock private IUserRepository userRepository;
    @Mock private IColorRepository colorRepository;
    @Mock private IEmailTemplateRepository emailTemplateRepository;
    @Mock private IRecipientGroupRepository recipientGroupRepository;
    @Mock private IRecipientGroupMemberRepository recipientGroupMemberRepository;

    private DataInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new DataInitializer(
                userRepository,
                colorRepository,
                emailTemplateRepository,
                recipientGroupRepository,
                recipientGroupMemberRepository
        );
    }

    @Test
    void run_whenAllCountsZero_seedsUsersColorsTemplatesGroupsAndMembers() throws Exception {
        // GIVEN: tüm tablolar boş
        when(userRepository.count()).thenReturn(0L);
        when(colorRepository.count()).thenReturn(0L);
        when(emailTemplateRepository.count()).thenReturn(0L);
        when(recipientGroupRepository.count()).thenReturn(0L);

        // Gruplar oluşturulduktan sonra üyelikler için kullanıcılar okunacak
        List<User> users = List.of(
                // department alanları DEV/QA olmalı ki doğru gruplara dağıtılsın
                new User(1L, "Ahmet", "Yılmaz", "dev1@ex.com",  "5551112233", "DEV", true, null, null),
                new User(2L, "Ayşe",  "Kara",   "dev2@ex.com",  "5554445566", "DEV", true, null, null),
                new User(3L, "Mehmet","Demir",  "qa1@ex.com",   "5557778899", "QA",  true, null, null)
        );
        when(userRepository.findAll()).thenReturn(users);

        // WHEN
        initializer.run();

        // THEN: Users 3 adet
        ArgumentCaptor<List<User>> usersCap = ArgumentCaptor.forClass(List.class);
        verify(userRepository).saveAll(usersCap.capture());
        assertEquals(3, usersCap.getValue().size());

        // Colors 5 adet
        ArgumentCaptor<List<Color>> colorsCap = ArgumentCaptor.forClass(List.class);
        verify(colorRepository).saveAll(colorsCap.capture());
        assertEquals(5, colorsCap.getValue().size());

        // Email templates 5 adet
        ArgumentCaptor<List<EmailTemplate>> tplCap = ArgumentCaptor.forClass(List.class);
        verify(emailTemplateRepository).saveAll(tplCap.capture());
        assertEquals(5, tplCap.getValue().size());

        // Gruplar 3 adet: DEV Team, QA Team, All Stakeholders
        ArgumentCaptor<List<RecipientGroup>> grpCap = ArgumentCaptor.forClass(List.class);
        verify(recipientGroupRepository).saveAll(grpCap.capture());
        List<RecipientGroup> groupsSaved = grpCap.getValue();
        assertEquals(3, groupsSaved.size());
        var names = groupsSaved.stream().map(RecipientGroup::getGroupName).toList();
        assertTrue(names.containsAll(List.of("DEV Team", "QA Team", "All Stakeholders")));

        // Üyelikler:
        // - Her kullanıcı All Stakeholders: 3 kayıt
        // - DEV olan 2 kişi DEV Team: 2 kayıt
        // - QA olan 1 kişi QA Team: 1 kayıt
        // Toplam 6 kayıt
        ArgumentCaptor<RecipientGroupMember> memCap = ArgumentCaptor.forClass(RecipientGroupMember.class);
        verify(recipientGroupMemberRepository, times(6)).save(memCap.capture());
        var savedMembers = memCap.getAllValues();
        assertEquals(6, savedMembers.size());

        // Grupların isim dağılımı beklenen gibi mi?
        long allStakeholdersCount = savedMembers.stream()
                .filter(m -> m.getGroup() != null && "All Stakeholders".equals(m.getGroup().getGroupName()))
                .count();
        long devTeamCount = savedMembers.stream()
                .filter(m -> m.getGroup() != null && "DEV Team".equals(m.getGroup().getGroupName()))
                .count();
        long qaTeamCount = savedMembers.stream()
                .filter(m -> m.getGroup() != null && "QA Team".equals(m.getGroup().getGroupName()))
                .count();

        assertEquals(3, allStakeholdersCount);
        assertEquals(2, devTeamCount);
        assertEquals(1, qaTeamCount);

        // Kullanıcılar gruplara dağıtılırken listesi okunmuş olmalı
        verify(userRepository).findAll();

        verifyNoMoreInteractions(recipientGroupMemberRepository);
    }

    @Test
    void run_whenAllCountsNonZero_doesNothing() throws Exception {
        // GIVEN: hepsi dolu
        when(userRepository.count()).thenReturn(1L);
        when(colorRepository.count()).thenReturn(1L);
        when(emailTemplateRepository.count()).thenReturn(1L);
        when(recipientGroupRepository.count()).thenReturn(1L);

        // WHEN
        initializer.run();

        // THEN: hiçbir save çağrısı olmamalı
        verify(userRepository, never()).saveAll(anyList());
        verify(colorRepository, never()).saveAll(anyList());
        verify(emailTemplateRepository, never()).saveAll(anyList());
        verify(recipientGroupRepository, never()).saveAll(anyList());
        verifyNoInteractions(recipientGroupMemberRepository);
    }
}