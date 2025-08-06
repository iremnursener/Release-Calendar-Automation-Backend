package com.example.ReleaseRadar.repositories;

import com.example.ReleaseRadar.entities.RecipientGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface IRecipientGroupMemberRepository extends JpaRepository<RecipientGroupMember, Long> {
    List<RecipientGroupMember> findByGroupId(Long groupId);




    @Query("""
    SELECT m.user.email
    FROM EmailReminder r
    JOIN r.group g
    JOIN RecipientGroupMember m ON m.group = g
    WHERE r.id = :reminderId
""")
    List<String> findEmailsByEmailReminderId(@Param("reminderId") Long reminderId);
}
