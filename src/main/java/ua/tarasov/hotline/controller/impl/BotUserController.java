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
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import ua.tarasov.hotline.controller.Controller;
import ua.tarasov.hotline.entities.BotUser;
import ua.tarasov.hotline.entities.UserRequest;
import ua.tarasov.hotline.handlers.RequestHandler;
import ua.tarasov.hotline.models.Department;
import ua.tarasov.hotline.models.Role;
import ua.tarasov.hotline.models.StateOfRequest;
import ua.tarasov.hotline.service.BotUserService;
import ua.tarasov.hotline.service.KeyboardService;
import ua.tarasov.hotline.service.impl.BotUserServiceImpl;
import ua.tarasov.hotline.service.impl.KeyboardServiceImpl;

import java.util.*;

import static ua.tarasov.hotline.handlers.RequestHandler.START_TEXT;

@Component
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BotUserController implements Controller {
    final DepartmentController departmentController;
    final BotUserService botUserService;
    final KeyboardService keyboardService;
    BotUser botUser = new BotUser();
    List<Department> departments = new ArrayList<>();

    public BotUserController(DepartmentController departmentController, BotUserServiceImpl botUserService,
                             KeyboardServiceImpl keyboardService) {
        this.departmentController = departmentController;
        this.botUserService = botUserService;
        this.keyboardService = keyboardService;
    }

    @NotNull
    @Unmodifiable
    public List<BotApiMethod<?>> setStartProperties(@NotNull User user) {
        chatPropertyModeService.setCurrentRequest(user.getId(), new UserRequest());
        chatPropertyModeService.setCurrentStateOfRequest(user.getId(), StateOfRequest.REQUEST_CREATED);
        log.info("USER = {}", user);
        botUser.setId(user.getId());
        botUser.setUsername(user.getUserName());
        botUser.setFullName(user.getFirstName() + " " + user.getLastName());
        if (botUserService.findById(botUser.getId()).isPresent()) {
            BotUser currentUser = botUserService.findById(botUser.getId()).get();
            botUser.setRole(currentUser.getRole());
            botUser.setDepartments(currentUser.getDepartments());
            botUser.setWarningCount(currentUser.getWarningCount());
        } else {
            botUser.setWarningCount(0);
            if ((long) botUserService.findAll().size() == 0) {
                botUser.setRole(Role.SUPER_ADMIN);
                botUser.getDepartments().addAll(Arrays.stream(Department.values()).toList());
            } else {
                botUser.setRole(Role.USER);
                botUser.setDepartments(Collections.singleton(Department.USER));
            }
        }
        botUserService.saveBotUser(botUser);
        chatPropertyModeService.setCurrentAdminKeyboardState(user.getId(), botUser.getRole().equals(Role.ADMIN));
        return Collections.singletonList(SendMessage.builder()
                .chatId(String.valueOf(user.getId()))
                .text("Радий Вас вітати, " + botUser.getFullName() +
                        "\n\nЧи бажаєте Ви додати свій телефонний" +
                        "\nномер для майбутнього зв'язку" +
                        "\nз Вами співробітників обслуговуючих" +
                        "\nдепартаментів з метою будь-яких" +
                        "\nуточнень? \uD83D\uDC47")
                .replyMarkup(ReplyKeyboardMarkup.builder()
                        .keyboard(keyboardService.getAgreeAddContactReplyButtons())
                        .resizeKeyboard(true)
                        .build())
                .build());
    }

    public List<BotApiMethod<?>> setBotUserPhone(Message message) {
        List<BotApiMethod<?>> responseMessages = Controller.getSimpleResponseToRequest(message, "Something wrong...");
        if (botUserService.findById(message.getChatId()).isPresent()) {
            botUser = botUserService.findById(message.getChatId()).get();
            String phone = message.getContact().getPhoneNumber();
            if (!phone.startsWith("+")) phone = "+" + phone;
            botUser.setPhone(phone);
            botUserService.saveBotUser(botUser);
            responseMessages = keyboardService.setReplyKeyboardOfUser(message.getChatId(), START_TEXT);
        }
        return responseMessages;
    }

    @NotNull
    @Unmodifiable
    public List<BotApiMethod<?>> setBotUserDepartment(@NotNull CallbackQuery callbackQuery) {
        StringBuilder builder = new StringBuilder();
        BotUser superAdmin = botUserService.findByRole(Role.SUPER_ADMIN);
        String[] depText = jsonConverter.fromJson(callbackQuery.getData().substring("yes-department".length()), String[].class);
        if (botUserService.findById(Long.parseLong(depText[0])).isPresent()) {
            botUser = botUserService.findById(Long.parseLong(depText[0])).get();
        }
        Set<Department> departments = new HashSet<>();
        List<String> departmentsNumbers = Arrays.stream(depText).skip(1).toList();
        departmentsNumbers.forEach(departmentsNumber -> {
            Department department = Department.values()[Integer.parseInt(departmentsNumber)];
            departments.add(department);
        });
        if (departments.isEmpty()) {
            if (botUser.getRole().equals(Role.SUPER_ADMIN)) {
                return List.of(SendMessage.builder()
                        .chatId(String.valueOf(superAdmin.getId()))
                        .text("Неможливо скасувати права суперадміна.\nНеобхідно спочатку призначити іншого суперадміна")
                        .build());
            }
            botUser.setRole(Role.USER);
            departments.add(Department.USER);
            builder.append("скасовано");
        } else {
            if (!botUser.getRole().equals(Role.SUPER_ADMIN)) botUser.setRole(Role.ADMIN);
            builder.append("встановлені для департаментів: ");
            departments.forEach(department -> builder.append("\n").append(department));
        }
        botUser.setDepartments(departments);
        botUserService.saveBotUser(this.botUser);
        return List.of(SendMessage.builder()
                        .chatId(botUser.getId().toString())
                        .text("Ваші права доступу адміністратора\n" + builder)
                        .replyMarkup(ReplyKeyboardMarkup.builder()
                                .keyboard(keyboardService.getAdminReplyButtons())
                                .resizeKeyboard(true)
                                .oneTimeKeyboard(false)
                                .build())
                        .build(),
                SendMessage.builder()
                        .chatId(String.valueOf(superAdmin.getId()))
                        .text("Права доступу адміністратора\n" + botUser.getFullName() + "\n" + builder)
                        .build());
    }

    public List<BotApiMethod<?>> changeBotUserRole(@NotNull Message message) {
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
            case "\uD83E\uDDF9 Очистити" -> {
                departments.clear();
                methods.addAll(Controller.getSimpleResponseToRequest(message, """
                        Дякую,
                        відправлено запит на видалення
                        всіх прав адміністратора"""));
                methods.addAll(requestRole(message, departments));
            }
            case "❌ Скасувати заявку" -> {
                chatPropertyModeService.setCurrentStateOfRequest(message.getChatId(), StateOfRequest.REQUEST_CREATED);
                methods.addAll(keyboardService.setReplyKeyboardOfUser(message.getChatId(), "Заявку скасовано"));
            }
            case "\uD83D\uDCE8 Відправити заявку" -> methods.addAll(requestRole(message, departments));
            default -> {
                List<String> namesOfButtons = List.of("➕ Додати", "\uD83E\uDDF9 Очистити", "\uD83D\uDCE8 Відправити заявку",
                        "❌ Скасувати заявку");
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
}
