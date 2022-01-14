package ua.tarasov.hotline.service;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Location;
import ua.tarasov.hotline.models.models.BotState;
import ua.tarasov.hotline.models.models.Department;

import java.util.HashMap;
import java.util.Map;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public class ChatPropertyModeService {
    static ChatPropertyModeService chatProperties;
    final Map<Long, Department> currentDepartment = new HashMap<>();
    final Map<Long, Boolean> currentAdminKeyboardState = new HashMap<>();
    final Map<Long, BotState> botStateMap = new HashMap<>();
    final Map<Long, String> currentRequestAddress = new HashMap<>();
    final Map<Long, Location> currentLocation = new HashMap<>();

    private ChatPropertyModeService() {
    }

    @Bean
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

    public void setCurrentRequestAddress(long chatId, String address) {
        currentRequestAddress.put(chatId, address);
    }

    public String getCurrentRequestAddress(long chatId) {
        return currentRequestAddress.getOrDefault(chatId, "Адресу не встановлено");
    }

    public void setCurrentLocation(long chatId, Location location) {
        currentLocation.put(chatId, location);
    }

    public Location getCurrentLocation(long chatId) {
        return currentLocation.get(chatId);
    }
}
