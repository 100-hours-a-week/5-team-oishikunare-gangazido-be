package org.example.gangazido_be.pet.repository;

import org.example.gangazido_be.pet.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
