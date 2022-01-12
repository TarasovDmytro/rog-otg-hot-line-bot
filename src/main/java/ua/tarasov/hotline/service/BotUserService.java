package ua.tarasov.hotline.service;

import org.springframework.stereotype.Service;
import ua.tarasov.hotline.models.entities.BotUser;
import ua.tarasov.hotline.models.models.Department;
import ua.tarasov.hotline.models.models.Role;
import ua.tarasov.hotline.repository.BotUserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class BotUserService {
    private final BotUserRepository botUserRepository;

    public BotUserService(BotUserRepository botUserRepository) {
        this.botUserRepository = botUserRepository;
    }

    public void saveBotUser (BotUser botUser){
        botUserRepository.save(botUser);
    }

    public Optional<BotUser> findById (Long botUserId){
        return botUserRepository.findById(botUserId);
    }

    public List<BotUser> findAll (){
        return botUserRepository.findAll();
    }

    public List<BotUser> findAllByDepartment(Department department) {
        return botUserRepository.findAllByDepartmentsContains(department);
    }

    public BotUser findByRole(Role role) {
        return botUserRepository.findByRole(role);
    }
}
