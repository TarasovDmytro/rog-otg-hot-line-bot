package ua.tarasov.hotline.handlers.impl;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.tarasov.hotline.controller.Controller;
import ua.tarasov.hotline.controller.impl.*;
import ua.tarasov.hotline.entities.BotUser;
import ua.tarasov.hotline.handlers.RequestHandler;
import ua.tarasov.hotline.models.StateOfRequest;
import ua.tarasov.hotline.service.BotUserService;
import ua.tarasov.hotline.service.ChatPropertyModeService;
import ua.tarasov.hotline.service.KeyboardService;
import ua.tarasov.hotline.service.impl.ChatPropertyModeServiceImpl;
import ua.tarasov.hotline.service.impl.KeyboardServiceImpl;

import java.util.List;

@Component
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MessageHandler implements RequestHandler {
    final KeyboardService keyboardService;
    final ChatPropertyModeService chatPropertyModeService;
    final BotUserController botUserController;
    final UserRequestController userRequestController;
    final NotificationController notificationController;
    final SuperAdminController superAdminController;
    final MessageController messageController;
    final BotUserService userService;

    public MessageHandler(KeyboardServiceImpl keyboardService,
                          @Qualifier("getChatProperties") ChatPropertyModeServiceImpl chatPropertyModeService,
                          BotUserController botUserController, UserRequestController userRequestController,
                          NotificationController notificationController, SuperAdminController superAdminController,
                          MessageController messageController, BotUserService userService) {
        this.keyboardService = keyboardService;
        this.chatPropertyModeService = chatPropertyModeService;
        this.botUserController = botUserController;
        this.userRequestController = userRequestController;
        this.notificationController = notificationController;
        this.superAdminController = superAdminController;
        this.messageController = messageController;
        this.userService = userService;
    }

    @Override
    public List<BotApiMethod<?>> getHandlerUpdate(@NotNull Update update) {
        log.info("messageHandler get update = {}", update);
        Message message = update.getMessage();
        Long userId = message.getChatId();
        BotUser user = new BotUser();
        if (message.hasEntities() && message.hasText()) {
            String text = message.getText();
            switch (text) {
                case "/start" -> {
                    return botUserController.setStartProperties(message);
                }
                case "/new_admin" -> {
                    chatPropertyModeService.setCurrentStateOfRequest(message.getChatId(), StateOfRequest.WAIT_PHONE);
                    return superAdminController.changeRoleRequest(message);
                }
            }
        }
        if (userService.findById(userId).isPresent()) {
            user = userService.findById(userId).get();
        }
        if (user.getWarningCount() < 3) {
            if (chatPropertyModeService.getStateOfRequest(message.getChatId()).equals(StateOfRequest.SET_ROLES) ||
                    chatPropertyModeService.getStateOfRequest(message.getChatId()).equals(StateOfRequest.SET_PHONE)) {
                return superAdminController.changeRoleRequest(message);
            }
            if (!chatPropertyModeService.getStateOfRequest(message.getChatId()).equals(StateOfRequest.REQUEST_CREATED)) {
                return userRequestController.createRequest(message);
            }
            if (message.hasText()) {
                switch (message.getText()) {
                    case "\uD83D\uDCC4 Зробити заявку" -> {
                        chatPropertyModeService.setCurrentStateOfRequest(message.getChatId(), StateOfRequest.SET_DEPARTMENT);
                        return userRequestController.createRequest(message);
                    }
                    case "\uD83D\uDCDA Мої заявки" -> {
                        return userRequestController.getAllStatesRequestsOfUser(message);
                    }
                    case "\uD83D\uDCDA Всі заявки" -> {
                        return userRequestController.getAllStatesRequestsOfAdmin(message);
                    }
                    case "\uD83D\uDCD5 Мої не виконані заявки" -> {
                        return userRequestController.getFalseStateRequestsOfUser(message);
                    }
                    case "\uD83D\uDCD5 Не виконані заявки" -> {
                        return userRequestController.getFalseStateRequestsOfAdmin(message);
                    }
                    case "\uD83D\uDD01 Змінити меню" -> {
                        return keyboardService.setChangeMenu(message);
                    }
                    case "❌ Відмовитись" -> {
                        return keyboardService.setReplyKeyboardOfUser(message.getChatId(), START_TEXT);
                    }
                    case "\uD83D\uDD0A Останні оголошення" -> {
                        return notificationController.getNotifications(message);
                    }
                    default -> {
                        return Controller.getSimpleResponseToRequest(message, WRONG_ACTION_TEXT);
                    }
                }
            }
            if (message.hasContact()) return botUserController.setBotUserPhone(message);
            return messageController.sendMessageToAll(message);
        } else return List.of(SendMessage.builder()
                .chatId(String.valueOf(userId))
                .text("Вибачте, але Ви заблоковані за некоректне використання сервісу")
                .build());
    }
}
