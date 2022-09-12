package ua.tarasov.hotline.service;

import ua.tarasov.hotline.entities.UserRequest;
import ua.tarasov.hotline.models.BotState;
import ua.tarasov.hotline.models.Department;
import ua.tarasov.hotline.models.StateOfRequest;

import java.util.HashMap;
import java.util.Map;

public interface ChatPropertyModeService {
    Map<Long, Department> currentDepartment = new HashMap<>();
    Map<Long, Boolean> currentAdminKeyboardState = new HashMap<>();
    Map<Long, BotState> currentBotState = new HashMap<>();
    Map<Long, StateOfRequest> currentStateOfRequest = new HashMap<>();
    Map<Long, UserRequest> currentRequest = new HashMap<>();
    UserRequest getCurrentRequest(long chatId);
    void setCurrentRequest(long chatId, UserRequest userRequest);

    Department getCurrentDepartment(long chatId);

    void setCurrentDepartment(long chatId, Department department);

    Boolean getCurrentAdminKeyboardState(Long chatId);

    void setCurrentAdminKeyboardState(long chatId, boolean adminKeyboardState);

    void setCurrentBotState(long chatId, BotState botState);

    BotState getCurrentBotState(long chatId);

    void setCurrentStateOfRequest(long chatId, StateOfRequest stateOfRequest);

    StateOfRequest getCurrentStateOfRequest(Long chatId);
}
