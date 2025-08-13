package NetGuard.Login_Backend.Repository;

import NetGuard.Login_Backend.Model.Parent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ParentRepository extends JpaRepository<Parent, Long> {

    Optional<Parent> findByEmail(String email);

    // New method needed by AuthService
    Optional<Parent> findByEmailAndActiveTrue(String email);

    boolean existsByEmail(String email);

    // New method needed by AuthService
    boolean existsByPhone(String phone);

    // Additional useful methods for the enhanced system
    @Query("SELECT p FROM Parent p WHERE p.email = :email AND p.active = true AND p.emailVerified = true")
    Optional<Parent> findByEmailAndActiveTrueAndEmailVerifiedTrue(@Param("email") String email);

    @Query("SELECT COUNT(p) FROM Parent p WHERE p.active = true")
    long countActiveParents();

    @Query("SELECT COUNT(p) FROM Parent p WHERE p.emailVerified = true AND p.active = true")
    long countVerifiedActiveParents();
}
