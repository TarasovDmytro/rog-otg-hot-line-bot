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
import ua.tarasov.hotline.service.impl.BotUserServiceImpl;
import ua.tarasov.hotline.service.impl.ChatPropertyModeServiceImpl;
import ua.tarasov.hotline.service.impl.KeyboardServiceImpl;
import ua.tarasov.hotline.service.impl.UserRequestServiceImpl;

import java.util.List;

@Component
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CallBackQueryHandler implements RequestHandler {
//    final UserRequestServiceImpl requestService;
//    final BotUserService botUserService;
//    final KeyboardService keyboardService;
//    final ChatPropertyModeService chatPropertyModeService;

    final DepartmentController departmentController;
    final UserRequestController userRequestController;
    final MessageController messageController;
    final BotUserController botUserController;

//    UserRequest userRequest = new UserRequest();
//    BotUser botUser = new BotUser();

    public CallBackQueryHandler(UserRequestServiceImpl requestService, BotUserServiceImpl botUserService,
                                KeyboardServiceImpl keyboardService,
                                @Qualifier("getChatProperties") ChatPropertyModeServiceImpl chatPropertyModeService, DepartmentController departmentController, UserRequestController userRequestController, MessageController messageController, BotUserController botUserController) {
//        this.requestService = requestService;
//        this.botUserService = botUserService;
//        this.keyboardService = keyboardService;
//        this.chatPropertyModeService = chatPropertyModeService;
        this.departmentController = departmentController;
        this.userRequestController = userRequestController;
        this.messageController = messageController;
        this.botUserController = botUserController;
    }

    @Override
    public List<BotApiMethod<?>> getHandlerUpdate(@NotNull Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
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
        return Controller.getSimpleResponseToRequest(callbackQuery.getMessage(), WRONG_ACTION_TEXT);
    }

//    private @NotNull @Unmodifiable List<BotApiMethod<?>> setRefuseRequestMessage(@NotNull CallbackQuery callbackQuery) {
//        BotUser superAdmin = botUserService.findByRole(Role.SUPER_ADMIN);
//        return (List.of(SendMessage.builder()
//                        .chatId(String.valueOf(callbackQuery.getMessage().getChatId()))
//                        .text("Вибачте, Вам відмовлено в зміні прав доступу")
//                        .build(),
//                SendMessage.builder()
//                        .chatId(String.valueOf(superAdmin.getId()))
//                        .text("Відмовлено")
//                        .build()));
//    }

//    private @NotNull @Unmodifiable List<BotApiMethod<?>> setBotUserDepartment(@NotNull CallbackQuery callbackQuery) {
//        String[] depText = jsonConverter.fromJson(callbackQuery.getData().substring("yes-department".length()), String[].class);
//        if (botUserService.findById(Long.parseLong(depText[0])).isPresent()) {
//            botUser = botUserService.findById(Long.parseLong(depText[0])).get();
//        }
//        Set<Department> departments = new HashSet<>();
//        List<String> departmentsNumber = Arrays.stream(depText).skip(1).toList();
//        for (String s : departmentsNumber) {
//            Department department = Department.values()[Integer.parseInt(s) - 1];
//            departments.add(department);
//        }
//        botUser.setDepartments(departments);
//        if (!botUser.getRole().equals(Role.SUPER_ADMIN)) botUser.setRole(Role.ADMIN);
//        botUserService.saveBotUser(this.botUser);
//        BotUser superAdmin = botUserService.findByRole(Role.SUPER_ADMIN);
//        StringBuilder builder = new StringBuilder("встановлені для департаментів: ");
//        botUser.getDepartments().forEach(department -> builder.append("\n").append(department));
//        return List.of(SendMessage.builder()
//                        .chatId(botUser.getId().toString())
//                        .text("Ваші права доступу адміністратора " + builder)
//                        .replyMarkup(ReplyKeyboardMarkup.builder()
//                                .keyboard(keyboardService.getAdminReplyButtons())
//                                .resizeKeyboard(true)
//                                .oneTimeKeyboard(false)
//                                .build())
//                        .build(),
//                SendMessage.builder()
//                        .chatId(String.valueOf(superAdmin.getId()))
//                        .text("Права доступу адміністратора " + botUser.getFullName() + " " + builder)
//                        .build());
//    }

//    private List<BotApiMethod<?>> refuseSetLocationOfRequest(CallbackQuery callbackQuery) {
//        return messageController.setRequestAddressMessage(callbackQuery);
//    }

//    private List<BotApiMethod<?>> getLocationOfRequest(@NotNull CallbackQuery callbackQuery) {
//        Integer messageId = jsonConverter.fromJson(callbackQuery
//                .getData().substring("location".length()), Integer.class);
//        Message message = callbackQuery.getMessage();
//        userRequest = requestService.findByMessageId(messageId);
//        if (userRequest != null) {
//            Location messageLocation = userRequest.getLocation();
//            if (messageLocation != null) {
//                return Collections.singletonList(SendLocation.builder()
//                        .chatId(String.valueOf(message.getChatId()))
//                        .replyToMessageId(message.getMessageId())
//                        .heading(messageLocation.getHeading())
//                        .horizontalAccuracy(messageLocation.getHorizontalAccuracy())
//                        .latitude(messageLocation.getLatitude())
//                        .livePeriod(messageLocation.getLivePeriod())
//                        .longitude(messageLocation.getLongitude())
//                        .proximityAlertRadius(messageLocation.getProximityAlertRadius())
//                        .build());
//            } else
//                return List.of(SendMessage.builder()
//                        .chatId(String.valueOf(message.getChatId()))
//                        .replyToMessageId(message.getMessageId())
//                        .text("Вибачте, але до заявки ID:" + messageId + " локацію не додавали")
//                        .build());
//        }
//        return Controller.getSimpleResponseToRequest(message, "Ви не можете отримати шнформацію, пов'язану із цією заявкою," +
//                                                   " бо, на теперішній час її вже не існує");
//    }

//    private @NotNull @Unmodifiable List<BotApiMethod<?>> getButtonDepartmentHandler(CallbackQuery callbackQuery) {
//        log.info("get button department");
//        chatPropertyModeService.setCurrentBotState(callbackQuery.getMessage().getChatId(), BotState.WAIT_BUTTON);
//        String textMessage = "Департамент обрано.\nЧи бажаєте Ви додати до заявки геолокацію?";
//        return buttonDepartmentHandler(callbackQuery, textMessage);
//    }
//
//    private @NotNull @Unmodifiable List<BotApiMethod<?>> buttonDepartmentHandler(@NotNull CallbackQuery callbackQuery, String textMessage) {
//        log.info("button department handler");
//        Message message = callbackQuery.getMessage();
//        Department department = jsonConverter.fromJson(callbackQuery
//                .getData().substring("department".length()), Department.class);
//        log.info("Set department: " + department);
//        chatPropertyModeService.setCurrentDepartment(message.getChatId(), department);
//        log.info("Current department: " + chatPropertyModeService.getCurrentDepartment(message.getChatId()));
//        return List.of(
//                keyboardService.getCorrectReplyMarkup(message, keyboardService.getDepartmentInlineButtons(department)),
//                SendMessage.builder()
//                        .chatId(String.valueOf(message.getChatId()))
//                        .text(textMessage)
//                        .replyMarkup(InlineKeyboardMarkup.builder()
//                                .keyboard(keyboardService.getAgreeButtons("location"))
//                                .build())
//                        .build()
//        );
//    }

//    private @NotNull @Unmodifiable List<BotApiMethod<?>> setStateRequest(@NotNull CallbackQuery callbackQuery) {
//        Message message = callbackQuery.getMessage();
//        Integer messageId = jsonConverter.fromJson(callbackQuery.getData().substring("message_id".length()), Integer.class);
//        userRequest = requestService.findByMessageId(messageId);
//        if (userRequest != null) {
//            userRequest.setState(!userRequest.isState());
//            requestService.saveRequest(userRequest);
//            stateText.set(userRequest.isState() ? TRUE_ACTION_STATE_TEXT : FALSE_ACTION_STATE_TEXT);
//            return List.of(keyboardService.getCorrectReplyMarkup(message,
//                            keyboardService.getStateRequestButton(messageId, stateText.get())),
//                    SendMessage.builder()
//                            .chatId(userRequest.getChatId().toString())
//                            .replyToMessageId(messageId)
//                            .text("Ваша заявка\nID " + messageId + "\nвід " + userRequest.getDateTimeToString() + "\n" + stateText)
//                            .build());
//        } else return getSimpleResponseToRequest(callbackQuery.getMessage(), "Цю заявку було видалено раніше");
//    }

//    private @NotNull @Unmodifiable List<BotApiMethod<?>> setRefuseRequest(@NotNull CallbackQuery callbackQuery) {
//        Integer messageId = jsonConverter.fromJson(callbackQuery.getData().substring("refuse_request".length()), Integer.class);
//        userRequest = requestService.findByMessageId(messageId);
//        if (userRequest != null) {
//            long chatId = userRequest.getChatId();
//            requestService.deleteUserRequest(userRequest);
//            return List.of(SendMessage.builder()
//                    .chatId(String.valueOf(chatId))
//                    .replyToMessageId(messageId)
//                    .text("Нажаль, ми вимушені відмовити Вам у виконанні заявки" +
//                          "\nID " + messageId + "\nвід " + userRequest.getDateTimeToString() +
//                          "\n\nВаша заявка не є в компетенції нашого департаменту," +
//                          "\nабо її не можливо виконати з незалежних від нас причин")
//                    .build());
//        } else return Controller.getSimpleResponseToRequest(callbackQuery.getMessage(), "Цю заявку було видалено раніше");
//    }

//    private List<BotApiMethod<?>> getContactOfRequest(@NotNull CallbackQuery callbackQuery) {
//        Message message = callbackQuery.getMessage();
//        Integer messageId = jsonConverter.fromJson(callbackQuery.getData().substring("contact".length()), Integer.class);
//        userRequest = requestService.findByMessageId(messageId);
//        if (userRequest != null) {
//            Long botUserId = userRequest.getChatId();
//            if (botUserService.findById(botUserId).isPresent()) {
//                botUser = botUserService.findById(botUserId).get();
//                String phone = botUser.getPhone();
//                if (phone != null) {
//                    if (!phone.startsWith("+")) {
//                        phone = "+" + phone;
//                    }
//                    String messageText = userRequest.getBodyOfMessage() +
//                                         "\n\nІз користувачем можна зв'язатись за телефоном:\n"
//                                         + phone;
//                    return Controller.getSimpleResponseToRequest(message, messageText);
//                } else {
//                    return Collections.singletonList(AnswerCallbackQuery.builder()
//                            .callbackQueryId(callbackQuery.getId())
//                            .text("Користувач відмовився надати свій номер телефону")
//                            .showAlert(true)
//                            .build());
//                }
//            }
//        }
//        return getSimpleResponseToRequest(message, "Ви не можете отримати інформацію, пов'язану із цією заявкою," +
//                                                   " бо, на теперішній час її вже не існує");
//    }

//    private @NotNull @Unmodifiable List<BotApiMethod<?>> setLocationMessage(@NotNull CallbackQuery callbackQuery) {
//        chatPropertyModeService.setCurrentBotState(callbackQuery.getMessage().getChatId(), BotState.WAIT_LOCATION);
//        return Collections.singletonList(SendMessage.builder()
//                .chatId(callbackQuery.getMessage().getChatId().toString())
//                .text("""
//                        Дякую, відправте, будьласка, Вашу поточну геолокацію.
//                        Це можна зробити, натиснув на позначку 'скрепки' поруч
//                        із полем для вводу текста.\s
//                        Увага! Телеграм підтримує цю послугу тільки у версії для
//                        смартфонів, якщо Ви використовуєте інший пристрій, або
//                        передумали - натиснить кнопку 'відмовитись'""")
//                .replyMarkup(InlineKeyboardMarkup.builder()
//                        .keyboard(keyboardService.getRefuseButton(callbackQuery.getMessage()))
//                        .build())
//                .build());
//    }

//    private List<BotApiMethod<?>> setRequestAddressMessage(@NotNull CallbackQuery callbackQuery) {
//        chatPropertyModeService.setCurrentBotState(callbackQuery.getMessage().getChatId(), BotState.WAIT_ADDRESS);
//        return Controller.getSimpleResponseToRequest(callbackQuery.getMessage(), "Добре. Введіть, будьласка, адресу," +
//                                                                      " за якою сталася проблема");
//    }
}
