package ua.tarasov.hotline.service;

import ua.tarasov.hotline.entities.UserRequest;
import ua.tarasov.hotline.models.Department;

import java.util.List;

public interface UserRequestService {
    void saveRequest(UserRequest request);
    UserRequest findByMessageId(Integer messageId);
    List<UserRequest> findMessagesByBotUser(Long id);
    List<UserRequest> findMessagesByBotUserAndState(Long botUserId, boolean state);
    List<UserRequest> findAllByDepartment(Department department);
    List<UserRequest> findMessagesByDepartmentAndState(Department department, boolean state);
    void deleteUserRequest(UserRequest userRequest);
}
