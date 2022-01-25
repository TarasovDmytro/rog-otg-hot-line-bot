package ua.tarasov.hotline.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.tarasov.hotline.models.Department;
import ua.tarasov.hotline.entities.UserRequest;
import ua.tarasov.hotline.repository.UserRequestRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class UserRequestService {
    private final UserRequestRepository requestRepository;
    @Value("${request.storage.months}")
    private Integer termOfStorageOfRequestsInMonths;

    public UserRequestService(@Autowired UserRequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }


    public void saveRequest(UserRequest request){
        requestRepository.save(request);
    }

    public UserRequest findByMessageId(Integer messageId) {
        return requestRepository.findByMessageId(messageId);
    }

    public List<UserRequest> findMessagesByBotUser(Long id) {
        return requestRepository.findAllByChatId(id);
    }

    public List<UserRequest> findMessagesByBotUserAndState(Long botUserId, boolean state) {
        return requestRepository.findAllByChatIdAndState(botUserId, state);
    }

    public List<UserRequest> findAllByDepartment(Department department) {
        return requestRepository.findAllByDepartment(department);
    }

    public List<UserRequest> findMessagesByDepartmentAndState(Department department, boolean state) {
        return requestRepository.findAllByDepartmentAndState(department, state);
    }

    public void deleteUserRequest(UserRequest userRequest) {
        requestRepository.delete(userRequest);
    }

    @Transactional
    public void cleanRequestDB() {
        List<UserRequest> requests = requestRepository.findAll(Sort.by("id"));
        log.info("requests: {}", requests);
        UserRequest lastRequest = requests.get(requests.size() - 1);
        LocalDateTime dateTime = lastRequest.getDateTime();
        LocalDateTime minDateTime = dateTime.minusMonths(termOfStorageOfRequestsInMonths);
        List<UserRequest> oldRequests = requestRepository.findAllByDateTimeBeforeAndState(minDateTime, true);
        requestRepository.deleteAll(oldRequests);
    }
}
