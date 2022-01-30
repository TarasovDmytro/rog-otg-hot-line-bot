package ua.tarasov.hotline.service.impl;

import com.google.gson.Gson;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ua.tarasov.hotline.models.Department;
import ua.tarasov.hotline.service.KeyboardService;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KeyboardServiceImpl implements KeyboardService {
    final CheckRoleServiceImpl checkRoleServiceImpl;
    final Gson jsonConverter = new Gson();

    public KeyboardServiceImpl(CheckRoleServiceImpl checkRoleServiceImpl) {
        this.checkRoleServiceImpl = checkRoleServiceImpl;
    }

    @Override
    public List<KeyboardRow> getAdminReplyButtons() {
        var firstRow = new KeyboardRow();
        firstRow.add("Повідомлення всім");
        firstRow.add("Всі заявки");
        firstRow.add("Не виконані заявки");
        var secondRow = new KeyboardRow();
        secondRow.add("Останні оголошення");
        secondRow.add("Змінити меню");
        return List.of(firstRow, secondRow);
    }

    @Override
    public List<KeyboardRow> getUserReplyButtons(Message message) {
        var firstRow = new KeyboardRow();
        firstRow.add("Зробити заявку");
        firstRow.add("Мої заявки");
        firstRow.add("Мої не виконані заявки");
        var secondRow = new KeyboardRow();
        secondRow.add("Останні оголошення");
        if (checkRoleServiceImpl.checkIsAdmin(message)) {
            secondRow.add("Змінити меню");
        }
        return List.of(firstRow, secondRow);
    }

    @Override
    public List<KeyboardRow> getAgreeAddContactReplyButtons() {
        return List.of(new KeyboardRow(List.of(
                KeyboardButton.builder()
                        .text("❌ Відмовитись")
                        .build(),
                KeyboardButton.builder()
                        .requestContact(true)
                        .text("✅  Додати контакт")
                        .build())));
    }

    @Override
    public List<List<InlineKeyboardButton>> getRefuseButton(@NotNull Message message) {
        Integer messageId = message.getMessageId();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        buttons.add(List.of(
                InlineKeyboardButton.builder()
                        .text("Відмовитись")
                        .callbackData("refuse" + jsonConverter.toJson(messageId))
                        .build()));
        return buttons;
    }

    @Override
    public List<List<InlineKeyboardButton>> getAgreeButtons(String dataStartText) {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        buttons.add(List.of(
                InlineKeyboardButton.builder()
                        .text("✅  Так")
                        .callbackData("yes-" + dataStartText)
                        .build(),
                InlineKeyboardButton.builder()
                        .text("❌ Ні")
                        .callbackData("no-" + dataStartText)
                        .build()));
        return buttons;
    }

    @Override
    public List<List<InlineKeyboardButton>> getDepartmentInlineButtons(Department currentDepartment) {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        for (Department department : Department.values()) {
            if (!department.equals(Department.USER)) {
                buttons.add(List.of(
                        InlineKeyboardButton.builder()
                                .text(getDepartmentName(currentDepartment, department))
                                .callbackData("department" + jsonConverter.toJson(department))
                                .build()));
            }
        }
        return buttons;
    }

    private String getDepartmentName(Department currentDepartment, Department department) {
        return currentDepartment == department ? currentDepartment + "✅" : department.toString();
    }

    @Override
    public EditMessageReplyMarkup getCorrectReplyMarkup(@NotNull Message message, List<List<InlineKeyboardButton>> buttons) {
        return EditMessageReplyMarkup.builder()
                .chatId(message.getChatId().toString())
                .messageId(message.getMessageId())
                .replyMarkup(InlineKeyboardMarkup.builder()
                        .keyboard(buttons)
                        .build())
                .build();
    }

    @Override
    public List<List<InlineKeyboardButton>> getStateRequestButton(Integer messageId, String text) {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        buttons.add(List.of(
                InlineKeyboardButton.builder()
                        .text("❌ Відмовити")
                        .callbackData("refuse_request" + jsonConverter.toJson(messageId))
                        .build(),
                InlineKeyboardButton.builder()
                        .text(text)
                        .callbackData("message_id" + jsonConverter.toJson(messageId))
                        .build()));
        buttons.add(List.of(
                InlineKeyboardButton.builder()
                        .text("Отримати контакт")
                        .callbackData("contact" + jsonConverter.toJson(messageId))
                        .build()));
        buttons.add(List.of(
                InlineKeyboardButton.builder()
                        .text("Отримати локацію")
                        .callbackData("location" + jsonConverter.toJson(messageId))
                        .build()));
        return buttons;
    }
}