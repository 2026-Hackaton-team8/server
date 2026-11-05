package org.study.jibeo.domain.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.study.jibeo.domain.user.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByLoginId(String loginId);

    Optional<User> findByLoginId(String loginId);

    @Query("SELECT u FROM User u JOIN FETCH u.company WHERE u.id = :id")
    Optional<User> findWithCompanyById(@Param("id") Long id);
}
