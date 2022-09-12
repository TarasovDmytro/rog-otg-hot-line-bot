package ua.tarasov.hotline.controller.impl;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
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
import ua.tarasov.hotline.handlers.RequestHandler;
import ua.tarasov.hotline.models.Department;
import ua.tarasov.hotline.models.Role;
import ua.tarasov.hotline.models.StateOfRequest;
import ua.tarasov.hotline.service.BotUserService;
import ua.tarasov.hotline.service.KeyboardService;
import ua.tarasov.hotline.service.UserRequestService;
import ua.tarasov.hotline.service.impl.BotUserServiceImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SuperAdminController implements Controller {
    final BotUserService botUserService;
    final KeyboardService keyboardService;
    final UserRequestService requestService;
    final DepartmentController departmentController;
    List<Department> departments = new ArrayList<>();
    BotUser botUser = new BotUser();

    public SuperAdminController(BotUserServiceImpl botUserService, KeyboardService keyboardService,
                                UserRequestService requestService, DepartmentController departmentController) {
        this.botUserService = botUserService;
        this.keyboardService = keyboardService;
        this.requestService = requestService;
        this.departmentController = departmentController;
    }

    private @NotNull List<BotApiMethod<?>> requestRole(@NotNull Message message, List<Department> departments) {
        BotUser admin = new BotUser();
        if (botUserService.findById(message.getChatId()).isPresent()) {
            admin = botUserService.findById(message.getChatId()).get();
        }
        List<String> depText = new ArrayList<>();
        depText.add(botUser.getId().toString());
        departments.forEach(department -> depText.add(String.valueOf((department.ordinal()))));
        String dataStartText = "department" + jsonConverter.toJson(depText);
        BotUser superAdmin = botUserService.findByRole(Role.SUPER_ADMIN);
        chatPropertyModeService.setCurrentStateOfRequest(message.getChatId(), StateOfRequest.REQUEST_CREATED);
        List<BotApiMethod<?>> methods = new ArrayList<>(keyboardService.setReplyKeyboardOfUser(message.getChatId(),
                "✅  Заявку прийнято"));
        methods.add(SendMessage.builder()
                .chatId(String.valueOf(superAdmin.getId()))
                .text("<b>Отримана заявка від </b>" + admin.getFullName() + "\n<b>тел.</b>" + admin.getPhone()
                        + "\n<b>ID:</b>" + admin.getId() + "\nна встановлення зв'язку адмін-департамент\nміж користувачем\n"
                        + botUser.getFullName() + "\n<b>тел.</b>" + botUser.getPhone()
                        + "\n<b>ID:</b>" + botUser.getId() +
                        "\nта департаментами:\n" + departments + "\nВстановити зв'язок?")
                .parseMode("HTML")
                .replyMarkup(InlineKeyboardMarkup.builder()
                        .keyboard(keyboardService.getAgreeButtons(dataStartText))
                        .build())
                .build());
        this.departments = new ArrayList<>();
        return methods;
    }

    public List<BotApiMethod<?>> changeRoleRequest(@NotNull Message message) {
        if (botUserService.checkIsAdmin(message.getChatId())) {
            if (message.getText().equals("❌ Скасувати заявку")) {
                chatPropertyModeService.setCurrentStateOfRequest(message.getChatId(), StateOfRequest.REQUEST_CREATED);
                return keyboardService.setReplyKeyboardOfUser(message.getChatId(), "Заявку скасовано");
            }
            switch (chatPropertyModeService.getCurrentStateOfRequest(message.getChatId())) {
                case SET_ROLES -> {
                    return setDepartmentsOfAdmin(message);
                }
                case WAIT_PHONE -> {
                    chatPropertyModeService.setCurrentStateOfRequest(message.getChatId(), StateOfRequest.SET_PHONE);
                    return keyboardService.setMenuReplyKeyboard(message.getChatId(),
                            List.of("❌ Скасувати заявку"),
                            "Введіть номер телефону адміністратора у форматі '+123456789098'");
                }
                case SET_PHONE -> {
                    return setPhoneOfAdmin(message);
                }
            }
            return keyboardService.setMenuReplyKeyboard(message.getChatId(),
                    List.of("❌ Скасувати заявку"),
                    RequestHandler.WRONG_ACTION_TEXT);
        } else {
            chatPropertyModeService.setCurrentStateOfRequest(message.getChatId(), StateOfRequest.REQUEST_CREATED);
            return Collections.singletonList(botUserService.getFalseAdminText(message.getChatId()));
        }
    }

    @NotNull
    private List<BotApiMethod<?>> setPhoneOfAdmin(@NotNull Message message) {
        String phoneNumber = message.getText();
        if (message.getText().startsWith("+") && botUserService.findByPhone(phoneNumber).isPresent()) {
            botUser = botUserService.findByPhone(phoneNumber).get();
            chatPropertyModeService.setCurrentStateOfRequest(message.getChatId(), StateOfRequest.SET_ROLES);
            return setDepartmentsOfAdmin(message);
        } else {
            return keyboardService.setMenuReplyKeyboard(message.getChatId(),
                    List.of("❌ Скасувати заявку"),
                    "Невірний формат телефонного номеру,\nабо такий номер не зареєстрований.\nСпробуйте ще раз");
        }
    }

    @NotNull
    private List<BotApiMethod<?>> setDepartmentsOfAdmin(@NotNull Message message) {
        log.info("PHONE = {}", botUser.getPhone());
        List<BotApiMethod<?>> methods = new ArrayList<>();
        switch (message.getText()) {
            case "❌ Скасувати заявку" -> {
                chatPropertyModeService.setCurrentStateOfRequest(message.getChatId(), StateOfRequest.REQUEST_CREATED);
                methods.addAll(keyboardService.setReplyKeyboardOfUser(message.getChatId(), "Заявку скасовано"));
            }
            case "\uD83D\uDCE8 Відправити заявку" -> methods.addAll(requestRole(message, departments));
            default -> {
                List<String> namesOfButtons = List.of("➕ Додати", "\uD83D\uDCE8 Відправити заявку", "❌ Скасувати заявку");
                methods.addAll(departmentController.getMenuOfDepartments(message));
                methods.addAll(keyboardService.setMenuReplyKeyboard(message.getChatId(), namesOfButtons,
                        "Ви можете додавати Департаменти, поки не натиснуте кнопку 'Відправити заявку'"));
                if (message.getText().equals("➕ Додати")) {
                    departments.add(chatPropertyModeService.getCurrentDepartment(message.getChatId()));
                }
            }
        }
        return methods;
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

    public List<BotApiMethod<?>> sendComplaint(CallbackQuery callbackQuery) {
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
}
