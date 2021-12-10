package ua.tarasov.hotline.service;

import com.google.gson.Gson;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ua.tarasov.hotline.models.model.Departments;

import java.util.ArrayList;
import java.util.List;

@Service
public class KeyboardService {
    private final ChatPropertyModeService chatPropertyModeService = ChatPropertyModeService.getChatProperties();
    private final AdminService adminService;
    private final Gson jsonConverter = new Gson();

    public KeyboardService(AdminService adminService) {
        this.adminService = adminService;
    }

    public List<KeyboardRow> getAdminsReplyButtons() {
        var firstRow = new KeyboardRow();
        firstRow.add("Повідомлення всім");
        firstRow.add("Всі заявки");
        firstRow.add("Не виконані заявки");
        var secondRow = new KeyboardRow();
        secondRow.add("змінити меню");
        return List.of(firstRow, secondRow);
    }

    public List<KeyboardRow> getUserReplyButtons(Message message) {
        var firstRow = new KeyboardRow();
        firstRow.add("Зробити заявку");
        firstRow.add("Мої заявки");
        firstRow.add("Мої не виконані заявки");
        var secondRow = new KeyboardRow();
        if (adminService.checkIsAdmin(message)) {
            secondRow.add("змінити меню");
        }
        return List.of(firstRow, secondRow);
    }

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

    public List<List<InlineKeyboardButton>> getRefuseButton(Message message) {
        Integer messageId = message.getMessageId();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        buttons.add(List.of(
                InlineKeyboardButton.builder()
                        .text("Відмовитись")
                        .callbackData("refuse" + jsonConverter.toJson(messageId))
                        .build()));
        return buttons;
    }

    public List<List<InlineKeyboardButton>> getAgreeButtons(Message message, String dataStartText) {
        Integer messageId = message.getMessageId();
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

    public List<List<InlineKeyboardButton>> getDepartmentInlineButtons(Message message) {

        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        Departments currentDepartment = chatPropertyModeService.getCurrentDepartment(message.getChatId());
        for (Departments department : Departments.values()) {
            if (!department.equals(Departments.USER)) {
                buttons.add(List.of(
                        InlineKeyboardButton.builder()
                                .text(getDepartmentName(currentDepartment, department))
                                .callbackData("department" + jsonConverter.toJson(department))
                                .build()));
            }
        }
        return buttons;
    }

    private String getDepartmentName(Departments currentDepartment, Departments department) {
        return currentDepartment == department ? currentDepartment + "✅" : department.name();
    }

    public EditMessageReplyMarkup getCorrectReplyMarkup(Message message, List<List<InlineKeyboardButton>> buttons) {
        return EditMessageReplyMarkup.builder()
                .chatId(message.getChatId().toString())
                .messageId(message.getMessageId())
                .replyMarkup(InlineKeyboardMarkup.builder()
                        .keyboard(buttons)
                        .build())
                .build();
    }

    public List<List<InlineKeyboardButton>> getStateRequestButton(Integer messageId, String text) {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        buttons.add(List.of(
                InlineKeyboardButton.builder()
                        .text("Отримати контакт")
                        .callbackData("contact" + jsonConverter.toJson(messageId))
                        .build(),
                InlineKeyboardButton.builder()
                        .text(text)
                        .callbackData("message_id" + jsonConverter.toJson(messageId))
                        .build()));
        buttons.add(List.of(
                InlineKeyboardButton.builder()
                        .text("Отримати локацію")
                        .callbackData("location" + jsonConverter.toJson(messageId))
                        .build()));
        return buttons;
    }
}
