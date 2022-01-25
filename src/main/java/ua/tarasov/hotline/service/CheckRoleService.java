package ua.tarasov.hotline.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

public interface CheckRoleService {
    boolean checkIsAdmin(Message message);
    SendMessage getFalseAdminText(Message message);
}
