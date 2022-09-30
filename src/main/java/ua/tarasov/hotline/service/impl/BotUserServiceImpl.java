package ua.tarasov.hotline.service.impl;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ua.tarasov.hotline.entities.BotUser;
import ua.tarasov.hotline.models.Department;
import ua.tarasov.hotline.models.Role;
import ua.tarasov.hotline.repository.BotUserRepository;
import ua.tarasov.hotline.service.BotUserService;

import java.util.List;
import java.util.Optional;

@Service
public class BotUserServiceImpl implements BotUserService {
    private final BotUserRepository botUserRepository;

    public BotUserServiceImpl(BotUserRepository botUserRepository) {
        this.botUserRepository = botUserRepository;
    }

    @Override
    public void saveBotUser(BotUser botUser) {
        botUserRepository.save(botUser);
    }

    @Override
    public Optional<BotUser> findById(Long botUserId) {
        return botUserRepository.findById(botUserId);
    }

    @Override
    public List<BotUser> findAll() {
        return botUserRepository.findAll();
    }

    @Override
    public List<BotUser> findAllByDepartment(Department department) {
        return botUserRepository.findAllByDepartmentsContains(department);
    }

    @Override
    public BotUser findByRole(Role role) {
        return botUserRepository.findByRole(role);
    }

    @Override
    public Optional<BotUser> findByPhone(String userPhone) {
        return botUserRepository.findByPhone(userPhone);
    }
    @Override
    public boolean checkIsAdmin(@NotNull Long userId) {
        BotUser botUser = new BotUser();
        if (findById(userId).isPresent()) {
            botUser = findById(userId).get();
        }
        if (botUser.getRole() != null) {
            return botUser.getRole().equals(Role.ADMIN) || botUser.getRole().equals(Role.SUPER_ADMIN);
        } else return false;
    }
    @Override
    public SendMessage getFalseAdminText(@NotNull Long userId) {
        return SendMessage.builder()
                .chatId(String.valueOf(userId))
                .text("""
                        Вибачте,
                        Ваші права доступу не дозволяють
                        виконати цю дію.""")
                .build();
    }
}
