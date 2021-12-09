package ua.tarasov.hotline.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.tarasov.hotline.models.entities.BotUser;
import ua.tarasov.hotline.models.model.Departments;
import ua.tarasov.hotline.models.model.Role;

import java.util.List;

public interface BotUserRepository extends JpaRepository<BotUser, Long> {

    List<BotUser> findAllByDepartmentsContains(Departments department);
    BotUser findBotUserByPhone(String phone);

    BotUser findByRole(Role role);
}
