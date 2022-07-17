package ua.tarasov.hotline.controller.impl;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
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
import ua.tarasov.hotline.service.BotUserService;
import ua.tarasov.hotline.service.KeyboardService;
import ua.tarasov.hotline.service.UserRequestService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserRequestController implements Controller {
    final UserRequestService requestService;
    final BotUserService botUserService;
    final KeyboardService keyboardService;

    List<BotApiMethod<?>> answerMessages = new ArrayList<>();
    UserRequest userRequest = new UserRequest();

    public UserRequestController(UserRequestService requestService, BotUserService botUserService, KeyboardService keyboardService) {
        this.requestService = requestService;
        this.botUserService = botUserService;
        this.keyboardService = keyboardService;
    }

    public List<BotApiMethod<?>> getAllStateRequests(@NotNull Message message) {
        List<UserRequest> messages = requestService.findMessagesByBotUser(message.getChatId());
        chatPropertyModeService.setCurrentBotState(message.getChatId(), BotState.WAIT_BUTTON);
        return sendUserListOfMessages(message, messages);
    }

    private List<BotApiMethod<?>> sendUserListOfMessages(Message message, @NotNull List<UserRequest> messages) {
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

    public List<BotApiMethod<?>> getAdminAllStateRequests(@NotNull Message message) {
        chatPropertyModeService.setCurrentBotState(message.getChatId(), BotState.WAIT_BUTTON);
        if (botUserService.findById(message.getChatId()).isPresent()) {
            BotUser botUser = botUserService.findById(message.getChatId()).get();
            Set<Department> departments = botUser.getDepartments();
            List<UserRequest> messages = new ArrayList<>();
            departments.forEach(department -> messages.addAll(requestService.findAllByDepartment(department)));
            answerMessages = sendAdminListOfMessages(message, messages);
        }
        return answerMessages;
    }

    private List<BotApiMethod<?>> sendAdminListOfMessages(Message message, @NotNull List<UserRequest> messages) {
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

    public List<BotApiMethod<?>> getFalseStateRequests(@NotNull Message message) {
        chatPropertyModeService.setCurrentBotState(message.getChatId(), BotState.WAIT_BUTTON);
        List<UserRequest> messages = requestService.findMessagesByBotUserAndState(message.getChatId(), false);
        return sendUserListOfMessages(message, messages);
    }

    public List<BotApiMethod<?>> getAdminFalseStateRequests(@NotNull Message message) {
        chatPropertyModeService.setCurrentBotState(message.getChatId(), BotState.WAIT_BUTTON);
        if (botUserService.findById(message.getChatId()).isPresent()) {
            BotUser botUser = botUserService.findById(message.getChatId()).get();
            Set<Department> departments = botUser.getDepartments();
            List<UserRequest> messages = new ArrayList<>();
            departments.forEach(department -> messages.addAll
                    (requestService.findMessagesByDepartmentAndState(department, false)));
            answerMessages = sendAdminListOfMessages(message, messages);
        }
        return answerMessages;
    }
    public List<BotApiMethod<?>> createRequestMessageHandler(@NotNull Message message) {
        if (chatPropertyModeService.getCurrentBotState(message.getChatId()).equals(BotState.WAIT_MESSAGE)) {
            UserRequest userRequest = createNewUserRequest(message);
            List<BotUser> botUsers = botUserService.findAllByDepartment(userRequest.getDepartment());
            List<BotApiMethod<?>> answerMessages = new ArrayList<>();
            if (!botUsers.isEmpty()) {
                botUsers.forEach(botUser -> answerMessages.add(SendMessage.builder()
                        .chatId(String.valueOf(botUser.getId()))
                        .text(userRequest.getBodyOfMessage() +
                                "\n\n" + FALSE_ACTION_STATE_TEXT)
                        .build()));
            }
            answerMessages.addAll(Controller.getSimpleResponseToRequest(message,
                    "\uD83D\uDC4D\nДякуємо, Ваша заявка\nID " + userRequest.getMessageId() +
                            "\nвід " + userRequest.getDateTimeToString() + "\nприйнята"));
            chatPropertyModeService.setCurrentBotState(message.getChatId(), BotState.WAIT_BUTTON);
            return answerMessages;
        } else return Controller.getSimpleResponseToRequest(message, "Вибачте, але я бот і читати не вмію." +
                "\n<b>Виконайте, будьласка, коректну дію за допомогою кнопок</b>");
    }

    private UserRequest createNewUserRequest(@NotNull Message message) {
        userRequest.setId(0L);
        userRequest.setDepartment(chatPropertyModeService.getCurrentDepartment(message.getChatId()));
        userRequest.setChatId(message.getChatId());
        userRequest.setMessageId(message.getMessageId());
        userRequest.setDateTime(LocalDateTime.now(ZoneId.of("Europe/Kiev")));
        userRequest.setAddress(chatPropertyModeService.getCurrentRequestAddress(message.getChatId()));
        userRequest.setLocation(chatPropertyModeService.getCurrentLocation(message.getChatId()));
        chatPropertyModeService.setCurrentLocation(message.getChatId(), null);
        String isLocation = userRequest.getLocation() != null ? "Локація: +" : "Локація: --";
        userRequest.setBodyOfMessage(userRequest.getDepartment().toString().substring("1. ".length()) + "\nID " + userRequest.getMessageId() +
                "\nвід " + userRequest.getDateTimeToString() + "\n\n" + message.getText() +
                "\n\nадреса: " + userRequest.getAddress() + "\n" + isLocation);
        userRequest.setState(false);
        requestService.saveRequest(userRequest);
        return userRequest;
    }

    public List<BotApiMethod<?>> setRequestAddress(@NotNull Message message) {
        chatPropertyModeService.setCurrentRequestAddress(message.getChatId(), message.getText());
        chatPropertyModeService.setCurrentBotState(message.getChatId(), BotState.WAIT_MESSAGE);
        return Controller.getSimpleResponseToRequest(message, "Адресу додано до заявки" +
                "\nВведіть, будьласка, текст заявки");
    }

    public List<BotApiMethod<?>> setRequestLocation(@NotNull Message message) {
        if (chatPropertyModeService.getCurrentBotState(message.getChatId()).equals(BotState.WAIT_LOCATION)) {
            Location location = message.getLocation();
            chatPropertyModeService.setCurrentLocation(message.getChatId(), location);
            chatPropertyModeService.setCurrentBotState(message.getChatId(), BotState.WAIT_ADDRESS);
            return Controller.getSimpleResponseToRequest(message, "Локацію установлено" +
                    "\nВведіть, будьласка, адресу, за якою сталася проблема");
        } else return Controller.getSimpleResponseToRequest(message,
                "Вибачте, але локацію має сенс додавати тільки при створенні заявки.");
    }

    @NotNull @Unmodifiable
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
        } else return Controller.getSimpleResponseToRequest(callbackQuery.getMessage(), "Цю заявку було видалено раніше");
    }

    @NotNull @Unmodifiable
    public List<BotApiMethod<?>> setRefuseRequest(@NotNull CallbackQuery callbackQuery) {
        Integer messageId = jsonConverter.fromJson(callbackQuery.getData().substring("refuse_request".length()), Integer.class);
        userRequest = requestService.findByMessageId(messageId);
        if (userRequest != null) {
            long chatId = userRequest.getChatId();
            requestService.deleteUserRequest(userRequest);
            return List.of(SendMessage.builder()
                    .chatId(String.valueOf(chatId))
                    .replyToMessageId(messageId)
                    .text("Нажаль, ми вимушені відмовити Вам у виконанні заявки" +
                            "\nID " + messageId + "\nвід " + userRequest.getDateTimeToString() +
                            "\n\nВаша заявка не є в компетенції нашого департаменту," +
                            "\nабо її не можливо виконати з незалежних від нас причин")
                    .build());
        } else return Controller.getSimpleResponseToRequest(callbackQuery.getMessage(), "Цю заявку було видалено раніше");
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
        return Controller.getSimpleResponseToRequest(message, "Ви не можете отримати інформацію, пов'язану із цією заявкою," +
                " бо, на теперішній час її вже не існує");
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
        return Controller.getSimpleResponseToRequest(message, "Ви не можете отримати шнформацію, пов'язану із цією заявкою," +
                " бо, на теперішній час її вже не існує");
    }
}
