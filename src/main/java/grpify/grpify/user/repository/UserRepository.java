package grpify.grpify.user.repository;

import grpify.grpify.user.domain.User;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByName(String name);
    Optional<User> findByEmail(String email);

    boolean existsByName(String name);
    boolean existsByEmail(String email);

}
