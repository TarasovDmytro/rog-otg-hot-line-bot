package ua.tarasov.hotline.controller.impl;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
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
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BotUserController implements Controller {
    final BotUserService botUserService;
    final KeyboardService keyboardService;
    BotUser botUser = new BotUser();

    public BotUserController(BotUserServiceImpl botUserService, KeyboardServiceImpl keyboardService) {
        this.botUserService = botUserService;
        this.keyboardService = keyboardService;
    }

    @NotNull @Unmodifiable
    public List<PartialBotApiMethod<?>> setStartProperties(@NotNull Message message) {
        chatPropertyModeService.setCurrentBotState(message.getChatId(), BotState.WAIT_BUTTON);
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

    public List<PartialBotApiMethod<?>> setBotUserPhone(Message message) {
        List<PartialBotApiMethod<?>> responseMessages = Controller.getSimpleResponseToRequest(message, "Something wrong...");
        if (botUserService.findById(message.getChatId()).isPresent()) {
            botUser = botUserService.findById(message.getChatId()).get();
            String phone = message.getContact().getPhoneNumber();
            if (!phone.startsWith("+")) phone = "+" + phone;
            botUser.setPhone(phone);
            botUserService.saveBotUser(botUser);
            responseMessages = keyboardService.setReplyKeyboard(message.getChatId(), START_TEXT);
        }
        return responseMessages;
    }

    @NotNull @Unmodifiable
    public List<PartialBotApiMethod<?>> setBotUserDepartment(@NotNull CallbackQuery callbackQuery) {
        String[] depText = jsonConverter.fromJson(callbackQuery.getData().substring("yes-department".length()), String[].class);
        if (botUserService.findById(Long.parseLong(depText[0])).isPresent()) {
            botUser = botUserService.findById(Long.parseLong(depText[0])).get();
        }
        Set<Department> departments = new HashSet<>();
        List<String> departmentsNumber = Arrays.stream(depText).skip(1).toList();
        for (String s : departmentsNumber) {
            Department department = Department.values()[Integer.parseInt(s) - 1];
            departments.add(department);
        }
        botUser.setDepartments(departments);
        if (!botUser.getRole().equals(Role.SUPER_ADMIN)) botUser.setRole(Role.ADMIN);
        botUserService.saveBotUser(this.botUser);
        BotUser superAdmin = botUserService.findByRole(Role.SUPER_ADMIN);
        StringBuilder builder = new StringBuilder("встановлені для департаментів: ");
        botUser.getDepartments().forEach(department -> builder.append("\n").append(department));
        return List.of(SendMessage.builder()
                        .chatId(botUser.getId().toString())
                        .text("Ваші права доступу адміністратора " + builder)
                        .replyMarkup(ReplyKeyboardMarkup.builder()
                                .keyboard(keyboardService.getAdminReplyButtons())
                                .resizeKeyboard(true)
                                .oneTimeKeyboard(false)
                                .build())
                        .build(),
                SendMessage.builder()
                        .chatId(String.valueOf(superAdmin.getId()))
                        .text("Права доступу адміністратора " + botUser.getFullName() + " " + builder)
                        .build());
    }
}
