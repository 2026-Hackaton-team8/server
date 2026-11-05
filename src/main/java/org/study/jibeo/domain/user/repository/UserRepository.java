package org.study.jibeo.domain.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.study.jibeo.domain.user.model.User;
import org.study.jibeo.domain.user.model.UserRole;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByLoginId(String loginId);

    Optional<User> findByLoginId(String loginId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.company WHERE u.id = :id")
    Optional<User> findWithCompanyById(@Param("id") Long id);

    @Query("SELECT u FROM User u WHERE u.company.id = :companyId AND u.role = :role ORDER BY u.name ASC")
    List<User> findAllByCompanyAndRole(@Param("companyId") Long companyId, @Param("role") UserRole role);
}
