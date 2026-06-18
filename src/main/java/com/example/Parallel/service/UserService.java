package com.example.Parallel.service;

import com.example.Parallel.entity.User;
import com.example.Parallel.exception.ResourceNotFoundException;
import com.example.Parallel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository ;


    public User getUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User does not exist"));

        return user;
    }
}
