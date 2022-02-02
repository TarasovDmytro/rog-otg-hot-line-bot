package ua.tarasov.hotline.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ua.tarasov.hotline.entities.UserRequest;
import ua.tarasov.hotline.models.Department;
import ua.tarasov.hotline.repository.UserRequestRepository;
import ua.tarasov.hotline.service.UserRequestService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class UserRequestServiceImpl implements UserRequestService {
    private final UserRequestRepository requestRepository;
    @Value("${request.storage.months}")
    private Integer termOfStorageOfRequestsInMonths;

    public UserRequestServiceImpl(@Autowired UserRequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    @Override
    public void saveRequest(UserRequest request){
        requestRepository.save(request);
        cleanRequestDB();
    }

    @Override
    public UserRequest findByMessageId(Integer messageId) {
        return requestRepository.findByMessageId(messageId);
    }

    @Override
    public List<UserRequest> findMessagesByBotUser(Long id) {
        return requestRepository.findAllByChatId(id);
    }

    @Override
    public List<UserRequest> findMessagesByBotUserAndState(Long botUserId, boolean state) {
        return requestRepository.findAllByChatIdAndState(botUserId, state);
    }

    @Override
    public List<UserRequest> findAllByDepartment(Department department) {
        return requestRepository.findAllByDepartment(department);
    }

    @Override
    public List<UserRequest> findMessagesByDepartmentAndState(Department department, boolean state) {
        return requestRepository.findAllByDepartmentAndState(department, state);
    }

    @Override
    public void deleteUserRequest(UserRequest userRequest) {
        requestRepository.delete(userRequest);
    }

    private void cleanRequestDB() {
        List<UserRequest> requests = requestRepository.findAll(Sort.by("id"));
        log.info("requests: {}", requests);
        UserRequest lastRequest = requests.get(requests.size() - 1);
        LocalDateTime dateTime = lastRequest.getDateTime();
        LocalDateTime minDateTime = dateTime.minusMonths(termOfStorageOfRequestsInMonths);
        List<UserRequest> oldRequests = requestRepository.findAllByDateTimeBeforeAndState(minDateTime, true);
        requestRepository.deleteAll(oldRequests);
    }
}
