package ua.tarasov.hotline.service;

import org.jetbrains.annotations.NotNull;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
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

    Optional<BotUser> findByPhone(String userPhone);
    boolean checkIsAdmin(@NotNull Long userId);
    boolean checkIsSuperAdmin(Long userId);
    SendMessage getFalseAdminText(@NotNull Long userId);

    void deleteUser(BotUser botUser);
}
