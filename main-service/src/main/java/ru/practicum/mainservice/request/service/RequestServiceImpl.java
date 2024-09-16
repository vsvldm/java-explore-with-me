package ru.practicum.mainservice.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.mainservice.event.model.Event;
import ru.practicum.mainservice.event.model.State;
import ru.practicum.mainservice.event.repository.EventRepository;
import ru.practicum.mainservice.exception.exception.NotFoundException;
import ru.practicum.mainservice.exception.exception.EventParticipationConstraintException;
import ru.practicum.mainservice.request.dto.ParticipationRequestDto;
import ru.practicum.mainservice.request.mapper.RequestMapper;
import ru.practicum.mainservice.request.model.ParticipationRequest;
import ru.practicum.mainservice.request.model.RequestStatus;
import ru.practicum.mainservice.request.repository.RequestRepository;
import ru.practicum.mainservice.user.model.User;
import ru.practicum.mainservice.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static ru.practicum.mainservice.util.LogColorizeUtil.colorizeClass;
import static ru.practicum.mainservice.util.LogColorizeUtil.colorizeMethod;

@Service
@Slf4j
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RequestMapper requestMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getRequests(Long userId) {
        log.info("{}: Starting execution of {} method.", colorizeClass("RequestService"), colorizeMethod("getRequests()"));

        log.info("{}.{}: Fetching user with id={}", colorizeClass("RequestService"), colorizeMethod("getRequests()"), userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("User with id=%d not found", userId)));

        log.info("{}.{}: Fetching requests by user with id={}", colorizeClass("RequestService"), colorizeMethod("getRequests()"), userId);
        Collection<ParticipationRequest> requests = requestRepository.findAllByRequester(user);

        if (requests.isEmpty()) {
            log.info("{}.{}: No requests found for user with id={}", colorizeClass("RequestService"), colorizeMethod("getRequests()"), userId);
            return List.of();
        }

        log.info("{}.{}: Mapping requests to DTOs", colorizeClass("RequestService"), colorizeMethod("getRequests()"));
        List<ParticipationRequestDto> result = requests.stream()
                .map(requestMapper::toParticipationRequestDto)
                .toList();

        log.info("{}.{}: Successfully fetched requests for user with id={}.", colorizeClass("RequestService"), colorizeMethod("getRequests()"), userId);
        return result;
    }

    @Override
    @Transactional
    public ParticipationRequestDto create(Long userId, Long eventId) {
        log.info("{}: Starting execution of {} method.", colorizeClass("RequestService"), colorizeMethod("create()"));

        log.info("{}.{}: Fetching user with id={}", colorizeClass("RequestService"), colorizeMethod("create()"), userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("User with id=%d not found", userId)));

        log.info("{}.{}: Fetching event with id={} for user with id={}", colorizeClass("RequestService"), colorizeMethod("create()"), eventId, userId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Event with id=%d not found", eventId)));

        log.info("{}.{}: Checking whether the creator of the event with id={} is the creator of the request.", colorizeClass("RequestService"), colorizeMethod("create()"), eventId);
        if (event.getInitiator().equals(user)) {
            throw new EventParticipationConstraintException(String.format("User with id=%d is the owner of the event with id=%d.", userId, eventId));
        }

        if (!event.getState().equals(State.PUBLISHED)) {
            throw new EventParticipationConstraintException(String.format("Event not published. " +
                    "A user with id=%d cannot make a request to participate in an event with id=%d.", userId, eventId));
        }

        log.info("{}.{}: Checking if event with id={} has reached its participant limit.", colorizeClass("RequestService"), colorizeMethod("create()"), eventId);
        if (event.getParticipantLimit() > 0 && event.getConfirmedRequests().equals(event.getParticipantLimit())) {
            throw new EventParticipationConstraintException(String.format("The event with id=%d has reached the limit(%d) of requests for participation.", eventId, event.getParticipantLimit()));
        }

        log.info("{}.{}: Determining the status of the participation request for event with id={}", colorizeClass("RequestService"), colorizeMethod("create()"), eventId);
        RequestStatus status = event.isRequestModeration() ? RequestStatus.PENDING : RequestStatus.CONFIRMED;

        if (event.getParticipantLimit() == 0) {
            status = RequestStatus.CONFIRMED;
        }

        log.info("{}.{}: Creating a new participation request for user with id={} and event with id={}", colorizeClass("RequestService"), colorizeMethod("create()"), userId, eventId);
        ParticipationRequest request = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .event(event)
                .requester(user)
                .status(status)
                .build();

        log.info("{}.{}: Saving the participation request", colorizeClass("RequestService"), colorizeMethod("create()"));
        request = requestRepository.save(request);

        if (status.equals(RequestStatus.CONFIRMED)) {
            log.info("{}.{}: Participation request for event with id={} has status CONFIRMED. Incrementing confirmed requests count.", colorizeClass("RequestService"), colorizeMethod("create()"), eventId);
            event.setConfirmedRequests(event.getConfirmedRequests() + 1);
            eventRepository.save(event);
        }

        log.info("{}.{}: Mapping participation request to ParticipationRequestDto", colorizeClass("RequestService"), colorizeMethod("create()"));
        ParticipationRequestDto requestDto = requestMapper.toParticipationRequestDto(request);

        log.info("{}.{}: Successfully created participation request with id={}", colorizeClass("RequestService"), colorizeMethod("create()"), request.getId());

        return requestDto;
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancel(Long userId, Long requestId) {
        log.info("{}: Starting execution of {} method.", colorizeClass("RequestService"), colorizeMethod("cancel()"));

        log.info("{}.{}: Fetching user with id={}", colorizeClass("RequestService"), colorizeMethod("cancel()"), userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("User with id=%d not found", userId)));

        log.info("{}.{}: Fetching request with id={}", colorizeClass("RequestService"), colorizeMethod("cancel()"), requestId);
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException(String.format("Request with id=%d not found", requestId)));

        if (!request.getRequester().equals(user)) {
            throw new NotFoundException(String.format("Request with id=%d not found", requestId));
        }

        log.info("{}.{}: Cancelling request with id={}", colorizeClass("RequestService"), colorizeMethod("cancel()"), requestId);
        request.setStatus(RequestStatus.CANCELED);

        log.info("{}.{}: Saving the request", colorizeClass("RequestService"), colorizeMethod("cancel()"));
        request = requestRepository.save(request);

        log.info("{}.{}: Mapping request to DTO", colorizeClass("RequestService"), colorizeMethod("cancel()"));
        ParticipationRequestDto requestDto = requestMapper.toParticipationRequestDto(request);

        log.info("{}.{}: Successfully cancelled request with id={}", colorizeClass("RequestService"), colorizeMethod("cancel()"), requestId);
        return requestDto;
    }
}
