package ua.tarasov.hotline.listener;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;

import java.util.List;

public interface NotificationListener {
    List<BotApiMethod<?>> getNewNotifications();
}
