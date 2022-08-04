package ua.tarasov.hotline.controller.impl;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ua.tarasov.hotline.controller.Controller;
import ua.tarasov.hotline.entities.BotUser;
import ua.tarasov.hotline.handlers.RequestHandler;
import ua.tarasov.hotline.models.Department;
import ua.tarasov.hotline.models.Role;
import ua.tarasov.hotline.models.StateOfRequest;
import ua.tarasov.hotline.service.BotUserService;
import ua.tarasov.hotline.service.CheckRoleService;
import ua.tarasov.hotline.service.KeyboardService;
import ua.tarasov.hotline.service.impl.BotUserServiceImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SuperAdminController implements Controller {
    final BotUserService botUserService;
    final KeyboardService keyboardService;
    final CheckRoleService checkRoleService;
    final DepartmentController departmentController;
    List<Department> departments = new ArrayList<>();
    BotUser botUser = new BotUser();

    public SuperAdminController(BotUserServiceImpl botUserService, KeyboardService keyboardService, CheckRoleService checkRoleService, DepartmentController departmentController) {
        this.botUserService = botUserService;
        this.keyboardService = keyboardService;
        this.checkRoleService = checkRoleService;
        this.departmentController = departmentController;
    }

//    @NotNull
//    @Unmodifiable
//    public List<BotApiMethod<?>> requestAdminRole(@NotNull Message message) {
//        BotUser admin = new BotUser();
//        if (botUserService.findById(message.getChatId()).isPresent()) {
//            admin = botUserService.findById(message.getChatId()).get();
//        }
//        List<String> depText = new ArrayList<>();
//        depText.add(message.getChatId().toString());
//        depText.addAll(Arrays.stream(message.getText().substring("*admin*".length()).split(":")).toList());
//        String dataStartText = "department" + jsonConverter.toJson(depText);
//        BotUser superAdmin = botUserService.findByRole(Role.SUPER_ADMIN);
//        return List.of(SendMessage.builder()
//                .chatId(String.valueOf(superAdmin.getId()))
//                .text("<b>Отримана заявка від </b>" + admin.getFullName() + "\n<b>тел.</b>" + admin.getPhone()
//                        + "\n<b>ID:</b>" + admin.getId() + "\nна встановлення зв'язку адмін-департамент\nміж користувачем"
//                        +
//                        "\nдепартаменти:" + depText.stream().skip(1).toList() + "\nВстановити зв'язок?")
//                .parseMode("HTML")
//                .replyMarkup(InlineKeyboardMarkup.builder()
//                        .keyboard(keyboardService.getAgreeButtons(dataStartText))
//                        .build())
//                .build());
//    }

    private List<BotApiMethod<?>> requestRole(@NotNull Message message, List<Department> departments) {
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
                "Заявку прийнято"));
        methods.add(SendMessage.builder()
                .chatId(String.valueOf(superAdmin.getId()))
                .text("<b>Отримана заявка від </b>" + admin.getFullName() + "\n<b>тел.</b>" + admin.getPhone()
                        + "\n<b>ID:</b>" + admin.getId() + "\nна встановлення зв'язку адмін-департамент\nміж користувачем"
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

//    public List<BotApiMethod<?>> handelRequestAdminRole(Message message) {
//        BotUser superAdmin = botUserService.findByRole(Role.SUPER_ADMIN);
//        if (message.getChatId().equals(superAdmin.getId())) {
//            List<String> messageData = new ArrayList<>(Arrays.stream(message.getText().substring("*set*".length())
//                    .split(":")).toList());
//            String userPhone = messageData.get(0);
//            if (botUserService.findByPhone(userPhone).isPresent()) {
//                botUser = botUserService.findByPhone(userPhone).get();
//            } else
//                return Controller.getSimpleResponseToRequest(message, "Користувача з телефонним номером: " + userPhone +
//                        " не існує");
//            Set<Department> departments = new HashSet<>();
//            List<String> departmentsNumber = messageData.stream().skip(1).toList();
//            for (String s : departmentsNumber) {
//                if (Integer.parseInt(s) > 0) {
//                    Department department = Department.values()[Integer.parseInt(s) - 1];
//                    departments.add(department);
//                }
//            }
//            botUser.setDepartments(departments);
//            if (!botUser.getRole().equals(Role.SUPER_ADMIN)) {
//                botUser.setRole(Role.ADMIN);
//                chatPropertyModeService.setCurrentAdminKeyboardState(botUser.getId(), true);
//            }
//            if (departments.size() == 1 && departments.contains(Department.USER)) {
//                botUser.setRole(Role.USER);
//                chatPropertyModeService.setCurrentAdminKeyboardState(botUser.getId(), false);
//            }
//            botUserService.saveBotUser(botUser);
//            if (checkRoleService.checkIsAdmin(botUser.getId())) {
//                StringBuilder builder = new StringBuilder("встановлені для департаментів: ");
//                botUser.getDepartments().forEach(department -> builder.append("\n").append(department));
//                return List.of(keyboardService.setReplyKeyboardOfUser(botUser.getId(), "Ваші права доступу адміністратора " + builder).get(0),
//                        SendMessage.builder()
//                                .chatId(String.valueOf(superAdmin.getId()))
//                                .text("Права доступу адміністратора " + botUser.getFullName() + " " + builder)
//                                .build());
//            } else {
//                return List.of(keyboardService.setReplyKeyboardOfUser(botUser.getId(), "Ваші права доступу адміністратора анульовані").get(0),
//                        SendMessage.builder()
//                                .chatId(String.valueOf(superAdmin.getId()))
//                                .text("Права доступу адміністратора для " + botUser.getFullName() + " анульовані")
//                                .build());
//            }
//        } else return Controller.getSimpleResponseToRequest(message, "You do not have enough access rights");
//    }

    public List<BotApiMethod<?>> changeRoleRequest(@NotNull Message message) {
        if (message.getText().equals("Скасувати заявку")) {
            chatPropertyModeService.setCurrentStateOfRequest(message.getChatId(), StateOfRequest.REQUEST_CREATED);
            return keyboardService.setReplyKeyboardOfUser(message.getChatId(), "Заявку скасовано");
        }
        if (chatPropertyModeService.getStateOfRequest(message.getChatId()).equals(StateOfRequest.SET_ROLES)) {
            return setDepartmentsOfAdmin(message);
        }
        if (chatPropertyModeService.getStateOfRequest(message.getChatId()).equals(StateOfRequest.WAIT_PHONE)) {
            chatPropertyModeService.setCurrentStateOfRequest(message.getChatId(), StateOfRequest.SET_PHONE);
            return keyboardService.setRoleReplyKeyboard(message.getChatId(),
                    List.of("Скасувати заявку"),
                    "Введіть номер телефону адміністратора у форматі '+123456789098'");
        }
        if (chatPropertyModeService.getStateOfRequest(message.getChatId()).equals(StateOfRequest.SET_PHONE)) {
            return setPhoneOfAdmin(message);
        }

        return keyboardService.setRoleReplyKeyboard(message.getChatId(),
                List.of("Скасувати заявку"),
                RequestHandler.WRONG_ACTION_TEXT);
    }

    @NotNull
    private List<BotApiMethod<?>> setPhoneOfAdmin(@NotNull Message message) {
        String phoneNumber = message.getText();
        if (message.getText().startsWith("+") && botUserService.findByPhone(phoneNumber).isPresent()) {
            botUser = botUserService.findByPhone(phoneNumber).get();
            chatPropertyModeService.setCurrentStateOfRequest(message.getChatId(), StateOfRequest.SET_ROLES);
            return setDepartmentsOfAdmin(message);
        } else {
            return keyboardService.setRoleReplyKeyboard(message.getChatId(),
                    List.of("Скасувати заявку"),
                    "Невірний формат телефонного номеру,\nабо такий номер не зареєстрований.\nСпробуйте ще раз");
        }

    }

    @NotNull
    private List<BotApiMethod<?>> setDepartmentsOfAdmin(@NotNull Message message) {
        log.info("PHONE = {}", botUser.getPhone());
        List<BotApiMethod<?>> methods = new ArrayList<>();
        switch (message.getText()) {
            case "Скасувати заявку" -> {
                chatPropertyModeService.setCurrentStateOfRequest(message.getChatId(), StateOfRequest.REQUEST_CREATED);
                methods.addAll(keyboardService.setReplyKeyboardOfUser(message.getChatId(), "Заявку скасовано"));
            }
            case "Відправити заявку" -> methods.addAll(requestRole(message, departments));
            default -> {
                List<String> namesOfButtons = List.of("Додати", "Відправити заявку", "Скасувати заявку");
                methods.addAll(departmentController.getMenuOfDepartments(message));
                methods.addAll(keyboardService.setRoleReplyKeyboard(message.getChatId(), namesOfButtons,
                        "Ви можете додавати Департаменти, поки не натисните кнопку 'Відправити заявку'"));
                if (message.getText().equals("Додати")) {
                    departments.add(chatPropertyModeService.getCurrentDepartment(message.getChatId()));
                }
            }
        }
        return methods;
    }
}
