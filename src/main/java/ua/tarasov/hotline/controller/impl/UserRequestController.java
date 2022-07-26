package ua.tarasov.hotline.controller.impl;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Location;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ua.tarasov.hotline.controller.Controller;
import ua.tarasov.hotline.entities.BotUser;
import ua.tarasov.hotline.entities.UserRequest;
import ua.tarasov.hotline.models.BotState;
import ua.tarasov.hotline.models.Department;
import ua.tarasov.hotline.models.StateOfRequest;
import ua.tarasov.hotline.service.BotUserService;
import ua.tarasov.hotline.service.KeyboardService;
import ua.tarasov.hotline.service.UserRequestService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Component
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserRequestController implements Controller {
    final UserRequestService requestService;
    final BotUserService botUserService;
    final KeyboardService keyboardService;
    final DepartmentController departmentController;
    final MessageController messageController;

    List<BotApiMethod<?>> answerMessages = new ArrayList<>();
    UserRequest userRequest = new UserRequest();

    public UserRequestController(UserRequestService requestService, BotUserService botUserService, KeyboardService keyboardService, DepartmentController departmentController, MessageController messageController) {
        this.requestService = requestService;
        this.botUserService = botUserService;
        this.keyboardService = keyboardService;
        this.departmentController = departmentController;
        this.messageController = messageController;
    }

    public List<BotApiMethod<?>> createRequest(Message message) {
        Long chatId = message.getChatId();
        if (!chatPropertyModeService.getStateOfRequest(chatId).equals(StateOfRequest.CREATE_REQUEST)) {
            if (message.hasLocation()) return setRequestLocation(message);
            if (message.hasText()) {
                switch (message.getText()) {
                    case "Далі" -> switchStateOfRequest(chatId);
                    case "Скасувати заявку" -> {
                        chatPropertyModeService.setCurrentLocation(message.getChatId(), null);
                        chatPropertyModeService.setCurrentBotState(chatId, BotState.WAIT_BUTTON);
                        chatPropertyModeService.setCurrentStateOfRequest(chatId, StateOfRequest.REQUEST_CREATED);
                        return keyboardService.setReplyKeyboard(chatId, "Заявку скасовано");
                    }
                    case "Відправити заявку" ->
                            chatPropertyModeService.setCurrentStateOfRequest(chatId, StateOfRequest.CREATE_REQUEST);
                }
            }
            switch (chatPropertyModeService.getStateOfRequest(message.getChatId())) {
                case SET_DEPARTMENT -> {
                    List<BotApiMethod<?>> methods = new ArrayList<>();
                    methods.addAll(keyboardService.setRequestReplyKeyboard(message.getChatId(), "Далі", "Почнемо"));
                    methods.addAll(departmentController.getMenuOfDepartments(message));
                    methods.addAll(Controller.getSimpleResponseToRequest(message, "Ви можете змінити ці данні," +
                            " або натисніть кнопку 'Далі'"));
                    return methods;
                }
                case SET_LOCATION -> {
                    log.info("case SET_LOCATION = {}", chatPropertyModeService.getStateOfRequest(chatId));
                    return getLocationMenu(message);
                }
                case WAIT_ADDRESS -> {
                    log.info("case WAIT_ADDRESS = {}", chatPropertyModeService.getStateOfRequest(chatId));
                    return messageController.setRequestAddressMessage(message);
                }
                case SET_ADDRESS -> {
                    log.info("case SET_ADDRESS = {}", chatPropertyModeService.getStateOfRequest(chatId));
                    chatPropertyModeService.setCurrentBotState(chatId, BotState.WAIT_ADDRESS);
                    return setRequestAddress(message);
                }
                case WAIT_TEXT -> {
                    chatPropertyModeService.setCurrentStateOfRequest(chatId, StateOfRequest.SET_TEXT);
                    return Controller.getSimpleResponseToRequest(message, "Введіть, будьласка, докладний опис існуючої проблеми");
                }
                case SET_TEXT -> {
                    log.info("case SET_TEXT = {}", chatPropertyModeService.getStateOfRequest(chatId));
                    return createNewUserRequest(message);
                }
            }
        }
        return createRequestMessageHandler(message);
    }

    public void switchStateOfRequest(Long chatId) {
        switch (chatPropertyModeService.getStateOfRequest(chatId)) {
            case SET_DEPARTMENT ->
                    chatPropertyModeService.setCurrentStateOfRequest(chatId, StateOfRequest.SET_LOCATION);
            case SET_LOCATION -> {
                chatPropertyModeService.setCurrentBotState(chatId, BotState.WAIT_MESSAGE);
                chatPropertyModeService.setCurrentStateOfRequest(chatId, StateOfRequest.WAIT_ADDRESS);
            }
            case SET_ADDRESS -> chatPropertyModeService.setCurrentStateOfRequest(chatId, StateOfRequest.WAIT_TEXT);
            case SET_TEXT -> chatPropertyModeService.setCurrentStateOfRequest(chatId, StateOfRequest.REQUEST_CREATED);
        }
    }

    public List<BotApiMethod<?>> getLocationMenu(Message message) {
        return List.of(
                SendMessage.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .text("Чи бажаєте Ви додати геолокацію?")
                        .replyMarkup(InlineKeyboardMarkup.builder()
                                .keyboard(keyboardService.getAgreeButtons("location"))
                                .build())
                        .build());
    }

    public List<BotApiMethod<?>> getAllStatesRequestsOfUser(@NotNull Message message) {
        List<UserRequest> messages = requestService.findMessagesByBotUser(message.getChatId());
        chatPropertyModeService.setCurrentBotState(message.getChatId(), BotState.WAIT_BUTTON);
        return sendListOfMessagesToUser(message, messages);
    }

    private List<BotApiMethod<?>> sendListOfMessagesToUser(Message message, @NotNull List<UserRequest> messages) {
        List<BotApiMethod<?>> answerMessages = new ArrayList<>();
        if (!messages.isEmpty()) {
            messages.sort(Comparator.comparing(UserRequest::getId));
            messages.forEach(currentMessage -> {
                stateText.set(currentMessage.isState() ? TRUE_ACTION_STATE_TEXT : FALSE_ACTION_STATE_TEXT);
                answerMessages.add(SendMessage.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .text(currentMessage.getBodyOfMessage() + "\n\n" + stateText)
                        .build());
            });
        } else answerMessages.add(SendMessage.builder()
                .chatId(String.valueOf(message.getChatId()))
                .text("Наразі не існує таких заявок")
                .build());
        return answerMessages;
    }

    public List<BotApiMethod<?>> getAllStatesRequestsOfAdmin(@NotNull Message message) {
        chatPropertyModeService.setCurrentBotState(message.getChatId(), BotState.WAIT_BUTTON);
        if (botUserService.findById(message.getChatId()).isPresent()) {
            BotUser botUser = botUserService.findById(message.getChatId()).get();
            Set<Department> departments = botUser.getDepartments();
            List<UserRequest> messages = new ArrayList<>();
            departments.forEach(department -> messages.addAll(requestService.findAllByDepartment(department)));
            answerMessages = sendListOfMessagesToAdmin(message, messages);
        }
        return answerMessages;
    }

    private List<BotApiMethod<?>> sendListOfMessagesToAdmin(Message message, @NotNull List<UserRequest> messages) {
        if (!messages.isEmpty()) {
            List<BotApiMethod<?>> answerMessages = new ArrayList<>();
            messages.sort(Comparator.comparing(UserRequest::getId));
            messages.forEach(currentMessage -> {
                stateText.set(currentMessage.isState() ? TRUE_ACTION_STATE_TEXT : FALSE_ACTION_STATE_TEXT);
                List<List<InlineKeyboardButton>> buttons =
                        keyboardService.getStateRequestButton(currentMessage.getMessageId(), stateText.get());
                answerMessages.add(SendMessage.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .text(currentMessage.getBodyOfMessage())
                        .replyMarkup(InlineKeyboardMarkup.builder()
                                .keyboard(buttons)
                                .build())
                        .build());
            });
            return answerMessages;
        }
        return Controller.getSimpleResponseToRequest(message, "Наразі не існує таких заявок");
    }

    public List<BotApiMethod<?>> getFalseStateRequestsOfUser(@NotNull Message message) {
        chatPropertyModeService.setCurrentBotState(message.getChatId(), BotState.WAIT_BUTTON);
        List<UserRequest> messages = requestService.findMessagesByBotUserAndState(message.getChatId(), false);
        return sendListOfMessagesToUser(message, messages);
    }

    public List<BotApiMethod<?>> getFalseStateRequestsOfAdmin(@NotNull Message message) {
        chatPropertyModeService.setCurrentBotState(message.getChatId(), BotState.WAIT_BUTTON);
        if (botUserService.findById(message.getChatId()).isPresent()) {
            BotUser botUser = botUserService.findById(message.getChatId()).get();
            Set<Department> departments = botUser.getDepartments();
            List<UserRequest> messages = new ArrayList<>();
            departments.forEach(department -> messages.addAll
                    (requestService.findMessagesByDepartmentAndState(department, false)));
            answerMessages = sendListOfMessagesToAdmin(message, messages);
        }
        return answerMessages;
    }

    public List<BotApiMethod<?>> createRequestMessageHandler(@NotNull Message message) {
        if (chatPropertyModeService.getCurrentBotState(message.getChatId()).equals(BotState.WAIT_MESSAGE)) {
            requestService.saveRequest(userRequest);
            chatPropertyModeService.setCurrentLocation(message.getChatId(), null);
            List<BotUser> botUsers = botUserService.findAllByDepartment(userRequest.getDepartment());
            List<BotApiMethod<?>> answerMessages = new ArrayList<>();
            if (!botUsers.isEmpty()) {
                botUsers.forEach(botUser -> answerMessages.add(SendMessage.builder()
                        .chatId(String.valueOf(botUser.getId()))
                        .text(userRequest.getBodyOfMessage() +
                                "\n\n" + FALSE_ACTION_STATE_TEXT)
                        .build()));
            }
            chatPropertyModeService.setCurrentBotState(message.getChatId(), BotState.WAIT_BUTTON);
            chatPropertyModeService.setCurrentStateOfRequest(message.getChatId(), StateOfRequest.REQUEST_CREATED);
            answerMessages.addAll(keyboardService.setReplyKeyboard(message.getChatId(),
                    "\uD83D\uDC4D\nДякуємо, Ваша заявка\nID " + userRequest.getMessageId() +
                            "\nвід " + userRequest.getDateTimeToString() + "\nприйнята"));
            return answerMessages;
        } else return Controller.getSimpleResponseToRequest(message, "Вибачте, але я бот і читати не вмію." +
                "\n<b>Виконайте, будьласка, коректну дію за допомогою кнопок</b>");
    }

    private List<BotApiMethod<?>> createNewUserRequest(@NotNull Message message) {
        userRequest.setId(0L);
        userRequest.setDepartment(chatPropertyModeService.getCurrentDepartment(message.getChatId()));
        userRequest.setChatId(message.getChatId());
        userRequest.setMessageId(message.getMessageId());
        userRequest.setDateTime(LocalDateTime.now(ZoneId.of("Europe/Kiev")));
        userRequest.setAddress(chatPropertyModeService.getCurrentRequestAddress(message.getChatId()));
        userRequest.setLocation(chatPropertyModeService.getCurrentLocation(message.getChatId()));
        String isLocation = userRequest.getLocation() != null ? "Локація: +" : "Локація: --";
        userRequest.setBodyOfMessage(userRequest.getDepartment().toString().substring("1. ".length()) + "\nID "
                + userRequest.getMessageId() + "\nвід " + userRequest.getDateTimeToString() + "\n\n" + message.getText() +
                "\n\nадреса: " + userRequest.getAddress() + "\n" + isLocation);
        userRequest.setState(false);
        List<BotApiMethod<?>> methods = new ArrayList<>();
        methods.addAll(Controller.getSimpleResponseToRequest(message, "Опис проблеми додано до заявки,\nВи можете" +
                " його змінити,або натисніть кнопку 'Відправити заявку'"));
        methods.addAll(keyboardService.setRequestReplyKeyboard(message.getChatId(), "Відправити заявку",
                "Ваша заявка\n" + userRequest.getBodyOfMessage()));
        return methods;
    }

    public List<BotApiMethod<?>> setRequestAddress(@NotNull Message message) {
        chatPropertyModeService.setCurrentRequestAddress(message.getChatId(), message.getText());
        chatPropertyModeService.setCurrentBotState(message.getChatId(), BotState.WAIT_MESSAGE);
        List<BotApiMethod<?>> methods = new ArrayList<>();
        methods.addAll(keyboardService.setRequestReplyKeyboard(message.getChatId(), "Далі", "Адресу додано до заявки"));
        methods.addAll(Controller.getSimpleResponseToRequest(message, "Ви можете змінити ці данні," +
                " або натисніть кнопку 'Далі'"));
        return methods;
    }

    public List<BotApiMethod<?>> setRequestLocation(@NotNull Message message) {
        if (chatPropertyModeService.getCurrentBotState(message.getChatId()).equals(BotState.WAIT_LOCATION)) {
            Location location = message.getLocation();
            chatPropertyModeService.setCurrentLocation(message.getChatId(), location);
            List<BotApiMethod<?>> methods = new ArrayList<>();
            methods.addAll(keyboardService.setRequestReplyKeyboard(message.getChatId(), "Далі", "Локацію додано до заявки"));
            methods.addAll(Controller.getSimpleResponseToRequest(message, "Ви можете змінити ці данні," +
                    " або натисніть кнопку 'Далі'"));
            return methods;
        } else return createRequest(message);
    }

    @NotNull
    @Unmodifiable
    public List<BotApiMethod<?>> setStateRequest(@NotNull CallbackQuery callbackQuery) {
        Message message = callbackQuery.getMessage();
        Integer messageId = jsonConverter.fromJson(callbackQuery.getData().substring("message_id".length()), Integer.class);
        userRequest = requestService.findByMessageId(messageId);
        if (userRequest != null) {
            userRequest.setState(!userRequest.isState());
            requestService.saveRequest(userRequest);
            stateText.set(userRequest.isState() ? TRUE_ACTION_STATE_TEXT : FALSE_ACTION_STATE_TEXT);
            return List.of(keyboardService.getCorrectReplyMarkup(message,
                            keyboardService.getStateRequestButton(messageId, stateText.get())),
                    SendMessage.builder()
                            .chatId(userRequest.getChatId().toString())
                            .replyToMessageId(messageId)
                            .text("Ваша заявка\nID " + messageId + "\nвід " + userRequest.getDateTimeToString() + "\n" + stateText)
                            .build());
        } else
            return Controller.getSimpleResponseToRequest(callbackQuery.getMessage(), "Цю заявку було видалено раніше");
    }

    @NotNull
    @Unmodifiable
    public List<BotApiMethod<?>> setRefuseRequest(@NotNull CallbackQuery callbackQuery) {
        Integer messageId = jsonConverter.fromJson(callbackQuery.getData().substring("refuse_request".length()), Integer.class);
        userRequest = requestService.findByMessageId(messageId);
        if (userRequest != null) {
            long chatId = userRequest.getChatId();
            if (!userRequest.isState()) {
                requestService.deleteUserRequest(userRequest);
                return List.of(SendMessage.builder()
                        .chatId(String.valueOf(chatId))
                        .replyToMessageId(messageId)
                        .text("Нажаль, ми вимушені відмовити Вам у виконанні заявки" +
                                "\nID " + messageId + "\nвід " + userRequest.getDateTimeToString() +
                                "\n\nВаша заявка не є в компетенції нашого департаменту," +
                                "\nабо її не можливо виконати з незалежних від нас причин")
                        .build());
            } else
                return Controller.getSimpleResponseToRequest(callbackQuery.getMessage(), "Ця заявка має статус" +
                        " 'Виконана' і не може бути видалена примусово");
        } else
            return Controller.getSimpleResponseToRequest(callbackQuery.getMessage(), "Цю заявку було" +
                    " видалено раніше");

    }

    public List<BotApiMethod<?>> getContactOfRequest(@NotNull CallbackQuery callbackQuery) {
        Message message = callbackQuery.getMessage();
        Integer messageId = jsonConverter.fromJson(callbackQuery.getData().substring("contact".length()), Integer.class);
        userRequest = requestService.findByMessageId(messageId);
        if (userRequest != null) {
            Long botUserId = userRequest.getChatId();
            if (botUserService.findById(botUserId).isPresent()) {
                BotUser botUser = botUserService.findById(botUserId).get();
                String phone = botUser.getPhone();
                if (phone != null) {
                    if (!phone.startsWith("+")) {
                        phone = "+" + phone;
                    }
                    String messageText = userRequest.getBodyOfMessage() +
                            "\n\nІз користувачем можна зв'язатись за телефоном:\n"
                            + phone;
                    return Controller.getSimpleResponseToRequest(message, messageText);
                } else {
                    return Collections.singletonList(AnswerCallbackQuery.builder()
                            .callbackQueryId(callbackQuery.getId())
                            .text("Користувач відмовився надати свій номер телефону")
                            .showAlert(true)
                            .build());
                }
            }
        }
        return Controller.getSimpleResponseToRequest(message, "Ви не можете отримати інформацію, пов'язану" +
                " із цією заявкою, бо її вже не існує");
    }

    public List<BotApiMethod<?>> getLocationOfRequest(@NotNull CallbackQuery callbackQuery) {
        Integer messageId = jsonConverter.fromJson(callbackQuery
                .getData().substring("location".length()), Integer.class);
        Message message = callbackQuery.getMessage();
        userRequest = requestService.findByMessageId(messageId);
        if (userRequest != null) {
            Location messageLocation = userRequest.getLocation();
            if (messageLocation != null) {
                return Collections.singletonList(SendLocation.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .replyToMessageId(message.getMessageId())
                        .heading(messageLocation.getHeading())
                        .horizontalAccuracy(messageLocation.getHorizontalAccuracy())
                        .latitude(messageLocation.getLatitude())
                        .livePeriod(messageLocation.getLivePeriod())
                        .longitude(messageLocation.getLongitude())
                        .proximityAlertRadius(messageLocation.getProximityAlertRadius())
                        .build());
            } else
                return List.of(SendMessage.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .replyToMessageId(message.getMessageId())
                        .text("Вибачте, але до заявки ID:" + messageId + " локацію не додавали")
                        .build());
        }
        return Controller.getSimpleResponseToRequest(message, "Ви не можете отримати шнформацію, пов'язану" +
                " із цією заявкою, бо її вже не існує");
    }
}
