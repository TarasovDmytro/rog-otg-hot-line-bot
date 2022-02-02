package ua.tarasov.hotline.service.impl;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Location;
import ua.tarasov.hotline.models.BotState;
import ua.tarasov.hotline.models.Department;
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
    public void setCurrentRequestAddress(long chatId, String address) {
        currentRequestAddress.put(chatId, address);
    }

    @Override
    public String getCurrentRequestAddress(long chatId) {
        return currentRequestAddress.getOrDefault(chatId, "Адресу не встановлено");
    }

    @Override
    public void setCurrentLocation(long chatId, Location location) {
        currentLocation.put(chatId, location);
    }

    @Override
    public Location getCurrentLocation(long chatId) {
        return currentLocation.get(chatId);
    }
}
