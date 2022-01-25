package ua.tarasov.hotline.service;

import org.telegram.telegrambots.meta.api.objects.Location;
import ua.tarasov.hotline.models.BotState;
import ua.tarasov.hotline.models.Department;

import java.util.HashMap;
import java.util.Map;

public interface ChatPropertyModeService {
    Map<Long, Department> currentDepartment = new HashMap<>();
    Map<Long, Boolean> currentAdminKeyboardState = new HashMap<>();
    Map<Long, BotState> currentBotState = new HashMap<>();
    Map<Long, String> currentRequestAddress = new HashMap<>();
    Map<Long, Location> currentLocation = new HashMap<>();

    Department getCurrentDepartment(long chatId);

    void setCurrentDepartment(long chatId, Department department);

    Boolean getCurrentAdminKeyboardState(Long chatId);

    void setCurrentAdminKeyboardState(long chatId, boolean adminKeyboardState);

    void setBotState(long chatId, BotState botState);

    BotState getCurrentBotState(long chatId);

    void setCurrentRequestAddress(long chatId, String address);

    String getCurrentRequestAddress(long chatId);

    void setCurrentLocation(long chatId, Location location);

    Location getCurrentLocation(long chatId);
}
