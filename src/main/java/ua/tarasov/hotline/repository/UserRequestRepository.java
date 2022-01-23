package ua.tarasov.hotline.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.tarasov.hotline.models.Department;
import ua.tarasov.hotline.entities.UserRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface UserRequestRepository extends JpaRepository<UserRequest, Long> {

    UserRequest findByMessageId (Integer messageId);
    List<UserRequest> findAllByChatId(Long chatId);
    List<UserRequest> findAllByChatIdAndState (Long chatId, boolean state);

    List<UserRequest> findAllByDepartment(Department department);

    List<UserRequest> findAllByDepartmentAndState(Department department, boolean state);

    List<UserRequest> findAllByDateTimeBeforeAndState(LocalDateTime beforeTime, boolean state);
}
