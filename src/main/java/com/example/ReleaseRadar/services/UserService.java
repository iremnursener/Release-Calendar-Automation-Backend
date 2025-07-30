package com.example.ReleaseRadar.services;

import com.example.ReleaseRadar.entities.User;
import com.example.ReleaseRadar.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UserService {

private final IUserRepository userRepository;

//    public User getCurrentLoggedInUser() {
//        String email = SecurityContextHolder.getContext().getAuthentication().getName();
//        return userRepository.findByEmail(email)
//                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
//    }

}
