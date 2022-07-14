package ua.tarasov.hotline.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public interface CheckRoleService {
    boolean checkIsAdmin(Long userId);
    SendMessage getFalseAdminText(Long userId);
}
