package com.microtech.smartshop.repository;
import com.microtech.smartshop.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, String> {
    Optional<Client> findByEmail(String email);
    Optional<Client> findByUserId(String userId);
}
