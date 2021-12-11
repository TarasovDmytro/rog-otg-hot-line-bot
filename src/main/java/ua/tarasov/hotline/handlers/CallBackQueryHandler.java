package ua.tarasov.hotline.handlers;

import com.google.gson.Gson;
import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Location;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ua.tarasov.hotline.models.model.BotState;
import ua.tarasov.hotline.models.entities.BotUser;
import ua.tarasov.hotline.models.model.Departments;
import ua.tarasov.hotline.models.entities.UserRequest;
import ua.tarasov.hotline.models.model.Role;
import ua.tarasov.hotline.service.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CallBackQueryHandler implements RequestHandler {
    final UserRequestService requestService;
    final BotUserService botUserService;
    final KeyboardService keyboardService;
    final ChatPropertyModeService chatPropertyModeService = ChatPropertyModeService.getChatProperties();

    final String TRUE_ACTION_STATE_TEXT = "✅  Виконана";
    final String FALSE_ACTION_STATE_TEXT = "⭕️ На виконанні";
    final AtomicReference<String> stateText = new AtomicReference<>("null");
    final Gson jsonConverter = new Gson();

    UserRequest userRequest = new UserRequest();
    BotUser botUser = new BotUser();
    List<BotApiMethod<?>> answerMessage = new ArrayList<>();

    public CallBackQueryHandler(UserRequestService requestService, BotUserService botUserService, KeyboardService keyboardService) {
        this.requestService = requestService;
        this.botUserService = botUserService;
        this.keyboardService = keyboardService;
    }

    @Override
    public List<BotApiMethod<?>> getHandlerUpdate(Update update) {
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
        return Collections.singletonList(SendMessage.builder()
                .chatId(String.valueOf(callbackQuery.getMessage().getChatId()))
                .text("Something wrong...")
                .build());
    }

    private List<BotApiMethod<?>> setRefuseRequestMessage(CallbackQuery callbackQuery) {
        BotUser superAdmin = botUserService.findByRole(Role.SUPER_ADMIN);
        return (List.of(SendMessage.builder()
                        .chatId(String.valueOf(callbackQuery.getMessage().getChatId()))
                        .text("Вібачте, Вам відмовлено в зміні прав доступу")
                        .build(),
                SendMessage.builder()
                        .chatId(String.valueOf(superAdmin.getId()))
                        .text("Відмовлено")
                        .build()));
    }

    private List<BotApiMethod<?>> setBotUserDepartment(CallbackQuery callbackQuery) {
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
        return List.of(SendMessage.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .text("Ваші права доступу встановлені")
                        .build(),
                SendMessage.builder()
                        .chatId(String.valueOf(superAdmin.getId()))
                        .text("Права доступу користувача " + botUser.getFullName() + " встановлені")
                        .build());
    }

    private List<BotApiMethod<?>> getRefusalPlaceLocation(CallbackQuery callbackQuery) {
        return setRequestMessage(callbackQuery);
    }

    private List<BotApiMethod<?>> getLocationOfMessage(CallbackQuery callbackQuery) {
        Integer messageId = jsonConverter.fromJson(callbackQuery
                .getData().substring("location".length()), Integer.class);
        userRequest = requestService.findByMessageId(messageId);
        Location messageLocation = userRequest.getLocation();
        if (messageLocation != null) {
            return Collections.singletonList(SendLocation.builder()
                    .chatId(String.valueOf(callbackQuery.getMessage().getChatId()))
                    .replyToMessageId(callbackQuery.getMessage().getMessageId())
                    .heading(messageLocation.getHeading())
                    .horizontalAccuracy(messageLocation.getHorizontalAccuracy())
                    .latitude(messageLocation.getLatitude())
                    .livePeriod(messageLocation.getLivePeriod())
                    .longitude(messageLocation.getLongitude())
                    .proximityAlertRadius(messageLocation.getProximityAlertRadius())
                    .build());
        } else
            return List.of(SendMessage.builder()
                    .chatId(String.valueOf(callbackQuery.getMessage().getChatId()))
                    .replyToMessageId(callbackQuery.getMessage().getMessageId())
                    .text("Вибачте, але до заявки ID:" + messageId + " локацію не додавали")
                    .build());
    }

    private List<BotApiMethod<?>> getButtonDepartmentHandler(CallbackQuery callbackQuery) {
        log.info("get button department");
        String textMessage = "Департамент обрано.\nЧи бажаєте Ви додати до заявки геолокацію?";
        return buttonDepartmentHandler(callbackQuery, textMessage);
    }

    @SneakyThrows
    private List<BotApiMethod<?>> buttonDepartmentHandler(CallbackQuery callbackQuery, String textMessage) {
        Message message = callbackQuery.getMessage();
        Departments department = jsonConverter.fromJson(callbackQuery
                .getData().substring("department".length()), Departments.class);
        chatPropertyModeService.setCurrentDepartment(message.getChatId(), department);
        return List.of(SendMessage.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .text(textMessage)
                        .replyMarkup(InlineKeyboardMarkup.builder()
                                .keyboard(keyboardService.getAgreeButtons("location"))
                                .build())
                        .build(),
                keyboardService.getCorrectReplyMarkup(message, keyboardService.getDepartmentInlineButtons(message)));
    }

    @SneakyThrows
    private List<BotApiMethod<?>> setStateRequest(CallbackQuery callbackQuery) {
        Message message = callbackQuery.getMessage();
        Integer messageId = jsonConverter.fromJson(callbackQuery.getData().substring("message_id".length()), Integer.class);
        userRequest = requestService.findByMessageId(messageId);
        userRequest.setState(!userRequest.isState());
        requestService.saveRequest(userRequest);
        stateText.set(userRequest.isState() ? TRUE_ACTION_STATE_TEXT : FALSE_ACTION_STATE_TEXT);
        return List.of(keyboardService.getCorrectReplyMarkup(message,
                        keyboardService.getStateRequestButton(messageId, stateText.get())),
                SendMessage.builder()
                        .chatId(userRequest.getChatId().toString())
                        .replyToMessageId(messageId)
                        .text("Ваша заявка\nID " + messageId + "\nвід " + userRequest.getDateTime() + "\n" + stateText)
                        .build());
    }

    @SneakyThrows
    private List<BotApiMethod<?>> requestBotUserContact(CallbackQuery callbackQuery) {
        Message message = callbackQuery.getMessage();
        Integer messageId = jsonConverter.fromJson(callbackQuery.getData().substring("contact".length()), Integer.class);
        Long botUserId = requestService.findByMessageId(messageId).getChatId();
        userRequest = requestService.findByMessageId(messageId);
        if (botUserService.findById(botUserId).isPresent()) {
            botUser = botUserService.findById(botUserId).get();
        }
        String phone = botUser.getPhone();
        if (phone != null) {
            answerMessage = Collections.singletonList(SendMessage.builder()
                    .chatId(String.valueOf(message.getChatId()))
                    .text(userRequest.getBodyOfMessage() + "\n\nІз користувачем можна зв'язатись за телефоном:\n" + phone)
                    .build());
        } else {
            answerMessage = Collections.singletonList(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQuery.getId())
                    .text("Користувач відмовився надати свій номер телефону")
                    .showAlert(true)
                    .build());
        }
        return answerMessage;
    }

    @SneakyThrows
    private List<BotApiMethod<?>> setLocationMessage(CallbackQuery callbackQuery) {
        chatPropertyModeService.setBotState(callbackQuery.getMessage().getChatId(), BotState.WAIT_LOCATION);
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

    private List<BotApiMethod<?>> setRequestMessage(CallbackQuery callbackQuery) {
        chatPropertyModeService.setBotState(callbackQuery.getMessage().getChatId(), BotState.WAIT_MESSAGE);
        return List.of(SendMessage.builder()
                .chatId(String.valueOf(callbackQuery.getMessage().getChatId()))
                .text("Добре. Введіть, будьласка, текст заявки")
                .build());
    }
}
