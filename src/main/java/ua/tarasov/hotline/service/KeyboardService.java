package ua.tarasov.hotline.service;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ua.tarasov.hotline.models.Department;

import java.util.List;

public interface KeyboardService {
    List<KeyboardRow> getAdminReplyButtons();

    List<KeyboardRow> getUserReplyButtons(Long userId);

    List<KeyboardRow> getAgreeAddContactReplyButtons();

    List<List<InlineKeyboardButton>> getRefuseButton(Message message);

    List<List<InlineKeyboardButton>> getAgreeButtons(String dataStartText);

    List<List<InlineKeyboardButton>> getDepartmentInlineButtons(Department currentDepartment);

    EditMessageReplyMarkup getCorrectReplyMarkup(Message message, List<List<InlineKeyboardButton>> buttons);

    List<List<InlineKeyboardButton>> getStateRequestButton(Integer messageId, String text);

    List<BotApiMethod<?>> setReplyKeyboard(Long userId, String messageText);

    List<BotApiMethod<?>> setChangeMenu(Message message);
    List<List<InlineKeyboardButton>> getContinueButton(Message message);

    List<KeyboardRow> getRequestReplyButtons(Long userId);
    List<BotApiMethod<?>> setRequestReplyKeyboard(Long userId, String messageText);
}
