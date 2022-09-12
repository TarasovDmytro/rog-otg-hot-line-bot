package ua.tarasov.hotline.service.impl;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import ua.tarasov.hotline.entities.UserRequest;
import ua.tarasov.hotline.models.BotState;
import ua.tarasov.hotline.models.Department;
import ua.tarasov.hotline.models.StateOfRequest;
import ua.tarasov.hotline.service.ChatPropertyModeService;

@Service
public class ChatPropertyModeServiceImpl implements ChatPropertyModeService {
    private static ChatPropertyModeServiceImpl chatProperties;

    private ChatPropertyModeServiceImpl() {
    }

    @Bean
    public static ChatPropertyModeServiceImpl getChatProperties() {
        if (chatProperties == null) {
            chatProperties = new ChatPropertyModeServiceImpl();
        }
        return chatProperties;
    }

    @Override
    public UserRequest getCurrentRequest(long chatId) {
        return currentRequest.getOrDefault(chatId, new UserRequest());
    }

    @Override
    public void setCurrentRequest(long chatId, UserRequest userRequest) {
        currentRequest.put(chatId, userRequest);
    }

    @Override
    public Department getCurrentDepartment(long chatId) {
        return currentDepartment.getOrDefault(chatId, Department.JEU_DOKUCHAEVSKE);
    }

    @Override
    public void setCurrentDepartment(long chatId, Department department) {
        currentDepartment.put(chatId, department);
    }

    @Override
    public Boolean getCurrentAdminKeyboardState(Long chatId) {
        return currentAdminKeyboardState.getOrDefault(chatId, true);
    }

    @Override
    public void setCurrentAdminKeyboardState(long chatId, boolean adminKeyboardState) {
        currentAdminKeyboardState.put(chatId, adminKeyboardState);
    }

    @Override
    public void setCurrentBotState(long chatId, BotState botState) {
        currentBotState.put(chatId, botState);
    }

    @Override
    public BotState getCurrentBotState(long chatId) {
        return currentBotState.getOrDefault(chatId, BotState.WAIT_BUTTON);
    }

    @Override
    public void setCurrentStateOfRequest(long chatId, StateOfRequest stateOfRequest) {
        currentStateOfRequest.put(chatId, stateOfRequest);
    }

    @Override
    public StateOfRequest getCurrentStateOfRequest(Long chatId) {
        return currentStateOfRequest.getOrDefault(chatId, StateOfRequest.REQUEST_CREATED);
    }
}


