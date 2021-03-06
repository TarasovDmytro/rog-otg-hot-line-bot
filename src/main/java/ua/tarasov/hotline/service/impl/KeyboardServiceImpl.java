package ua.tarasov.hotline.service.impl;

import com.google.gson.Gson;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ua.tarasov.hotline.models.Department;
import ua.tarasov.hotline.service.ChatPropertyModeService;
import ua.tarasov.hotline.service.CheckRoleService;
import ua.tarasov.hotline.service.KeyboardService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KeyboardServiceImpl implements KeyboardService {
    final CheckRoleServiceImpl checkRoleServiceImpl;
    final ChatPropertyModeService chatPropertyModeService;
    final CheckRoleService checkRoleService;

    final Gson jsonConverter = new Gson();

    public KeyboardServiceImpl(CheckRoleServiceImpl checkRoleServiceImpl,
                               @Qualifier("getChatProperties") ChatPropertyModeServiceImpl chatPropertyModeService, CheckRoleService checkRoleService) {
        this.checkRoleServiceImpl = checkRoleServiceImpl;
        this.chatPropertyModeService = chatPropertyModeService;
        this.checkRoleService = checkRoleService;
    }

    @Override
    public List<KeyboardRow> getAdminReplyButtons() {
        var firstRow = new KeyboardRow();
        firstRow.add("???????????????????????? ????????");
        firstRow.add("?????? ????????????");
        firstRow.add("???? ???????????????? ????????????");
        var secondRow = new KeyboardRow();
        secondRow.add("?????????????? ????????????????????");
        secondRow.add("?????????????? ????????");
        return List.of(firstRow, secondRow);
    }

    @Override
    public List<KeyboardRow> getUserReplyButtons(Long userId) {
        var firstRow = new KeyboardRow();
        firstRow.add("?????????????? ????????????");
        firstRow.add("?????? ????????????");
        firstRow.add("?????? ???? ???????????????? ????????????");
        var secondRow = new KeyboardRow();
        secondRow.add("?????????????? ????????????????????");
        if (checkRoleServiceImpl.checkIsAdmin(userId)) {
            secondRow.add("?????????????? ????????");
        }
        return List.of(firstRow, secondRow);
    }

    @Override
    public List<KeyboardRow> getAgreeAddContactReplyButtons() {
        return List.of(new KeyboardRow(List.of(
                KeyboardButton.builder()
                        .text("??? ??????????????????????")
                        .build(),
                KeyboardButton.builder()
                        .requestContact(true)
                        .text("???  ???????????? ??????????????")
                        .build())));
    }

    @Override
    public List<List<InlineKeyboardButton>> getRefuseButton(@NotNull Message message) {
        Integer messageId = message.getMessageId();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        buttons.add(List.of(
                InlineKeyboardButton.builder()
                        .text("??????????????????????")
                        .callbackData("refuse" + jsonConverter.toJson(messageId))
                        .build()));
        return buttons;
    }

    @Override
    public List<List<InlineKeyboardButton>> getAgreeButtons(String dataStartText) {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        buttons.add(List.of(
                InlineKeyboardButton.builder()
                        .text("???  ??????")
                        .callbackData("yes-" + dataStartText)
                        .build(),
                InlineKeyboardButton.builder()
                        .text("??? ????")
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
        return currentDepartment == department ? currentDepartment + "???" : department.toString();
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
                        .text("??? ??????????????????")
                        .callbackData("refuse_request" + jsonConverter.toJson(messageId))
                        .build(),
                InlineKeyboardButton.builder()
                        .text(text)
                        .callbackData("message_id" + jsonConverter.toJson(messageId))
                        .build()));
        buttons.add(List.of(
                InlineKeyboardButton.builder()
                        .text("???????????????? ??????????????")
                        .callbackData("contact" + jsonConverter.toJson(messageId))
                        .build()));
        buttons.add(List.of(
                InlineKeyboardButton.builder()
                        .text("???????????????? ??????????????")
                        .callbackData("location" + jsonConverter.toJson(messageId))
                        .build()));
        return buttons;
    }

    public List<BotApiMethod<?>> setReplyKeyboardOfUser(Long userId, String messageText) {
        List<KeyboardRow> keyboardRows = chatPropertyModeService.getCurrentAdminKeyboardState(userId) ?
                getAdminReplyButtons() : getUserReplyButtons(userId);
        return Collections.singletonList(SendMessage.builder()
                .chatId(String.valueOf(userId))
                .text(messageText)
                .replyMarkup(ReplyKeyboardMarkup.builder()
                        .keyboard(keyboardRows)
                        .resizeKeyboard(true)
                        .oneTimeKeyboard(false)
                        .build())
                .build());
    }

    public List<BotApiMethod<?>> setChangeMenu(Message message) {
        if (checkRoleService.checkIsAdmin(message.getChatId())) {
            boolean state = chatPropertyModeService.getCurrentAdminKeyboardState(message.getChatId());
            chatPropertyModeService.setCurrentAdminKeyboardState(message.getChatId(), !state);
            String messageText = "???????? ??????????????, ?????????????????? ????????????????????????";
            return setReplyKeyboardOfUser(message.getChatId(), messageText);
        }
        return List.of(checkRoleService.getFalseAdminText(message.getChatId()));
    }

    @Override
    public List<KeyboardRow> getRequestReplyButtons(Long userId, String nameOfButton) {
        var firstRow = new KeyboardRow();
        firstRow.add(nameOfButton);
        var secondRow = new KeyboardRow();
        secondRow.add("?????????????????? ????????????");
        return List.of(firstRow, secondRow);
    }

    public List<KeyboardRow> getRoleReplyButtons(String nameOfButton) {
        var firstRow = new KeyboardRow();
        firstRow.add(nameOfButton);
        var secondRow = new KeyboardRow();
        secondRow.add("?????????????????? ????????????");
        var thirdRow = new KeyboardRow();
        thirdRow.add("????????????");
        return List.of(firstRow, secondRow, thirdRow);
    }

    @Override
    public List<BotApiMethod<?>> setRequestReplyKeyboard(Long userId, String nameOfButton, String messageText) {
        List<KeyboardRow> keyboardRows = getRequestReplyButtons(userId, nameOfButton);
        return Collections.singletonList(SendMessage.builder()
                .chatId(String.valueOf(userId))
                .text(messageText)
                .replyMarkup(ReplyKeyboardMarkup.builder()
                        .keyboard(keyboardRows)
                        .resizeKeyboard(true)
                        .oneTimeKeyboard(false)
                        .build())
                .build());
    }

    public List<BotApiMethod<?>> setRoleReplyKeyboard(Long userId, String nameOfButton, String messageText) {
        List<KeyboardRow> keyboardRows = getRoleReplyButtons(nameOfButton);
        return Collections.singletonList(SendMessage.builder()
                .chatId(String.valueOf(userId))
                .text(messageText)
                .replyMarkup(ReplyKeyboardMarkup.builder()
                        .keyboard(keyboardRows)
                        .resizeKeyboard(true)
                        .oneTimeKeyboard(false)
                        .build())
                .build());
    }
}
