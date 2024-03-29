package ua.tarasov.hotline.controller.impl;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ua.tarasov.hotline.controller.Controller;
import ua.tarasov.hotline.models.Department;
import ua.tarasov.hotline.service.KeyboardService;
import ua.tarasov.hotline.service.impl.KeyboardServiceImpl;

import java.util.Collections;
import java.util.List;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DepartmentController implements Controller {
    final KeyboardService keyboardService;

    public DepartmentController(KeyboardServiceImpl keyboardService) {
        this.keyboardService = keyboardService;
    }

    @Contract("_ -> new")
    @NotNull
    @Unmodifiable
    public List<BotApiMethod<?>> getMenuOfDepartments(@NotNull Message message) {
        Department currentDepartment = chatPropertyModeService.getCurrentDepartment(message.getChatId());
        return Collections.singletonList(SendMessage.builder()
                .chatId(message.getChatId().toString())
                .text("Оберіть, будь ласка, обслуговуючий департамент\n(Ваш вибір помічений галочкою)")
                .replyMarkup(InlineKeyboardMarkup.builder()
                        .keyboard(keyboardService.getDepartmentInlineButtons(currentDepartment))
                        .build())
                .build());
    }

    public List<BotApiMethod<?>> setDepartment(@NotNull CallbackQuery callbackQuery) {
        Message message = callbackQuery.getMessage();
        Department department = jsonConverter.fromJson(callbackQuery
                .getData().substring("department".length()), Department.class);
        chatPropertyModeService.setCurrentDepartment(message.getChatId(), department);
        String textMessage = "Департамент обрано";
        return List.of(
                keyboardService.getCorrectReplyMarkup(message, keyboardService.getDepartmentInlineButtons(department)),
                SendMessage.builder()
                        .chatId(String.valueOf(message.getChatId()))
                        .text(textMessage)
                        .build());
    }
}
