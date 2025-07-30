package com.example.ReleaseRadar.repositories;

import com.example.ReleaseRadar.entities.RecipientGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface IRecipientGroupMemberRepository extends JpaRepository<RecipientGroupMember, Long> {
    List<RecipientGroupMember> findByGroupId(Long groupId);
}
