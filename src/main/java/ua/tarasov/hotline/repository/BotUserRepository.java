package ua.tarasov.hotline.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.tarasov.hotline.entities.BotUser;
import ua.tarasov.hotline.models.Department;
import ua.tarasov.hotline.models.Role;

import java.util.List;
import java.util.Optional;

public interface BotUserRepository extends JpaRepository<BotUser, Long> {

    List<BotUser> findAllByDepartmentsContains(Department department);

    BotUser findByRole(Role role);
    List<BotUser> findAllByRole(Role role);

    Optional<BotUser> findByPhone(String phone);
}
