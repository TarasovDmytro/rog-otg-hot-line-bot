package ua.tarasov.hotline.controller.impl;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ua.tarasov.hotline.controller.Controller;
import ua.tarasov.hotline.entities.BotUser;
import ua.tarasov.hotline.entities.UserRequest;
import ua.tarasov.hotline.models.Department;
import ua.tarasov.hotline.models.Role;
import ua.tarasov.hotline.models.StateOfRequest;
import ua.tarasov.hotline.service.BotUserService;
import ua.tarasov.hotline.service.KeyboardService;
import ua.tarasov.hotline.service.UserRequestService;
import ua.tarasov.hotline.service.impl.BotUserServiceImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SuperAdminController implements Controller {
    final BotUserService botUserService;
    final KeyboardService keyboardService;
    final UserRequestService requestService;
    BotUser botUser = new BotUser();

    public SuperAdminController(BotUserServiceImpl botUserService, KeyboardService keyboardService,
                                UserRequestService requestService) {
        this.botUserService = botUserService;
        this.keyboardService = keyboardService;
        this.requestService = requestService;
    }

    public List<BotApiMethod<?>> getComplaint(@NotNull CallbackQuery callbackQuery) {
        Integer messageId = jsonConverter.fromJson(callbackQuery.getData().substring("complaint".length()), Integer.class);
        UserRequest userRequest = requestService.findByMessageId(messageId);
        if (botUserService.findById(userRequest.getChatId()).isPresent()) {
            botUser = botUserService.findById(userRequest.getChatId()).get();
        } else Controller.getSimpleResponseToRequest(callbackQuery.getMessage(), "Такого користувача не існує");
        if (botUser.getWarningCount() < 3) {
            BotUser superAdmin = botUserService.findByRole(Role.SUPER_ADMIN);
            List<List<InlineKeyboardButton>> buttons = keyboardService.getComplaintButton(messageId);
            return List.of(SendMessage.builder()
                            .chatId(String.valueOf(superAdmin.getId()))
                            .text("Отримана скарга на користувача\n" +
                                    "ID " + botUser.getId() +
                                    "\n" + botUser.getFullName() +
                                    "\nпо заявці ID " + messageId)
                            .build(),
                    SendMessage.builder()
                            .chatId(String.valueOf(superAdmin.getId()))
                            .text(userRequest.toString())
                            .replyMarkup(InlineKeyboardMarkup.builder()
                                    .keyboard(buttons)
                                    .build())
                            .build());
        } else
            return Controller.getSimpleResponseToRequest(callbackQuery.getMessage(), "Цей користувач вже заблокований");
    }

    public List<BotApiMethod<?>> sendComplaint(@NotNull CallbackQuery callbackQuery) {
        Integer messageId = jsonConverter.fromJson(callbackQuery.getData().substring("agree_complaint".length()), Integer.class);
        UserRequest userRequest = requestService.findByMessageId(messageId);
        Long userId = userRequest.getChatId();
        if (botUserService.findById(userId).isPresent()) {
            botUser = botUserService.findById(userId).get();
        }
        botUser.setWarningCount(botUser.getWarningCount() + 1);
        botUserService.saveBotUser(botUser);
        return List.of(SendMessage.builder()
                        .chatId(String.valueOf(userId))
                        .text("❗ Ви отримали " + botUser.getWarningCount() + " попередження за некоректне використання сервісу." +
                                "\n❗️❗️❗️При отриманні 3 попереджень користувач автоматично блокується.\nСкарга була подана " +
                                "на Вашу заявку\nID " + messageId)
                        .build(),
                SendMessage.builder()
                        .chatId(String.valueOf(userId))
                        .text(userRequest.toString())
                        .build());
    }

    public List<BotApiMethod<?>> getMembers(@NotNull Message message) {
        if (botUserService.checkIsSuperAdmin(message.getChatId())) {
            return countOfMembers(message);
        }
        return Collections.singletonList(botUserService.getFalseAdminText(message.getChatId()));
    }

    private @NotNull @Unmodifiable List<BotApiMethod<?>> countOfMembers(Message message) {
        List<BotUser> members = botUserService.findAll();
        long counter = members.size();
        List<BotUser> blockingMembers = new ArrayList<>();
        members.forEach(user -> {
            if (user.getWarningCount()>2){
                blockingMembers.add(user);
            }
        });
        long blockCounter = blockingMembers.size();
        return Controller.getSimpleResponseToRequest(message, "Сервісом користуються " + counter +
                " користувачей\nз них " + blockCounter + " заблокованих");
    }

    public List<BotApiMethod<?>> getManagementMenu(Message message) {
        if (botUserService.checkIsSuperAdmin(message.getChatId())) {
            List<String> namesOfButtons = List.of("Знайти користувача", "Вийти");
            return keyboardService.setMenuReplyKeyboard(message.getChatId(), namesOfButtons,
                    "Виберіть, будь ласка, необхідну дію");
        } else return List.of(botUserService.getFalseAdminText(message.getChatId()));
    }

    public List<BotApiMethod<?>> manage(Message message) {
        if (botUserService.checkIsSuperAdmin(message.getChatId())) {
            String messageText = message.getText();
            List<BotApiMethod<?>> methods = new ArrayList<>();
            switch (messageText) {
                case "Вийти" -> {
                    chatPropertyModeService.setCurrentStateOfRequest(message.getChatId(), StateOfRequest.REQUEST_CREATED);
                    methods.addAll(keyboardService.setReplyKeyboardOfUser(message.getChatId(), "Приємного користування"));
                }
                case "Знайти користувача" -> methods.addAll(Controller.getSimpleResponseToRequest(message, "Введіть телефонний номер"));
                case "Видалити" -> {
                    botUserService.deleteUser(botUser);
                    methods.addAll(Controller.getSimpleResponseToRequest(message, "Користувача видалено"));
                    methods.addAll(getManagementMenu(message));
                }
                case "Заблокувати" -> {
                    botUser.setWarningCount(3);
                    botUserService.saveBotUser(botUser);
                    methods.addAll(Controller.getSimpleResponseToRequest(message, "Користувача заблоковано"));
                    methods.addAll(getManagementMenu(message));
                }
                case "Розблокувати" -> {
                    botUser.setWarningCount(0);
                    botUserService.saveBotUser(botUser);
                    methods.addAll(Controller.getSimpleResponseToRequest(message, "Користувача розблоковано"));
                    methods.addAll(getManagementMenu(message));
                }
                case "Передати права" -> {
                    BotUser superAdmin = botUserService.findByRole(Role.SUPER_ADMIN);
                    botUser.setRole(Role.SUPER_ADMIN);
                    botUser.setDepartments(Arrays.stream(Department.values()).collect(Collectors.toSet()));
                    superAdmin.setRole(Role.ADMIN);
                    superAdmin.setDepartments(Stream.of(Department.USER).collect(Collectors.toSet()));
                    botUserService.saveBotUser(botUser);
                    botUserService.saveBotUser(superAdmin);
                    methods.add(SendMessage.builder()
                            .chatId(String.valueOf(botUser.getId()))
                            .text("Ваш рівень доступу змінений на " + botUser.getRole())
                            .build());
                    methods.addAll(Controller.getSimpleResponseToRequest(message, "Ваш рівень доступу змінений на " + superAdmin.getRole()));
                    chatPropertyModeService.setCurrentStateOfRequest(message.getChatId(), StateOfRequest.REQUEST_CREATED);
                    methods.addAll(keyboardService.setReplyKeyboardOfUser(message.getChatId(), "Приємного користування"));
                }
                default -> {
                    if (messageText.startsWith("+")) {
                        if (botUserService.findByPhone(messageText).isPresent()) {
                            botUser = botUserService.findByPhone(messageText).get();
                            List<String> namesOfButtons = List.of("Видалити", "Заблокувати", "Розблокувати", "Передати права", "Вийти");
                            methods.addAll(Controller.getSimpleResponseToRequest(message, String.valueOf(botUser)));
                            methods.addAll(keyboardService.setMenuReplyKeyboard(message.getChatId(), namesOfButtons,
                                    "Виберіть, будь ласка, необхідну дію"));
                        } else
                            methods.addAll(Controller.getSimpleResponseToRequest(message, "Такого користувача не існує"));
                    } else
                        methods.addAll(Controller.getSimpleResponseToRequest(message, "Fail telephone number format"));
                }
            }
            return methods;
        } else return List.of(botUserService.getFalseAdminText(message.getChatId()));
    }
}
