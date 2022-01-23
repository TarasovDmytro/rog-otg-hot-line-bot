package ua.tarasov.hotline.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.tarasov.hotline.entities.BotUser;
import ua.tarasov.hotline.models.Department;
import ua.tarasov.hotline.models.Role;

import java.util.List;

public interface BotUserRepository extends JpaRepository<BotUser, Long> {

    List<BotUser> findAllByDepartmentsContains(Department department);

    BotUser findByRole(Role role);
}
