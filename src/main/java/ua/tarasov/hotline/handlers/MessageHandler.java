package ua.tarasov.hotline.handlers;

import com.google.gson.Gson;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Location;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ua.tarasov.hotline.models.model.BotState;
import ua.tarasov.hotline.models.model.Departments;
import ua.tarasov.hotline.models.model.ResponseContext;
import ua.tarasov.hotline.models.model.Role;
import ua.tarasov.hotline.models.entities.BotUser;
import ua.tarasov.hotline.models.entities.UserRequest;
import ua.tarasov.hotline.service.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class MessageHandler implements RequestHandler {
    private final UserRequestService requestService;
    private final BotUserService botUserService;
    private final KeyboardService keyboardService;
    private final AdminService adminService;
    private final ChatPropertyModeService chatPropertyModeService = ChatPropertyModeService.getChatProperties();

    private final String TRUE_STATE_TEXT = "✅  Виконана";
    private final String FALSE_STATE_TEXT = "⭕️ На виконанні";
    private final String START_TEXT = "\uD83D\uDC4C Дякую, давайте почнемо";
    private final AtomicReference<String> stateText = new AtomicReference<>("null");
    private final AtomicReference<Location> location = new AtomicReference<>(null);
    private final AtomicReference<ResponseContext> responseContext = new AtomicReference<>(null);

    private UserRequest userRequest = new UserRequest();
    private BotUser botUser = new BotUser();

    public MessageHandler(UserRequestService requestService, BotUserService botUserService,
                          KeyboardService keyboardService, AdminService adminService) {
        this.requestService = requestService;
        this.botUserService = botUserService;
        this.keyboardService = keyboardService;
        this.adminService = adminService;
    }

    @Override
    public ResponseContext getResponseContext(Update update) {
        Message message = update.getMessage();
        String messageText = message.getText();
        if (message.getContact() == null && message.getLocation() == null) {
            switch (messageText) {
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
                    if (message.getText().startsWith("@@")) return sendMessageToAll(message);
                    if (messageText.startsWith("*admin*")) return requestAdminRole(message);
                    else return createRequestMessageHandler(message);
                }
            }
        }
        if (message.getContact() != null) return setBotUserPhone(message);
        if (message.getLocation() != null) return setLocation(message);
        return getResponseToRequest(message, "Something wrong...");
    }

    private ResponseContext requestAdminRole(Message message) {
        if (botUserService.findById(message.getChatId()).isPresent()) {
            botUser = botUserService.findById(message.getChatId()).get();
        }
        String[] depText = message.getText().substring("*admin*".length()).split(":");
        String dataStartText = "department" + new Gson().toJson(depText);
        BotUser superAdmin = botUserService.findByRole(Role.SUPER_ADMIN);
        return ResponseContext.builder()
                .sendMessage(List.of(SendMessage.builder()
                        .chatId(String.valueOf(superAdmin.getId()))
                        .text("Отримана заявка від " + botUser.getFullName() + "\nтел." + botUser.getPhone()
                              + ", ID:" + botUser.getId() + "\nна встановлення зв'язку адмін-департамент" +
                              "\nдепартаменти:" + Arrays.toString(depText) + "\nВстановити зв'язок?")
                        .replyMarkup(InlineKeyboardMarkup.builder()
                                .keyboard(keyboardService.getAgreeButtons(message, dataStartText))
                                .build())
                        .build()))
                .build();
    }

    private ResponseContext getResponseToRequest(Message message, String textMessage) {
        return ResponseContext.builder()
                .sendMessage(Collections.singletonList(SendMessage.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .text(textMessage)
                        .build()))
                .build();
    }

    private ResponseContext setLocation(Message message) {
        if (chatPropertyModeService.getBotState(message.getChatId()).equals(BotState.WAIT_LOCATION)) {
            Location location = message.getLocation();
            this.location.set(location);
            chatPropertyModeService.setBotState(message.getChatId(), BotState.WAIT_MESSAGE);
            return getResponseToRequest(message, "Локацію установлено" +
                                                 "\nВведіть, будьласка, текст заявки");
        } else return ResponseContext.builder()
                .sendMessage(Collections.singletonList(SendMessage.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .text("Вибачте, але локацію має сенс додавати тільки при створенні заявки.")
                        .build()))
                .build();

    }

    private ResponseContext setBotUserPhone(Message message) {
        ResponseContext responseContext = getResponseToRequest(message, "Something wrong...");
        if (botUserService.findById(message.getChatId()).isPresent()) {
            botUser = botUserService.findById(message.getChatId()).get();
            botUser.setPhone(message.getContact().getPhoneNumber());
            botUserService.saveBotUser(botUser);
            responseContext = setReplyKeyboard(message, START_TEXT);
        }
        return responseContext;
    }

    private ResponseContext setReplyKeyboard(Message message, String messageText) {
        List<KeyboardRow> keyboardRows = chatPropertyModeService.getCurrentAdminKeyboardState(message.getChatId()) ?
                keyboardService.getAdminsReplyButtons() : keyboardService.getUserReplyButtons(message);
        return ResponseContext.builder()
                .sendMessage(Collections.singletonList(SendMessage.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .text(messageText)
                        .replyMarkup(ReplyKeyboardMarkup.builder()
                                .keyboard(keyboardRows)
                                .resizeKeyboard(true)
                                .oneTimeKeyboard(false)
                                .build())
                        .build()))
                .build();
    }

    private ResponseContext setStartProperties(Message message) {
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
                botUser.getDepartments().addAll(Arrays.stream(Departments.values()).toList());
            } else {
                botUser.setRole(Role.USER);
                botUser.getDepartments().add(Departments.USER);
            }
        }
        botUserService.saveBotUser(botUser);
        chatPropertyModeService.setCurrentAdminKeyboardState(message.getChatId(), botUser.getRole().equals(Role.ADMIN));
        return ResponseContext.builder()
                .sendMessage(Collections.singletonList(SendMessage.builder()
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
                        .build()))
                .build();
    }

    private ResponseContext setDepartmentOfRequest(Message message) {
        return ResponseContext.builder()
                .sendMessage(Collections.singletonList(SendMessage.builder()
                        .chatId(message.getChatId().toString())
                        .text("Оберіть, будьласка, обслуговуючий департамент")
                        .replyMarkup(InlineKeyboardMarkup.builder()
                                .keyboard(keyboardService.getDepartmentInlineButtons(message))
                                .build())
                        .build()))
                .build();
    }

    private ResponseContext getAllStateRequests(Message message) {
        List<UserRequest> messages = requestService.findMessagesByBotUser(message.getChatId());
        return ResponseContext.builder()
                .sendMessage(sendUserListOfMessages(message, messages))
                .build();
    }

    private ResponseContext getAdminAllStateRequests(Message message) {

        if (botUserService.findById(message.getChatId()).isPresent()) {
            botUser = botUserService.findById(message.getChatId()).get();
            Set<Departments> departments = botUser.getDepartments();
            List<UserRequest> messages = new ArrayList<>();
            departments.forEach(department -> messages.addAll(requestService.findAllByDepartment(department)));
            responseContext.set(ResponseContext.builder()
                    .sendMessage(sendAdminListOfMessages(message, messages))
                    .build());
        }
        return responseContext.get();
    }

    private ResponseContext getFalseStateRequests(Message message) {
        List<UserRequest> messages = requestService.findMessagesByBotUserAndState(message.getChatId(), false);
        return ResponseContext.builder()
                .sendMessage(sendUserListOfMessages(message, messages))
                .build();
    }

    private ResponseContext getAdminFalseStateRequests(Message message) {
        if (botUserService.findById(message.getChatId()).isPresent()) {
            botUser = botUserService.findById(message.getChatId()).get();
            Set<Departments> departments = botUser.getDepartments();
            List<UserRequest> messages = new ArrayList<>();
            departments.forEach(department -> messages.addAll
                    (requestService.findMessagesByDepartmentAndState(department, false)));
            responseContext.set(ResponseContext.builder()
                    .sendMessage(sendAdminListOfMessages(message, messages))
                    .build());
        }
        return responseContext.get();
    }

    private List<SendMessage> sendUserListOfMessages(Message message, List<UserRequest> messages) {
        List<SendMessage> answerMessages = new ArrayList<>();
        if (!messages.isEmpty()) {
            messages.sort(Comparator.comparing(UserRequest::getId));
            messages.forEach(currentMessage -> {
                stateText.set(currentMessage.isState() ? TRUE_STATE_TEXT : FALSE_STATE_TEXT);
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

    private List<SendMessage> sendAdminListOfMessages(Message message, List<UserRequest> messages) {
        List<SendMessage> answerMessages = new ArrayList<>();
        if (!messages.isEmpty()) {
            messages.sort(Comparator.comparing(UserRequest::getId));
            messages.forEach(currentMessage -> {
                stateText.set(currentMessage.isState() ? TRUE_STATE_TEXT : FALSE_STATE_TEXT);
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
        } else answerMessages.add(SendMessage.builder()
                .chatId(String.valueOf(message.getChatId()))
                .text("Наразі не існує таких заявок")
                .build());
        return answerMessages;
    }

    private ResponseContext setChangeMenu(Message message) {
        if (adminService.checkIsAdmin(message)) {
            boolean state = chatPropertyModeService.getCurrentAdminKeyboardState(message.getChatId());
            chatPropertyModeService.setCurrentAdminKeyboardState(message.getChatId(), !state);
            String messageText = "Меню змінено, приємного користування";
            return setReplyKeyboard(message, messageText);
        } else return ResponseContext.builder()
                .sendMessage(List.of(adminService.getFalseAdminText(message)))
                .build();
    }

    private ResponseContext setMessageToAll(Message message) {
        if (adminService.checkIsAdmin(message)) {
            chatPropertyModeService.setBotState(message.getChatId(), BotState.WAIT_MESSAGE_TO_ALL);
            return getResponseToRequest(message, """
                    Введіть, будьласка, повідомлення
                    для всіх користувачів в форматі:

                    @@'Ваше повідомлення'""");
        }
        return ResponseContext.builder()
                .sendMessage(Collections.singletonList(adminService.getFalseAdminText(message)))
                .build();
    }

    private ResponseContext sendMessageToAll(Message message) {
        if (adminService.checkIsAdmin(message)) {
            List<SendMessage> answerMessages = new ArrayList<>();
            List<BotUser> botUsers = botUserService.findAll();
            botUsers.forEach(botUser -> answerMessages.add(SendMessage.builder()
                    .chatId(String.valueOf(botUser.getId()))
                    .text(message.getText().substring("@@".length()))
                    .build()));
            chatPropertyModeService.setBotState(message.getChatId(), BotState.WAIT_BUTTON);
            return ResponseContext.builder()
                    .sendMessage(answerMessages)
                    .build();
        }
        chatPropertyModeService.setBotState(message.getChatId(), BotState.WAIT_BUTTON);
        return ResponseContext.builder()
                .sendMessage(Collections.singletonList(adminService.getFalseAdminText(message)))
                .build();
    }

    private ResponseContext createRequestMessageHandler(Message message) {
        if (chatPropertyModeService.getBotState(message.getChatId()).equals(BotState.WAIT_MESSAGE_TO_ALL)) {
            return getResponseToRequest(message, """
                    Невірний формат повідомлення.
                    Введіть повідомлення в форматі:

                    @@'Ваше повідомлення'""");
        }
        if (chatPropertyModeService.getBotState(message.getChatId()).equals(BotState.WAIT_MESSAGE)) {
            userRequest = createNewUserRequest(message);
            List<BotUser> botUsers = botUserService.findAllByDepartment(userRequest.getDepartment());
            List<SendMessage> answerMessages = new ArrayList<>();
            if (!botUsers.isEmpty()) {
                botUsers.forEach(botUser -> answerMessages.add(SendMessage.builder()
                        .chatId(String.valueOf(botUser.getId()))
                        .text(userRequest.getBodyOfMessage() +
                              "\n\n" + FALSE_STATE_TEXT)
                        .build()));
            }
            answerMessages.add((SendMessage.builder()
                    .chatId(message.getChatId().toString())
                    .text("\uD83D\uDC4D\nДякуємо, Ваша заявка\nID " + userRequest.getMessageId() +
                          "\nвід " + userRequest.getDateTime() + "\nприйнята")
                    .build()));
            chatPropertyModeService.setBotState(message.getChatId(), BotState.WAIT_BUTTON);
            return ResponseContext.builder()
                    .sendMessage(answerMessages)
                    .build();
        } else return ResponseContext.builder()
                .sendMessage(List.of(SendMessage.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .text("Вибачте, але я бот, а не людина і читати не вмію." +
                              " Виконайте, будьласка, коректну дію за допомогою кнопок")
                        .build()))
                .build();
    }

    private UserRequest createNewUserRequest(Message message) {
        userRequest.setId(0L);
        userRequest.setDepartment(chatPropertyModeService.getCurrentDepartment(message.getChatId()));
        userRequest.setChatId(message.getChatId());
        userRequest.setMessageId(message.getMessageId());
        userRequest.setDateTime(LocalDateTime.now());
        userRequest.setLocation(location.get());
        String isLocation = userRequest.getLocation() != null ? "Локація: +" : "Локація: --";
        userRequest.setBodyOfMessage(userRequest.getDepartment() + "\nID " + userRequest.getMessageId() +
                                     "\nвід " + userRequest.getDateTime() + "\n\n" + message.getText() + "\n\n" + isLocation);
        userRequest.setState(false);
        requestService.saveRequest(userRequest);
        this.location.set(null);
        return userRequest;
    }
}
