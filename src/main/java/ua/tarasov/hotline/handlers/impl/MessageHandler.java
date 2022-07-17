package ua.tarasov.hotline.handlers.impl;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Video;
import ua.tarasov.hotline.controller.Controller;
import ua.tarasov.hotline.controller.impl.*;
import ua.tarasov.hotline.handlers.RequestHandler;
import ua.tarasov.hotline.models.BotState;
import ua.tarasov.hotline.service.ChatPropertyModeService;
import ua.tarasov.hotline.service.KeyboardService;
import ua.tarasov.hotline.service.impl.*;

import java.util.List;

@Component
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MessageHandler implements RequestHandler {
    final KeyboardService keyboardService;
    final ChatPropertyModeService chatPropertyModeService;

    final BotUserController botUserController;
    final DepartmentController departmentController;
    final UserRequestController userRequestController;
    final NotificationController notificationController;
    final SuperAdminController superAdminController;
    final MessageController messageController;

    public MessageHandler(KeyboardServiceImpl keyboardService, @Qualifier("getChatProperties") ChatPropertyModeServiceImpl chatPropertyModeService,
                          BotUserController botUserController, DepartmentController departmentController,
                          UserRequestController userRequestController, NotificationController notificationController,
                          SuperAdminController superAdminController, MessageController messageController) {
        this.keyboardService = keyboardService;
        this.chatPropertyModeService = chatPropertyModeService;
        this.botUserController = botUserController;
        this.departmentController = departmentController;
        this.userRequestController = userRequestController;
        this.notificationController = notificationController;
        this.superAdminController = superAdminController;
        this.messageController = messageController;
    }

    @Override
    public List<BotApiMethod<?>> getHandlerUpdate(@NotNull Update update) {
        log.info("messageHandler get update = {}", update);
        Message message = update.getMessage();
        log.info("update has message = {}", message);
        if (message.hasText()) {
            log.info("message has text = {}", message.getText());
            switch (message.getText()) {
                case "/start" -> {
                    return botUserController.setStartProperties(message);
                }
                case "Зробити заявку" -> {
                    return departmentController.setDepartmentOfRequest(message);
                }
                case "Мої заявки" -> {
                    return userRequestController.getAllStateRequests(message);
                }
                case "Всі заявки" -> {
                    return userRequestController.getAdminAllStateRequests(message);
                }
                case "Мої не виконані заявки" -> {
                    return userRequestController.getFalseStateRequests(message);
                }
                case "Не виконані заявки" -> {
                    return userRequestController.getAdminFalseStateRequests(message);
                }
                case "Змінити меню" -> {
                    return keyboardService.setChangeMenu(message);
                }
                case "Повідомлення всім" -> {
                    return messageController.setMessageToAll(message);
                }
                case "❌ Відмовитись" -> {
                    return keyboardService.setReplyKeyboard(message.getChatId(), START_TEXT);
                }
                case "Останні оголошення" -> {
                    return notificationController.getNotifications(message);
                }
                default -> {
                    if (chatPropertyModeService.getCurrentBotState(message.getChatId()).equals(BotState.WAIT_MESSAGE_TO_ALL)) {
                        return messageController.sendMessageToAll(message);
                    }
                    if (message.getText().startsWith("*admin*")) return superAdminController.requestAdminRole(message);
                    if (message.getText().startsWith("*set*")) return superAdminController.handelRequestAdminRole(message);
                    if (chatPropertyModeService.getCurrentBotState(message.getChatId()).equals(BotState.WAIT_ADDRESS)) {
                        return userRequestController.setRequestAddress(message);
                    } else return userRequestController.createRequestMessageHandler(message);
                }
            }
        }
        if (message.hasContact()) return botUserController.setBotUserPhone(message);
        if (message.hasLocation()) return userRequestController.setRequestLocation(message);
        return Controller.getSimpleResponseToRequest(message, WRONG_ACTION_TEXT);
    }
}
