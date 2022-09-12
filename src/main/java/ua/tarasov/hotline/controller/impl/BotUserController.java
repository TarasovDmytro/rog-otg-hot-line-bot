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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import ua.tarasov.hotline.controller.Controller;
import ua.tarasov.hotline.entities.BotUser;
import ua.tarasov.hotline.models.BotState;
import ua.tarasov.hotline.models.Department;
import ua.tarasov.hotline.models.Role;
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
    final BotUserService botUserService;
    final KeyboardService keyboardService;
    BotUser botUser = new BotUser();

    public BotUserController(BotUserServiceImpl botUserService, KeyboardServiceImpl keyboardService) {
        this.botUserService = botUserService;
        this.keyboardService = keyboardService;
    }

    @NotNull
    @Unmodifiable
    public List<BotApiMethod<?>> setStartProperties(@NotNull User user) {
        chatPropertyModeService.setCurrentBotState(user.getId(), BotState.WAIT_BUTTON);
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
}
