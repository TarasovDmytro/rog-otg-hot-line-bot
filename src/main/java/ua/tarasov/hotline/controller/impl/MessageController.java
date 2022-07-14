package ua.tarasov.hotline.controller.impl;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ua.tarasov.hotline.controller.Controller;
import ua.tarasov.hotline.entities.BotUser;
import ua.tarasov.hotline.models.BotState;
import ua.tarasov.hotline.models.Role;
import ua.tarasov.hotline.service.BotUserService;
import ua.tarasov.hotline.service.CheckRoleService;
import ua.tarasov.hotline.service.KeyboardService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MessageController implements Controller {

    final CheckRoleService checkRoleService;
    final BotUserService botUserService;
    final KeyboardService keyboardService;

    public MessageController(CheckRoleService checkRoleService, BotUserService botUserService, KeyboardService keyboardService) {
        this.checkRoleService = checkRoleService;
        this.botUserService = botUserService;
        this.keyboardService = keyboardService;
    }

    public List<BotApiMethod<?>> setMessageToAll(Message message) {
        if (checkRoleService.checkIsAdmin(message.getChatId())) {
            chatPropertyModeService.setCurrentBotState(message.getChatId(), BotState.WAIT_MESSAGE_TO_ALL);
            return Controller.getSimpleResponseToRequest(message, """
                    Введіть, будьласка, повідомлення
                    для всіх користувачів""");
        }
        return Collections.singletonList(checkRoleService.getFalseAdminText(message.getChatId()));
    }

    @NotNull
    public List<BotApiMethod<?>> sendMessageToAll(Message message) {
        if (checkRoleService.checkIsAdmin(message.getChatId())) {
            List<BotApiMethod<?>> answerMessages = new ArrayList<>();
            List<BotUser> botUsers = botUserService.findAll();
            botUsers.forEach(botUser -> answerMessages.add(SendMessage.builder()
                    .chatId(String.valueOf(botUser.getId()))
                    .text(message.getText())
                    .parseMode("HTML")
                    .entities(message.getEntities())
                    .build()));
            chatPropertyModeService.setCurrentBotState(message.getChatId(), BotState.WAIT_BUTTON);
            return answerMessages;
        }
        chatPropertyModeService.setCurrentBotState(message.getChatId(), BotState.WAIT_BUTTON);
        return Collections.singletonList(checkRoleService.getFalseAdminText(message.getChatId()));
    }

    @NotNull
    @Unmodifiable
    public List<BotApiMethod<?>> setLocationMessage(@NotNull CallbackQuery callbackQuery) {
        chatPropertyModeService.setCurrentBotState(callbackQuery.getMessage().getChatId(), BotState.WAIT_LOCATION);
        return Collections.singletonList(SendMessage.builder()
                .chatId(callbackQuery.getMessage().getChatId().toString())
                .text("""
                        Дякую, відправте, будьласка, Вашу поточну геолокацію.
                        Це можна зробити, натиснув на позначку 'скрепки' поруч
                        із полем для вводу текста.\s
                        Увага! Телеграм підтримує цю послугу тільки у версії для
                        смартфонів, якщо Ви використовуєте інший пристрій, або
                        передумали - натиснить кнопку 'відмовитись'""")
                .replyMarkup(InlineKeyboardMarkup.builder()
                        .keyboard(keyboardService.getRefuseButton(callbackQuery.getMessage()))
                        .build())
                .build());
    }

    public List<BotApiMethod<?>> setRequestAddressMessage(@NotNull CallbackQuery callbackQuery) {
        chatPropertyModeService.setCurrentBotState(callbackQuery.getMessage().getChatId(), BotState.WAIT_ADDRESS);
        return Controller.getSimpleResponseToRequest(callbackQuery.getMessage(), "Добре. Введіть, будьласка, адресу," +
                " за якою сталася проблема");
    }

    @NotNull
    @Unmodifiable
    public List<BotApiMethod<?>> setRefuseRequestMessage(@NotNull CallbackQuery callbackQuery) {
        BotUser superAdmin = botUserService.findByRole(Role.SUPER_ADMIN);
        return (List.of(SendMessage.builder()
                        .chatId(String.valueOf(callbackQuery.getMessage().getChatId()))
                        .text("Вибачте, Вам відмовлено в зміні прав доступу")
                        .build(),
                SendMessage.builder()
                        .chatId(String.valueOf(superAdmin.getId()))
                        .text("Відмовлено")
                        .build()));
    }

    public List<BotApiMethod<?>> refuseSetLocationOfRequestMessage(CallbackQuery callbackQuery) {
        return setRequestAddressMessage(callbackQuery);
    }
}
