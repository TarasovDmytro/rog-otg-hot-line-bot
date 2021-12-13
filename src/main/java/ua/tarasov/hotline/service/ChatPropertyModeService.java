package ua.tarasov.hotline.service;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import ua.tarasov.hotline.models.model.BotState;
import ua.tarasov.hotline.models.model.Department;

import java.util.HashMap;
import java.util.Map;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatPropertyModeService {
    static ChatPropertyModeService chatProperties;
    final Map<Long, Department> currentDepartment = new HashMap<>();
    final Map<Long, Boolean> currentAdminKeyboardState = new HashMap<>();
    final Map<Long, BotState> botStateMap = new HashMap<>();

    private ChatPropertyModeService() {
    }

    public static ChatPropertyModeService getChatProperties() {
        if (chatProperties == null) {
            chatProperties = new ChatPropertyModeService();
        }
        return chatProperties;
    }

    public Department getCurrentDepartment(long chatId) {
        return currentDepartment.getOrDefault(chatId, Department.ЖЕУ_ДОКУЧАЄВСЬКЕ);
    }

    public void setCurrentDepartment(long chatId, Department department) {
        currentDepartment.put(chatId, department);
    }

    public Boolean getCurrentAdminKeyboardState(Long chatId) {
        return currentAdminKeyboardState.getOrDefault(chatId, true);
    }

    public void setCurrentAdminKeyboardState(long chatId, boolean adminKeyboardState) {
        currentAdminKeyboardState.put(chatId, adminKeyboardState);
    }

    public void setBotState(long chatId, BotState botState) {
        botStateMap.put(chatId, botState);
    }

    public BotState getBotState(long chatId) {
        return botStateMap.getOrDefault(chatId, BotState.WAIT_BUTTON);
    }
}
