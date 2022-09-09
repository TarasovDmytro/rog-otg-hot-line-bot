package ua.tarasov.hotline.handlers.impl;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.tarasov.hotline.controller.Controller;
import ua.tarasov.hotline.controller.impl.*;
import ua.tarasov.hotline.entities.BotUser;
import ua.tarasov.hotline.handlers.RequestHandler;
import ua.tarasov.hotline.service.BotUserService;

import java.util.List;

@Component
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CallBackQueryHandler implements RequestHandler {
    final DepartmentController departmentController;
    final UserRequestController userRequestController;
    final MessageController messageController;
    final BotUserController botUserController;
    final SuperAdminController superAdminController;
    final BotUserService userService;

    public CallBackQueryHandler(DepartmentController departmentController, UserRequestController userRequestController,
                                MessageController messageController, BotUserController botUserController,
                                SuperAdminController superAdminController, BotUserService userService) {
        this.departmentController = departmentController;
        this.userRequestController = userRequestController;
        this.messageController = messageController;
        this.botUserController = botUserController;
        this.superAdminController = superAdminController;
        this.userService = userService;
    }

    @Override
    public List<BotApiMethod<?>> getHandlerUpdate(@NotNull Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long userId = callbackQuery.getMessage().getChatId();
        BotUser user = new BotUser();
        if (userService.findById(userId).isPresent()) {
            user = userService.findById(userId).get();
        }
        if (user.getWarningCount() < 3) {
            if (callbackQuery.getData().startsWith("department")) {
                return departmentController.setDepartment(callbackQuery);
            }
            if (callbackQuery.getData().startsWith("state_request")) {
                return userRequestController.setStateRequest(callbackQuery);
            }
            if (callbackQuery.getData().startsWith("refuse_request")) {
                return userRequestController.setRefuseRequest(callbackQuery);
            }
            if (callbackQuery.getData().startsWith("contact")) {
                return userRequestController.getContactOfRequest(callbackQuery);
            }
            if (callbackQuery.getData().startsWith("yes-location")) {
                return messageController.setLocationMessage(callbackQuery);
            }
            if (callbackQuery.getData().startsWith("yes-department")) {
                return botUserController.setBotUserDepartment(callbackQuery);
            }
            if (callbackQuery.getData().startsWith("no-location")) {
                return messageController.setRequestAddressMessage(callbackQuery.getMessage());
            }
            if (callbackQuery.getData().startsWith("no-department")) {
                return messageController.setRefuseRequestMessage(callbackQuery);
            }

            if (callbackQuery.getData().startsWith("location")) {
                return userRequestController.getLocationOfRequest(callbackQuery);
            }
            if (callbackQuery.getData().startsWith("refuse")) {
                return messageController.refuseSetLocationOfRequestMessage(callbackQuery);
            }
            if (callbackQuery.getData().startsWith("complaint")) {
                return superAdminController.getComplaint(callbackQuery);
            }
            return Controller.getSimpleResponseToRequest(callbackQuery.getMessage(), WRONG_ACTION_TEXT);
        } else return List.of(SendMessage.builder()
                .chatId(String.valueOf(userId))
                .text("Вибачте, але Ви заблоковані за некоректне використання сервісу")
                .build());
    }
}
