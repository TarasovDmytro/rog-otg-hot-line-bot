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
import ua.tarasov.hotline.models.Department;
import ua.tarasov.hotline.models.Role;
import ua.tarasov.hotline.models.StateOfRequest;
import ua.tarasov.hotline.service.BotUserService;
import ua.tarasov.hotline.service.CheckRoleService;
import ua.tarasov.hotline.service.KeyboardService;
import ua.tarasov.hotline.service.impl.BotUserServiceImpl;

import java.util.*;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SuperAdminController implements Controller {
    final BotUserService botUserService;
    final KeyboardService keyboardService;
    final CheckRoleService checkRoleService;
    final List <Department> departments = new ArrayList<>();


    BotUser botUser = new BotUser();
    final DepartmentController departmentController;

    public SuperAdminController(BotUserServiceImpl botUserService, KeyboardService keyboardService, CheckRoleService checkRoleService, DepartmentController departmentController) {
        this.botUserService = botUserService;
        this.keyboardService = keyboardService;
        this.checkRoleService = checkRoleService;
        this.departmentController = departmentController;
    }

    @NotNull
    @Unmodifiable
    public List<BotApiMethod<?>> requestAdminRole(@NotNull Message message) {
        if (botUserService.findById(message.getChatId()).isPresent()) {
            botUser = botUserService.findById(message.getChatId()).get();
        }
        List<String> depText = new ArrayList<>();
        depText.add(message.getChatId().toString());
        depText.addAll(Arrays.stream(message.getText().substring("*admin*".length()).split(":")).toList());
        String dataStartText = "department" + jsonConverter.toJson(depText);
        BotUser superAdmin = botUserService.findByRole(Role.SUPER_ADMIN);
        return List.of(SendMessage.builder()
                .chatId(String.valueOf(superAdmin.getId()))
                .text("<b>Отримана заявка від </b>" + botUser.getFullName() + "\n<b>тел.</b>" + botUser.getPhone()
                        + "\n<b>ID:</b>" + botUser.getId() + "\nна встановлення зв'язку адмін-департамент" +
                        "\nдепартаменти:" + depText.stream().skip(1).toList() + "\nВстановити зв'язок?")
                .parseMode("HTML")
                .replyMarkup(InlineKeyboardMarkup.builder()
                        .keyboard(keyboardService.getAgreeButtons(dataStartText))
                        .build())
                .build());
    }

    public List<BotApiMethod<?>> requestRole(@NotNull Message message, List<Department> departments) {
        if (botUserService.findById(message.getChatId()).isPresent()) {
            botUser = botUserService.findById(message.getChatId()).get();
        }
        List<String> depText = new ArrayList<>();
        depText.add(message.getChatId().toString());
        departments.forEach(department -> depText.add(String.valueOf((department.ordinal() + 1))));
        String dataStartText = "department" + jsonConverter.toJson(depText);
        BotUser superAdmin = botUserService.findByRole(Role.SUPER_ADMIN);
        chatPropertyModeService.setCurrentStateOfRequest(message.getChatId(), StateOfRequest.REQUEST_CREATED);
        List<BotApiMethod<?>> methods = new ArrayList<>(keyboardService.setReplyKeyboardOfUser(message.getChatId(),
                "Заявку прийнято"));
        methods.add(SendMessage.builder()
                .chatId(String.valueOf(superAdmin.getId()))
                .text("<b>Отримана заявка від </b>" + botUser.getFullName() + "\n<b>тел.</b>" + botUser.getPhone()
                        + "\n<b>ID:</b>" + botUser.getId() + "\nна встановлення зв'язку адмін-департамент" +
                        "\nдепартаменти:" + departments + "\nВстановити зв'язок?")
                .parseMode("HTML")
                .replyMarkup(InlineKeyboardMarkup.builder()
                        .keyboard(keyboardService.getAgreeButtons(dataStartText))
                        .build())
                .build());
        return methods;
    }

    public List<BotApiMethod<?>> handelRequestAdminRole(Message message) {
        BotUser superAdmin = botUserService.findByRole(Role.SUPER_ADMIN);
        if (message.getChatId().equals(superAdmin.getId())) {
            List<String> messageData = new ArrayList<>(Arrays.stream(message.getText().substring("*set*".length())
                    .split(":")).toList());
            String userPhone = messageData.get(0);
            if (botUserService.findByPhone(userPhone).isPresent()) {
                botUser = botUserService.findByPhone(userPhone).get();
            } else
                return Controller.getSimpleResponseToRequest(message, "Користувача з телефонним номером: " + userPhone +
                        " не існує");
            Set<Department> departments = new HashSet<>();
            List<String> departmentsNumber = messageData.stream().skip(1).toList();
            for (String s : departmentsNumber) {
                if (Integer.parseInt(s) > 0) {
                    Department department = Department.values()[Integer.parseInt(s) - 1];
                    departments.add(department);
                }
            }
            botUser.setDepartments(departments);
            if (!botUser.getRole().equals(Role.SUPER_ADMIN)) {
                botUser.setRole(Role.ADMIN);
                chatPropertyModeService.setCurrentAdminKeyboardState(botUser.getId(), true);
            }
            if (departments.size() == 1 && departments.contains(Department.USER)) {
                botUser.setRole(Role.USER);
                chatPropertyModeService.setCurrentAdminKeyboardState(botUser.getId(), false);
            }
            botUserService.saveBotUser(botUser);
            if (checkRoleService.checkIsAdmin(botUser.getId())) {
                StringBuilder builder = new StringBuilder("встановлені для департаментів: ");
                botUser.getDepartments().forEach(department -> builder.append("\n").append(department));
                return List.of(keyboardService.setReplyKeyboardOfUser(botUser.getId(), "Ваші права доступу адміністратора " + builder).get(0),
                        SendMessage.builder()
                                .chatId(String.valueOf(superAdmin.getId()))
                                .text("Права доступу адміністратора " + botUser.getFullName() + " " + builder)
                                .build());
            } else {
                return List.of(keyboardService.setReplyKeyboardOfUser(botUser.getId(), "Ваші права доступу адміністратора анульовані").get(0),
                        SendMessage.builder()
                                .chatId(String.valueOf(superAdmin.getId()))
                                .text("Права доступу адміністратора для " + botUser.getFullName() + " анульовані")
                                .build());
            }
        } else return Controller.getSimpleResponseToRequest(message, "You do not have enough access rights");
    }

    public List<BotApiMethod<?>> changeRoleRequest(@NotNull Message message){
        if (message.getText().equals("Скасувати заявку")){
            chatPropertyModeService.setCurrentStateOfRequest(message.getChatId(), StateOfRequest.REQUEST_CREATED);
            return keyboardService.setReplyKeyboardOfUser(message.getChatId(), "Заявку скасовано");
        }
        if (!message.getText().equals("Відправити заявку")){
            List<BotApiMethod<?>> methods = new ArrayList<>();
            methods.addAll(departmentController.getMenuOfDepartments(message));
            methods.addAll(keyboardService.setRequestReplyKeyboard(message.getChatId(), "Відправити заявку",
                    "Ви можете додавати Департаменти, поки не натисните кнопку 'Відправити заявку'"));
            departments.add(chatPropertyModeService.getCurrentDepartment(message.getChatId()));
            return methods;
        } else {
            return requestRole(message, departments);
        }
    }
}
