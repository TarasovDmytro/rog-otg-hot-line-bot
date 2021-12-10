package ua.tarasov.hotline.handlers;

import com.google.gson.Gson;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Location;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ua.tarasov.hotline.models.model.BotState;
import ua.tarasov.hotline.models.model.ResponseContext;
import ua.tarasov.hotline.models.entities.BotUser;
import ua.tarasov.hotline.models.model.Departments;
import ua.tarasov.hotline.models.entities.UserRequest;
import ua.tarasov.hotline.models.model.Role;
import ua.tarasov.hotline.service.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class CallBackQueryHandler implements RequestHandler {
    private final UserRequestService requestService;
    private final BotUserService botUserService;
    private final KeyboardService keyboardService;
    private final ChatPropertyModeService chatPropertyModeService = ChatPropertyModeService.getChatProperties();

    private final String TRUE_ACTION_STATE_TEXT = "✅  Виконана";
    private final String FALSE_ACTION_STATE_TEXT = "⭕️ На виконанні";
    private final AtomicReference<String> stateText = new AtomicReference<>("null");
    private final Gson jsonConverter = new Gson();

    private UserRequest userRequest = new UserRequest();
    private BotUser botUser = new BotUser();

    public CallBackQueryHandler(UserRequestService requestService, BotUserService botUserService, KeyboardService keyboardService) {
        this.requestService = requestService;
        this.botUserService = botUserService;
        this.keyboardService = keyboardService;
    }

    @Override
    public ResponseContext getResponseContext(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery.getData().startsWith("department")) {
            return getButtonDepartmentHandler(callbackQuery);
        }
        if (callbackQuery.getData().startsWith("message_id")) {
            return setStateRequest(callbackQuery);
        }
        if (callbackQuery.getData().startsWith("contact")) {
            return requestBotUserContact(callbackQuery);
        }
        if (callbackQuery.getData().startsWith("yes-location")) {
            return setLocationMessage(callbackQuery);
        }
        if (callbackQuery.getData().startsWith("yes-department")) {
            return setBotUserDepartment(callbackQuery);
        }
        if (callbackQuery.getData().startsWith("no-location")) {
            return setRequestMessage(callbackQuery);
        }
        if (callbackQuery.getData().startsWith("no-department")) {
            return setRefuseRequestMessage(callbackQuery);
        }

        if (callbackQuery.getData().startsWith("location")) {
            return getLocationOfMessage(callbackQuery);
        }
        if (callbackQuery.getData().startsWith("refuse")) {
            return getRefusalPlaceLocation(callbackQuery);
        }
        return ResponseContext.builder()
                .sendMessage(Collections.singletonList(SendMessage.builder()
                        .chatId(String.valueOf(callbackQuery.getMessage().getChatId()))
                        .text("Something wrong...")
                        .build()))
                .build();
    }

    private ResponseContext setRefuseRequestMessage(CallbackQuery callbackQuery) {
        BotUser superAdmin = botUserService.findByRole(Role.SUPER_ADMIN);
        return ResponseContext.builder()
                .sendMessage(List.of(SendMessage.builder()
                                .chatId(String.valueOf(callbackQuery.getMessage().getChatId()))
                                .text("Вібачте, Вам відмовлено в зміні прав доступу")
                                .build(),
                        SendMessage.builder()
                                .chatId(String.valueOf(superAdmin.getId()))
                                .text("Відмовлено")
                                .build()))
                .build();
    }

    private ResponseContext setBotUserDepartment(CallbackQuery callbackQuery) {
        Message message = callbackQuery.getMessage();
        if (botUserService.findById(message.getChatId()).isPresent()) {
            botUser = botUserService.findById(message.getChatId()).get();
        }
        String[] depText = jsonConverter.fromJson(callbackQuery.getData().substring("yes-department".length()), String[].class);
        Set<Departments> departments = new HashSet<>();
        for (String s : depText) {
            Departments department = Departments.values()[Integer.parseInt(s) - 1];
            departments.add(department);
        }
        botUser.setDepartments(departments);
        BotUser superAdmin = botUserService.findByRole(Role.SUPER_ADMIN);
        botUserService.saveBotUser(this.botUser);
        return ResponseContext.builder()
                .sendMessage(List.of(SendMessage.builder()
                                .chatId(String.valueOf(message.getChatId()))
                                .text("Ваші права доступу встановлені")
                                .build(),
                        SendMessage.builder()
                                .chatId(String.valueOf(superAdmin.getId()))
                                .text("Права доступу користувача " + botUser.getFullName() + " встановлені")
                                .build()))
                .build();
    }

    private ResponseContext getRefusalPlaceLocation(CallbackQuery callbackQuery) {
        return setRequestMessage(callbackQuery);
    }

    private ResponseContext getLocationOfMessage(CallbackQuery callbackQuery) {
        Integer messageId = jsonConverter.fromJson(callbackQuery
                .getData().substring("location".length()), Integer.class);
        userRequest = requestService.findByMessageId(messageId);
        Location messageLocation = userRequest.getLocation();
        if (messageLocation != null) {
            return ResponseContext.builder()
                    .sendLocation(Collections.singletonList(SendLocation.builder()
                            .chatId(String.valueOf(callbackQuery.getMessage().getChatId()))
                            .replyToMessageId(callbackQuery.getMessage().getMessageId())
                            .heading(messageLocation.getHeading())
                            .horizontalAccuracy(messageLocation.getHorizontalAccuracy())
                            .latitude(messageLocation.getLatitude())
                            .livePeriod(messageLocation.getLivePeriod())
                            .longitude(messageLocation.getLongitude())
                            .proximityAlertRadius(messageLocation.getProximityAlertRadius())
                            .build()))
                    .build();
        } else
            return ResponseContext.builder()
                    .sendMessage(List.of(SendMessage.builder()
                            .chatId(String.valueOf(callbackQuery.getMessage().getChatId()))
                            .replyToMessageId(callbackQuery.getMessage().getMessageId())
                            .text("Вибачте, але до заявки ID:" + messageId + " локацію не додавали")
                            .build()))
                    .build();
    }

    private ResponseContext getButtonDepartmentHandler(CallbackQuery callbackQuery) {
        String textMessage = "Департамент обрано.\nЧи бажаєте Ви додати до заявки геолокацію?";
        return buttonDepartmentHandler(callbackQuery, textMessage);
    }

    @SneakyThrows
    private ResponseContext buttonDepartmentHandler(CallbackQuery callbackQuery, String textMessage) {
        Message message = callbackQuery.getMessage();
        Departments department = jsonConverter.fromJson(callbackQuery
                .getData().substring("department".length()), Departments.class);
        chatPropertyModeService.setCurrentDepartment(message.getChatId(), department);
        return ResponseContext.builder()
                .sendMessage(List.of(SendMessage.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .text(textMessage)
                        .replyMarkup(InlineKeyboardMarkup.builder()
                                .keyboard(keyboardService.getAgreeButtons("location"))
                                .build())
                        .build()))
                .editMessageReplyMarkup(List.of(keyboardService.getCorrectReplyMarkup(message,
                        keyboardService.getDepartmentInlineButtons(message))))
                .build();
    }

    @SneakyThrows
    private ResponseContext setStateRequest(CallbackQuery callbackQuery) {
        Message message = callbackQuery.getMessage();
        Integer messageId = jsonConverter.fromJson(callbackQuery.getData().substring("message_id".length()), Integer.class);
        userRequest = requestService.findByMessageId(messageId);
        userRequest.setState(!userRequest.isState());
        requestService.saveRequest(userRequest);
        stateText.set(userRequest.isState() ? TRUE_ACTION_STATE_TEXT : FALSE_ACTION_STATE_TEXT);
        return ResponseContext.builder()
                .editMessageReplyMarkup(List.of((keyboardService.getCorrectReplyMarkup(message,
                        keyboardService.getStateRequestButton(messageId, stateText.get())))))
                .sendMessage(List.of(SendMessage.builder()
                        .chatId(userRequest.getChatId().toString())
                        .replyToMessageId(messageId)
                        .text("Ваша заявка\nID " + messageId + "\nвід " + userRequest.getDateTime() + "\n" + stateText)
                        .build()))
                .build();
    }

    @SneakyThrows
    private ResponseContext requestBotUserContact(CallbackQuery callbackQuery) {
        ResponseContext responseContext;
        Message message = callbackQuery.getMessage();
        Integer messageId = jsonConverter.fromJson(callbackQuery.getData().substring("contact".length()), Integer.class);
        Long botUserId = requestService.findByMessageId(messageId).getChatId();
        userRequest = requestService.findByMessageId(messageId);
        if (botUserService.findById(botUserId).isPresent()) {
            botUser = botUserService.findById(botUserId).get();
        }
        String phone = botUser.getPhone();
        if (phone != null) {
            responseContext = ResponseContext.builder()
                    .sendMessage(Collections.singletonList(SendMessage.builder()
                            .chatId(String.valueOf(message.getChatId()))
                            .text(userRequest.getBodyOfMessage() + "\n\nІз користувачем можна зв'язатись за телефоном:\n" + phone)
                            .build()))
                    .build();
        } else {
            responseContext = ResponseContext.builder()
                    .answerCallbackQuery(Collections.singletonList(AnswerCallbackQuery.builder()
                            .callbackQueryId(callbackQuery.getId())
                            .text("Користувач відмовився надати свій номер телефону")
                            .showAlert(true)
                            .build()))
                    .build();
        }
        return responseContext;
    }

    @SneakyThrows
    private ResponseContext setLocationMessage(CallbackQuery callbackQuery) {
        chatPropertyModeService.setBotState(callbackQuery.getMessage().getChatId(), BotState.WAIT_LOCATION);
        return ResponseContext.builder()
                .sendMessage(Collections.singletonList(SendMessage.builder()
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
                        .build()))
                .build();
    }

    private ResponseContext setRequestMessage(CallbackQuery callbackQuery) {
        chatPropertyModeService.setBotState(callbackQuery.getMessage().getChatId(), BotState.WAIT_MESSAGE);
        return ResponseContext.builder()
                .sendMessage(List.of(SendMessage.builder()
                        .chatId(String.valueOf(callbackQuery.getMessage().getChatId()))
                        .text("Добре. Введіть, будьласка, текст заявки")
                        .build()))
                .build();
    }
}
