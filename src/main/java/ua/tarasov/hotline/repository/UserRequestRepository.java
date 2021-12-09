package ua.tarasov.hotline.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.tarasov.hotline.models.model.Departments;
import ua.tarasov.hotline.models.entities.UserRequest;

import java.util.List;

public interface UserRequestRepository extends JpaRepository<UserRequest, Long> {

    UserRequest findByMessageId (Integer messageId);
    List<UserRequest> findAllByChatId(Long chatId);
    List<UserRequest> findAllByChatIdAndState (Long chatId, boolean state);
    List<UserRequest> findAllByState (boolean state);

    List<UserRequest> findAllByDepartment(Departments department);

    List<UserRequest> findAllByDepartmentAndState(Departments department, boolean state);
}
