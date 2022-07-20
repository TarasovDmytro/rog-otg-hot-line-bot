package ua.tarasov.hotline.handlers.impl;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.tarasov.hotline.controller.Controller;
import ua.tarasov.hotline.controller.impl.BotUserController;
import ua.tarasov.hotline.controller.impl.DepartmentController;
import ua.tarasov.hotline.controller.impl.MessageController;
import ua.tarasov.hotline.controller.impl.UserRequestController;
import ua.tarasov.hotline.handlers.RequestHandler;
import ua.tarasov.hotline.models.StateOfRequest;
import ua.tarasov.hotline.service.ChatPropertyModeService;

import java.util.List;

@Component
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CallBackQueryHandler implements RequestHandler {
    final DepartmentController departmentController;
    final UserRequestController userRequestController;
    final MessageController messageController;
    final BotUserController botUserController;
    final ChatPropertyModeService chatPropertyModeService;

    public CallBackQueryHandler(DepartmentController departmentController, UserRequestController userRequestController,
                                MessageController messageController, BotUserController botUserController,
                                @Qualifier("chatPropertyModeServiceImpl") ChatPropertyModeService chatPropertyModeService) {
        this.departmentController = departmentController;
        this.userRequestController = userRequestController;
        this.messageController = messageController;
        this.botUserController = botUserController;
        this.chatPropertyModeService = chatPropertyModeService;
    }

    @Override
    public List<BotApiMethod<?>> getHandlerUpdate(@NotNull Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (chatPropertyModeService.getStateOfRequest(update.getMessage().getChatId()).equals(StateOfRequest.REQUEST_CREATED)) {
            if (callbackQuery.getData().startsWith("department")) {
                return departmentController.getButtonDepartmentHandler(callbackQuery);
            }
            if (callbackQuery.getData().startsWith("message_id")) {
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
                return messageController.setRequestAddressMessage(callbackQuery);
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
        } else return userRequestController.createRequest(update);
        return Controller.getSimpleResponseToRequest(callbackQuery.getMessage(), WRONG_ACTION_TEXT);
    }
}
