package com.example.ReleaseRadar.repositories;

import com.example.ReleaseRadar.entities.RecipientGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IRecipientGroupRepository  extends JpaRepository<RecipientGroup, Long> {

    Optional<RecipientGroup> findByGroupName(String groupname);
}
