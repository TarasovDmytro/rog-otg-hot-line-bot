package ua.tarasov.hotline.controller.impl;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ua.tarasov.hotline.controller.Controller;
import ua.tarasov.hotline.entities.BotUser;
import ua.tarasov.hotline.models.BotState;
import ua.tarasov.hotline.models.Role;
import ua.tarasov.hotline.models.StateOfRequest;
import ua.tarasov.hotline.service.BotUserService;
import ua.tarasov.hotline.service.KeyboardService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MessageController implements Controller {

    final BotUserService botUserService;
    final KeyboardService keyboardService;

    public MessageController(BotUserService botUserService, KeyboardService keyboardService) {
        this.botUserService = botUserService;
        this.keyboardService = keyboardService;
    }

    @NotNull
    public List<BotApiMethod<?>> sendMessageToAll(Message message) {
        if (botUserService.checkIsAdmin(message.getChatId())) {
            List<BotApiMethod<?>> answerMessages = new ArrayList<>();
            List<BotUser> botUsers = botUserService.findAll();
            botUsers.forEach(botUser ->
                    answerMessages.addAll(List.of(SendMessage.builder()
                                    .chatId(String.valueOf(botUser.getId()))
                                    .text("\uD83D\uDCE3")
                                    .build(),
                            ForwardMessage.builder()
                                    .chatId(String.valueOf(botUser.getId()))
                                    .fromChatId(String.valueOf(message.getChatId()))
                                    .messageId(message.getMessageId())
                                    .build())));
            chatPropertyModeService.setCurrentBotState(message.getChatId(), BotState.WAIT_BUTTON);
            return answerMessages;
        }
        chatPropertyModeService.setCurrentBotState(message.getChatId(), BotState.WAIT_BUTTON);
        return Collections.singletonList(botUserService.getFalseAdminText(message.getChatId()));
    }

    @NotNull
    @Unmodifiable
    public List<BotApiMethod<?>> setLocationMessage(@NotNull CallbackQuery callbackQuery) {
        chatPropertyModeService.setCurrentBotState(callbackQuery.getMessage().getChatId(), BotState.WAIT_LOCATION);
        return Collections.singletonList(SendMessage.builder()
                .chatId(callbackQuery.getMessage().getChatId().toString())
                .text("""
                        Дякую, відправте, будь ласка, Вашу поточну геолокацію.
                        Це можна зробити, натиснув на позначку 'скріпки' поруч
                        із полем для вводу тексту.\s
                        Увага! Телеграм підтримує цю послугу тільки у версії для
                        смартфонів, якщо Ви використовуєте інший пристрій, або
                        передумали - натисніть кнопку 'відмовитись'""")
                .replyMarkup(InlineKeyboardMarkup.builder()
                        .keyboard(keyboardService.getRefuseButton(callbackQuery.getMessage()))
                        .build())
                .build());
    }

    public List<BotApiMethod<?>> setRequestAddressMessage(@NotNull Message message) {
        chatPropertyModeService.setCurrentBotState(message.getChatId(), BotState.WAIT_ADDRESS);
        chatPropertyModeService.setCurrentStateOfRequest(message.getChatId(), StateOfRequest.SET_ADDRESS);
        return Controller.getSimpleResponseToRequest(message, "Введіть, будь ласка, адресу," +
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
        return setRequestAddressMessage(callbackQuery.getMessage());
    }
}
