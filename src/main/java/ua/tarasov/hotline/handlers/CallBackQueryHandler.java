package ua.tarasov.hotline.handlers;

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
import ua.tarasov.hotline.models.entities.BotUser;
import ua.tarasov.hotline.models.entities.UserRequest;
import ua.tarasov.hotline.models.model.BotState;
import ua.tarasov.hotline.models.model.Department;
import ua.tarasov.hotline.models.model.Role;
import ua.tarasov.hotline.service.BotUserService;
import ua.tarasov.hotline.service.ChatPropertyModeService;
import ua.tarasov.hotline.service.KeyboardService;
import ua.tarasov.hotline.service.UserRequestService;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CallBackQueryHandler implements RequestHandler {
    final UserRequestService requestService;
    final BotUserService botUserService;
    final KeyboardService keyboardService;
    final ChatPropertyModeService chatPropertyModeService;

    UserRequest userRequest = new UserRequest();
    BotUser botUser = new BotUser();

    public CallBackQueryHandler(UserRequestService requestService, BotUserService botUserService, KeyboardService keyboardService, ChatPropertyModeService chatPropertyModeService) {
        this.requestService = requestService;
        this.botUserService = botUserService;
        this.keyboardService = keyboardService;
        this.chatPropertyModeService = chatPropertyModeService;
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
        if (callbackQuery.getData().startsWith("refuse_request")) {
            return setRefuseRequest(callbackQuery);
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
                        .text("Вибачте, Вам відмовлено в зміні прав доступу")
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
        Set<Department> departments = new HashSet<>();
        for (String s : depText) {
            Department department = Department.values()[Integer.parseInt(s) - 1];
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
        return getSimpleResponseToRequest(message, "Ви не можете отримати шнформацію, пов'язану із цією заявкою," +
                                                   " бо, на теперішній час її вже не існує");
    }

    private List<BotApiMethod<?>> getButtonDepartmentHandler(CallbackQuery callbackQuery) {
        log.info("get button department");
        String textMessage = "Департамент обрано.\nЧи бажаєте Ви додати до заявки геолокацію?";
        return buttonDepartmentHandler(callbackQuery, textMessage);
    }

    @SneakyThrows
    private List<BotApiMethod<?>> buttonDepartmentHandler(CallbackQuery callbackQuery, String textMessage) {
        log.info("button department handler");
        Message message = callbackQuery.getMessage();
        Department department = jsonConverter.fromJson(callbackQuery
                .getData().substring("department".length()), Department.class);
        chatPropertyModeService.setCurrentDepartment(message.getChatId(), department);
        return List.of(
                keyboardService.getCorrectReplyMarkup(message, keyboardService.getDepartmentInlineButtons(message)),
                SendMessage.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .text(textMessage)
                        .replyMarkup(InlineKeyboardMarkup.builder()
                                .keyboard(keyboardService.getAgreeButtons("location"))
                                .build())
                        .build()
        );
    }

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

    private List<BotApiMethod<?>> setRefuseRequest(CallbackQuery callbackQuery) {
        Integer messageId = jsonConverter.fromJson(callbackQuery.getData().substring("refuse_request".length()), Integer.class);
        userRequest = requestService.findByMessageId(messageId);
        long chatId = userRequest.getChatId();
        requestService.deleteUserRequest(userRequest);
        return List.of(SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .replyToMessageId(messageId)
                .text("Нажаль, ми вимушені відмовити Вам у виконанні заявки" +
                      "\nID " + messageId + "\nвід " + userRequest.getDateTime() +
                      "\n\nВаша заявка не є в компетенції нашого департаменту," +
                      "\nабо її не можливо виконати з незалежних від нас причин")
                .build());
    }

    @SneakyThrows
    private List<BotApiMethod<?>> requestBotUserContact(CallbackQuery callbackQuery) {
        Message message = callbackQuery.getMessage();
        Integer messageId = jsonConverter.fromJson(callbackQuery.getData().substring("contact".length()), Integer.class);
        userRequest = requestService.findByMessageId(messageId);
        if (userRequest != null) {
            Long botUserId = userRequest.getChatId();
            if (botUserService.findById(botUserId).isPresent()) {
                botUser = botUserService.findById(botUserId).get();
            }
            String phone = botUser.getPhone();
            List<BotApiMethod<?>> answerMessage;
            if (phone != null) {
                String messageText = userRequest.getBodyOfMessage() +
                                     "\n\nІз користувачем можна зв'язатись за телефоном:\n"
                                     + phone;
                answerMessage = getSimpleResponseToRequest(message, messageText);
            } else {
                answerMessage = Collections.singletonList(AnswerCallbackQuery.builder()
                        .callbackQueryId(callbackQuery.getId())
                        .text("Користувач відмовився надати свій номер телефону")
                        .showAlert(true)
                        .build());
            }
            return answerMessage;
        }
        return getSimpleResponseToRequest(message, "Ви не можете отримати шнформацію, пов'язану із цією заявкою," +
                                                   " бо, на теперішній час її вже не існує");
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
        return getSimpleResponseToRequest(callbackQuery.getMessage(), "Добре. Введіть, будьласка, текст заявки");
    }
}
