package ua.tarasov.hotline.service;

import ua.tarasov.hotline.entities.BotUser;
import ua.tarasov.hotline.models.Department;
import ua.tarasov.hotline.models.Role;

import java.util.List;
import java.util.Optional;

public interface BotUserService {
    void saveBotUser(BotUser botUser);

    Optional<BotUser> findById(Long botUserId);

    List<BotUser> findAll();

    List<BotUser> findAllByDepartment(Department department);

    BotUser findByRole(Role role);
    List<BotUser> findAllByRole(Role role);

    Optional<BotUser> findByPhone(String userPhone);
}
