package ua.tarasov.hotline.listener;

import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;

import java.util.List;

public interface WebSiteListener {
    List<PartialBotApiMethod<?>> getWebSiteUpdate(String url);
}
