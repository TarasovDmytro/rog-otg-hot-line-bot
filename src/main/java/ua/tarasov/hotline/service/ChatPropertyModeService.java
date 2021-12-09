package ua.tarasov.hotline.service;

import org.springframework.stereotype.Service;
import ua.tarasov.hotline.models.model.BotState;
import ua.tarasov.hotline.models.model.Departments;

import java.util.HashMap;
import java.util.Map;

@Service
public class ChatPropertyModeService {
    private static ChatPropertyModeService chatProperties;
    private final Map<Long, Departments> currentDepartment = new HashMap<>();
    private final Map<Long, Boolean> currentAdminKeyboardState = new HashMap<>();
    private final Map<Long, BotState> botStateMap = new HashMap<>();

    private ChatPropertyModeService(){
    }

    public static ChatPropertyModeService getChatProperties(){
        if (chatProperties == null){
            chatProperties = new ChatPropertyModeService();
        } return chatProperties;
    }

    public Departments getCurrentDepartment(long chatId) {
        return currentDepartment.getOrDefault(chatId, Departments.ЖЕУ_ДОКУЧАЄВСЬКЕ);
    }

    public void setCurrentDepartment(long chatId, Departments department) {
        currentDepartment.put(chatId, department);
    }

    public Boolean getCurrentAdminKeyboardState(Long chatId) {
        return currentAdminKeyboardState.getOrDefault(chatId, true);
    }

    public void setCurrentAdminKeyboardState(long chatId, boolean adminKeyboardState) {
        currentAdminKeyboardState.put(chatId, adminKeyboardState);
    }

    public void setBotState(long chatId, BotState botState){
        botStateMap.put(chatId, botState);
    }

    public BotState getBotState(long chatId){
        return botStateMap.getOrDefault(chatId, BotState.WAIT_BUTTON);
    }
}
