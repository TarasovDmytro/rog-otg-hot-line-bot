package ua.tarasov.hotline.handlers;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Location;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ua.tarasov.hotline.entities.BotUser;
import ua.tarasov.hotline.entities.UserRequest;
import ua.tarasov.hotline.models.BotState;
import ua.tarasov.hotline.models.Department;
import ua.tarasov.hotline.models.Role;
import ua.tarasov.hotline.service.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Component
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MessageHandler implements RequestHandler {
    final UserRequestService requestService;
    final BotUserService botUserService;
    final KeyboardService keyboardService;
    final AdminService adminService;
    final ChatPropertyModeService chatPropertyModeService;

    final UserRequest userRequest = new UserRequest();
    BotUser botUser = new BotUser();
    List<BotApiMethod<?>> answerMessages = new ArrayList<>();

    public MessageHandler(UserRequestService requestService, BotUserService botUserService,
                          KeyboardService keyboardService, AdminService adminService, ChatPropertyModeService chatPropertyModeService) {
        this.requestService = requestService;
        this.botUserService = botUserService;
        this.keyboardService = keyboardService;
        this.adminService = adminService;
        this.chatPropertyModeService = chatPropertyModeService;
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
                    return setStartProperties(message);
                }
                case "Зробити заявку" -> {
                    return setDepartmentOfRequest(message);
                }
                case "Мої заявки" -> {
                    return getAllStateRequests(message);
                }
                case "Всі заявки" -> {
                    return getAdminAllStateRequests(message);
                }
                case "Мої не виконані заявки" -> {
                    return getFalseStateRequests(message);
                }
                case "Не виконані заявки" -> {
                    return getAdminFalseStateRequests(message);
                }
                case "змінити меню" -> {
                    return setChangeMenu(message);
                }
                case "Повідомлення всім" -> {
                    return setMessageToAll(message);
                }
                case "❌ Відмовитись" -> {
                    return setReplyKeyboard(message, START_TEXT);
                }
                default -> {
                    if (chatPropertyModeService.getBotState(message.getChatId()).equals(BotState.WAIT_MESSAGE_TO_ALL)) {
                        return sendMessageToAll(message);
                    }
                    if (message.getText().startsWith("*admin*")) return requestAdminRole(message);
                    if (chatPropertyModeService.getBotState(message.getChatId()).equals(BotState.WAIT_ADDRESS)) {
                        return setRequestAddress(message);
                    } else return createRequestMessageHandler(message);
                }
            }
        }
        if (message.hasContact()) return setBotUserPhone(message);
        if (message.hasLocation()) return setRequestLocation(message);
        return getSimpleResponseToRequest(message, WRONG_ACTION_TEXT);
    }

    private List<BotApiMethod<?>> setRequestAddress(@NotNull Message message) {
        chatPropertyModeService.setCurrentRequestAddress(message.getChatId(), message.getText());
        chatPropertyModeService.setBotState(message.getChatId(), BotState.WAIT_MESSAGE);
        return getSimpleResponseToRequest(message, "Адресу додано до заявки" +
                "\nВведіть, будьласка, текст заявки");
    }

    private @NotNull @Unmodifiable List<BotApiMethod<?>> requestAdminRole(@NotNull Message message) {
        if (botUserService.findById(message.getChatId()).isPresent()) {
            botUser = botUserService.findById(message.getChatId()).get();
        }
        String[] depText = message.getText().substring("*admin*".length()).split(":");
        String dataStartText = "department" + jsonConverter.toJson(depText);
        BotUser superAdmin = botUserService.findByRole(Role.SUPER_ADMIN);
        return List.of(SendMessage.builder()
                .chatId(String.valueOf(superAdmin.getId()))
                .text("<b>Отримана заявка від </b>" + botUser.getFullName() + "\n<b>тел.</b>" + botUser.getPhone()
                        + "\n<b>ID:</b>" + botUser.getId() + "\nна встановлення зв'язку адмін-департамент" +
                        "\nдепартаменти:" + Arrays.toString(depText) + "\nВстановити зв'язок?")
                .parseMode("HTML")
                .replyMarkup(InlineKeyboardMarkup.builder()
                        .keyboard(keyboardService.getAgreeButtons(dataStartText))
                        .build())
                .build());
    }

    private List<BotApiMethod<?>> setRequestLocation(@NotNull Message message) {
        if (chatPropertyModeService.getBotState(message.getChatId()).equals(BotState.WAIT_LOCATION)) {
            Location location = message.getLocation();
            chatPropertyModeService.setCurrentLocation(message.getChatId(), location);
            chatPropertyModeService.setBotState(message.getChatId(), BotState.WAIT_ADDRESS);
            return getSimpleResponseToRequest(message, "Локацію установлено" +
                    "\nВведіть, будьласка, адресу, за якою сталася проблема");
        } else return getSimpleResponseToRequest(message,
                "Вибачте, але локацію має сенс додавати тільки при створенні заявки.");
    }

    private List<BotApiMethod<?>> setBotUserPhone(Message message) {
        List<BotApiMethod<?>> responseMessages = getSimpleResponseToRequest(message, "Something wrong...");
        if (botUserService.findById(message.getChatId()).isPresent()) {
            botUser = botUserService.findById(message.getChatId()).get();
            String phone = message.getContact().getPhoneNumber();
            if (!phone.startsWith("+")) phone = "+" + phone;
            botUser.setPhone(phone);
            botUserService.saveBotUser(botUser);
            responseMessages = setReplyKeyboard(message, START_TEXT);
        }
        return responseMessages;
    }

    private @NotNull @Unmodifiable List<BotApiMethod<?>> setReplyKeyboard(@NotNull Message message, String messageText) {
        List<KeyboardRow> keyboardRows = chatPropertyModeService.getCurrentAdminKeyboardState(message.getChatId()) ?
                keyboardService.getAdminsReplyButtons() : keyboardService.getUserReplyButtons(message);
        return Collections.singletonList(SendMessage.builder()
                .chatId(String.valueOf(message.getChatId()))
                .text(messageText)
                .replyMarkup(ReplyKeyboardMarkup.builder()
                        .keyboard(keyboardRows)
                        .resizeKeyboard(true)
                        .oneTimeKeyboard(false)
                        .build())
                .build());
    }

    private @NotNull @Unmodifiable List<BotApiMethod<?>> setStartProperties(@NotNull Message message) {
        chatPropertyModeService.setBotState(message.getChatId(), BotState.WAIT_BUTTON);
        User user = message.getFrom();
        botUser.setId(message.getChatId());
        botUser.setUsername(user.getUserName());
        botUser.setFullName(user.getFirstName() + " " + user.getLastName());
        if (botUserService.findById(botUser.getId()).isPresent()) {
            botUser.setRole(botUserService.findById(botUser.getId()).get().getRole());
            botUser.setDepartments(botUserService.findById(botUser.getId()).get().getDepartments());
        } else {
            if ((long) botUserService.findAll().size() == 0) {
                botUser.setRole(Role.SUPER_ADMIN);
                botUser.getDepartments().addAll(Arrays.stream(Department.values()).toList());
            } else {
                botUser.setRole(Role.USER);
                botUser.setDepartments(Collections.singleton(Department.USER));
            }
        }
        botUserService.saveBotUser(botUser);
        chatPropertyModeService.setCurrentAdminKeyboardState(message.getChatId(), botUser.getRole().equals(Role.ADMIN));
        return Collections.singletonList(SendMessage.builder()
                .chatId(String.valueOf(message.getChatId()))
                .text("Радий Вас вітати, " + botUser.getFullName() +
                        "\n\nЧи бажаєте Ви додати свій телефонний номер" +
                        "\nдля майбутнього зв'язку з Вами співробітників" +
                        "\nобслуговуючих департаментів з метою будь-яких" +
                        "\nуточнень? \uD83D\uDC47")
                .replyMarkup(ReplyKeyboardMarkup.builder()
                        .keyboard(keyboardService.getAgreeAddContactReplyButtons())
                        .resizeKeyboard(true)
                        .build())
                .build());
    }

    @Contract("_ -> new")
    private @NotNull @Unmodifiable List<BotApiMethod<?>> setDepartmentOfRequest(@NotNull Message message) {
        Department currentDepartment = chatPropertyModeService.getCurrentDepartment(message.getChatId());
        chatPropertyModeService.setBotState(message.getChatId(), BotState.WAIT_BUTTON);
        return Collections.singletonList(SendMessage.builder()
                .chatId(message.getChatId().toString())
                .text("Оберіть, будьласка, обслуговуючий департамент")
                .replyMarkup(InlineKeyboardMarkup.builder()
                        .keyboard(keyboardService.getDepartmentInlineButtons(currentDepartment))
                        .build())
                .build());
    }

    private @NotNull List<BotApiMethod<?>> getAllStateRequests(@NotNull Message message) {
        List<UserRequest> messages = requestService.findMessagesByBotUser(message.getChatId());
        chatPropertyModeService.setBotState(message.getChatId(), BotState.WAIT_BUTTON);
        return sendUserListOfMessages(message, messages);
    }

    private List<BotApiMethod<?>> getAdminAllStateRequests(@NotNull Message message) {
        chatPropertyModeService.setBotState(message.getChatId(), BotState.WAIT_BUTTON);
        if (botUserService.findById(message.getChatId()).isPresent()) {
            botUser = botUserService.findById(message.getChatId()).get();
            Set<Department> departments = botUser.getDepartments();
            List<UserRequest> messages = new ArrayList<>();
            departments.forEach(department -> messages.addAll(requestService.findAllByDepartment(department)));
            answerMessages = sendAdminListOfMessages(message, messages);
        }
        return answerMessages;
    }

    private @NotNull List<BotApiMethod<?>> getFalseStateRequests(@NotNull Message message) {
        chatPropertyModeService.setBotState(message.getChatId(), BotState.WAIT_BUTTON);
        List<UserRequest> messages = requestService.findMessagesByBotUserAndState(message.getChatId(), false);
        return sendUserListOfMessages(message, messages);
    }

    private List<BotApiMethod<?>> getAdminFalseStateRequests(@NotNull Message message) {
        chatPropertyModeService.setBotState(message.getChatId(), BotState.WAIT_BUTTON);
        if (botUserService.findById(message.getChatId()).isPresent()) {
            botUser = botUserService.findById(message.getChatId()).get();
            Set<Department> departments = botUser.getDepartments();
            List<UserRequest> messages = new ArrayList<>();
            departments.forEach(department -> messages.addAll
                    (requestService.findMessagesByDepartmentAndState(department, false)));
            answerMessages = sendAdminListOfMessages(message, messages);
        }
        return answerMessages;
    }

    private @NotNull List<BotApiMethod<?>> sendUserListOfMessages(Message message, @NotNull List<UserRequest> messages) {
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
        return getSimpleResponseToRequest(message, "Наразі не існує таких заявок");
    }

    private List<BotApiMethod<?>> setChangeMenu(Message message) {
        if (adminService.checkIsAdmin(message)) {
            boolean state = chatPropertyModeService.getCurrentAdminKeyboardState(message.getChatId());
            chatPropertyModeService.setCurrentAdminKeyboardState(message.getChatId(), !state);
            String messageText = "Меню змінено, приємного користування";
            return setReplyKeyboard(message, messageText);
        }
        return List.of(adminService.getFalseAdminText(message));
    }

    private List<BotApiMethod<?>> setMessageToAll(Message message) {
        if (adminService.checkIsAdmin(message)) {
            chatPropertyModeService.setBotState(message.getChatId(), BotState.WAIT_MESSAGE_TO_ALL);
            return getSimpleResponseToRequest(message, """
                    Введіть, будьласка, повідомлення
                    для всіх користувачів""");
        }
        return Collections.singletonList(adminService.getFalseAdminText(message));
    }

    private @NotNull List<BotApiMethod<?>> sendMessageToAll(Message message) {
        if (adminService.checkIsAdmin(message)) {
            List<BotApiMethod<?>> answerMessages = new ArrayList<>();
            List<BotUser> botUsers = botUserService.findAll();
            botUsers.forEach(botUser -> answerMessages.add(SendMessage.builder()
                    .chatId(String.valueOf(botUser.getId()))
                    .text(message.getText())
                    .parseMode("HTML")
                    .build()));
            chatPropertyModeService.setBotState(message.getChatId(), BotState.WAIT_BUTTON);
            return answerMessages;
        }
        chatPropertyModeService.setBotState(message.getChatId(), BotState.WAIT_BUTTON);
        return Collections.singletonList(adminService.getFalseAdminText(message));
    }

    private List<BotApiMethod<?>> createRequestMessageHandler(@NotNull Message message) {
        if (chatPropertyModeService.getBotState(message.getChatId()).equals(BotState.WAIT_MESSAGE)) {
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
            answerMessages.addAll(getSimpleResponseToRequest(message,
                    "\uD83D\uDC4D\nДякуємо, Ваша заявка\nID " + userRequest.getMessageId() +
                            "\nвід " + userRequest.getDateTimeToString() + "\nприйнята"));
            chatPropertyModeService.setBotState(message.getChatId(), BotState.WAIT_BUTTON);
            return answerMessages;
        } else return getSimpleResponseToRequest(message, "Вибачте, але я бот і читати не вмію." +
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
        userRequest.setBodyOfMessage(userRequest.getDepartment() + "\nID " + userRequest.getMessageId() +
                "\nвід " + userRequest.getDateTimeToString() + "\n\n" + message.getText() +
                "\n\nадреса: " + userRequest.getAddress() + "\n" + isLocation);
        userRequest.setState(false);
        requestService.cleanRequestData();
        requestService.saveRequest(userRequest);
        return userRequest;
    }
}
