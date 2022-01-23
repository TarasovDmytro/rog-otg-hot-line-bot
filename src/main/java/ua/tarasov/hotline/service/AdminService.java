package ua.tarasov.hotline.service;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ua.tarasov.hotline.entities.BotUser;
import ua.tarasov.hotline.models.Role;

@Service
public class AdminService {
    private final BotUserService botUserService;

    public AdminService(BotUserService botUserService) {
        this.botUserService = botUserService;
    }

    public boolean checkIsAdmin(Message message) {
        BotUser botUser = new BotUser();
        if (botUserService.findById(message.getChatId()).isPresent()) {
            botUser = botUserService.findById(message.getChatId()).get();
        }
        return botUser.getRole().equals(Role.ADMIN)||botUser.getRole().equals(Role.SUPER_ADMIN);
    }

    public SendMessage getFalseAdminText(Message message) {
        return SendMessage.builder()
                .chatId(String.valueOf(message.getChatId()))
                .text("""
                        Вибачте,
                        Ви не можете виконати цю дію.
                        Для цього необхідно мати
                        права адміністратора""")
                .build();
    }
}
