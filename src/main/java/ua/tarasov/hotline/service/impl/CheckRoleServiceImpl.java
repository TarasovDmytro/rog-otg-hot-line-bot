package ua.tarasov.hotline.service.impl;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ua.tarasov.hotline.entities.BotUser;
import ua.tarasov.hotline.models.Role;
import ua.tarasov.hotline.service.CheckRoleService;

@Service
public class CheckRoleServiceImpl implements CheckRoleService {
    private final BotUserServiceImpl botUserService;

    public CheckRoleServiceImpl(BotUserServiceImpl botUserService) {
        this.botUserService = botUserService;
    }

    @Override
    public boolean checkIsAdmin(@NotNull Long userId) {
        BotUser botUser = new BotUser();
        if (botUserService.findById(userId).isPresent()) {
            botUser = botUserService.findById(userId).get();
        }
        return botUser.getRole().equals(Role.ADMIN) || botUser.getRole().equals(Role.SUPER_ADMIN);
    }

    @Override
    public SendMessage getFalseAdminText(@NotNull Long userId) {
        return SendMessage.builder()
                .chatId(String.valueOf(userId))
                .text("""
                        Вибачте,
                        Ви не можете виконати цю дію.
                        Для цього необхідно мати
                        права адміністратора""")
                .build();
    }
}
