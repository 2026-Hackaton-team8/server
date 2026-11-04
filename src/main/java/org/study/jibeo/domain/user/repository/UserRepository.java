package org.study.jibeo.domain.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.study.jibeo.domain.user.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByLoginId(String loginId);

    Optional<User> findByLoginId(String loginId);
}
