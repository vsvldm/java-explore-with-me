package ru.practicum.mainservice.event.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.mainservice.category.model.Category;
import ru.practicum.mainservice.category.repository.CategoryRepository;
import ru.practicum.mainservice.event.dto.*;
import ru.practicum.mainservice.event.mapper.EventMapper;
import ru.practicum.mainservice.event.model.*;
import ru.practicum.mainservice.event.repository.EventRepository;
import ru.practicum.mainservice.exception.exception.*;
import ru.practicum.mainservice.request.dto.ParticipationRequestDto;
import ru.practicum.mainservice.request.mapper.RequestMapper;
import ru.practicum.mainservice.request.model.ParticipationRequest;
import ru.practicum.mainservice.request.model.RequestStatus;
import ru.practicum.mainservice.request.repository.RequestRepository;
import ru.practicum.mainservice.user.model.User;
import ru.practicum.mainservice.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;

import static ru.practicum.mainservice.event.specification.EventSpecification.getAdminFilters;
import static ru.practicum.mainservice.event.specification.EventSpecification.getPublicFilters;
import static ru.practicum.mainservice.util.LogColorizeUtil.colorizeClass;
import static ru.practicum.mainservice.util.LogColorizeUtil.colorizeMethod;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;
    private final EventMapper eventMapper;
    private final RequestMapper requestMapper;
    private final ObjectMapper objectMapper;
    @Value("${spring.application.name}")
    private String serviceId;

    @Override
    @Transactional
    public List<EventShortDto> getEvents(String text,
                                         List<Long> categories,
                                         Boolean paid,
                                         LocalDateTime rangeStart,
                                         LocalDateTime rangeEnd,
                                         Boolean onlyAvailable,
                                         SortType sort,
                                         Integer from,
                                         Integer size,
                                         HttpServletRequest request) {
        log.info("{}: Starting execution of {} method.", colorizeClass("EventService"), colorizeMethod("getEvents()"));
        log.info("{}.{}: Checking rangeStart and rangeEnd values.", colorizeClass("EventService"), colorizeMethod("getEvents()"));
        if (rangeStart != null && rangeEnd != null) {
            if (rangeStart.isAfter(rangeEnd)) {
                throw new BadRequestException("rangeEnd cannot be before rangeStart");
            }
        }
        Specification<Event> spec = getPublicFilters(text, categories, paid, rangeStart, rangeEnd, onlyAvailable);
        Sort sorting = Sort.by("eventDate");

        if (sort != null) {
            log.info("{}.{}: Sorting events based on SortType.", colorizeClass("EventService"), colorizeMethod("getEvents()"));
            if (sort == SortType.EVENT_DATE) {
                sorting = Sort.by("eventDate");
            } else if (sort == SortType.VIEWS) {
                sorting = Sort.by("views");
            }
        }

        Pageable pageable = PageRequest.of(from / size, size, sorting);

        log.info("{}.{}: Fetching events with filters: text={}, categories={}, paid={}, rangeStart={}, rangeEnd={}, onlyAvailable={}",
                colorizeClass("EventService"), colorizeMethod("getEvents()"), text, categories, paid, rangeStart, rangeEnd, onlyAvailable);

        Page<Event> events = eventRepository.findAll(spec, pageable);
        log.info("{}.{}: Total events found: {}", colorizeClass("EventService"), colorizeMethod("getEvents()"), events.getTotalElements());

        log.info("{}.{}: Sending statistical data", colorizeClass("EventService"), colorizeMethod("getEvents()"));
        sendStatisticalData(request);

        log.info("{}.{}: Updating views for each event.", colorizeClass("EventService"), colorizeMethod("getEvents()"));
        List<Event> eventList = events.getContent().stream()
                .peek(event -> {
                    Long views = getUniqueViews(event, request.getRequestURI());
                    views++;
                    event.setViews(views);
                })
                .toList();

        log.info("{}.{}: Performing batch update for all events.", colorizeClass("EventService"), colorizeMethod("getEvents()"));
        eventRepository.saveAll(eventList);

        log.info("{}.{}: Converting events to EventShortDto.", colorizeClass("EventService"), colorizeMethod("getEvents()"));
        List<EventShortDto> shortEvents = events.getContent().stream()
                .map(eventMapper::toEventShortDtoFromEvent)
                .toList();

        log.info("{}.{}: Successfully fetched {} events.", colorizeClass("EventService"), colorizeMethod("getEvents()"), shortEvents.size());


        return shortEvents;
    }

    @Override
    @Transactional
    public EventFullDto getEventById(Long eventId, HttpServletRequest request) {
        log.info("{}: Starting execution of {} method.", colorizeClass("EventService"), colorizeMethod("getEventById()"));
        log.info("{}.{}: Fetching event with id={}.", colorizeClass("EventService"), colorizeMethod("getEventById()"), eventId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Event with id=%d not found", eventId)));

        if (event.getState() != State.PUBLISHED) {
            throw new NotFoundException(String.format("Event with id=%d not found", eventId));
        }

        log.info("{}.{}: Updating views for each event.", colorizeClass("EventService"), colorizeMethod("getEventById()"));
        Long views = getUniqueViews(event, request.getRequestURI());
        views++;
        event.setViews(views);

        log.info("{}.{}: Updated event with id={}.", colorizeClass("EventService"), colorizeMethod("getEventById()"), eventId);
        event = eventRepository.save(event);

        log.info("{}.{}: Mapping event to EventFullDto", colorizeClass("EventService"), colorizeMethod("getEventById()"));
        EventFullDto fullDto = eventMapper.toEventFullDtoFromEvent(event);

        log.info("{}.{}: Successfully fetched event with id={}.", colorizeClass("EventService"), colorizeMethod("getEventById()"), eventId);

        log.info("{}.{}: Sending statistical data", colorizeClass("EventService"), colorizeMethod("getEventById()"));
        sendStatisticalData(request);
        return fullDto;
    }

    @Override
    public List<EventFullDto> getFullEvents(List<Long> users,
                                            List<State> states,
                                            List<Long> categories,
                                            LocalDateTime rangeStart,
                                            LocalDateTime rangeEnd,
                                            Integer from,
                                            Integer size) {
        log.info("{}: Starting execution of {} method.", colorizeClass("EventService"), colorizeMethod("getFullEvents()"));

        Pageable pageable = PageRequest.of(from / size, size);
        Specification<Event> spec = getAdminFilters(users, states, categories, rangeStart, rangeEnd);

        log.info("{}.{}: Fetching events with filters: users={}, states={}, categories={}, rangeStart={}, rangeEnd={}",
                colorizeClass("EventService"), colorizeMethod("getFullEvents()"), users, states, categories, rangeStart, rangeEnd);

        Page<Event> events = eventRepository.findAll(spec, pageable);

        log.info("{}.{}: Total events found: {}", colorizeClass("EventService"), colorizeMethod("getFullEvents()"), events.getTotalElements());

        List<EventFullDto> eventFullDtos = events.getContent().stream()
                .map(eventMapper::toEventFullDtoFromEvent)
                .toList();

        log.info("{}.{}: Successfully fetched {} events.", colorizeClass("EventService"), colorizeMethod("getFullEvents()"), eventFullDtos.size());

        return eventFullDtos;
    }

    @Override
    @Transactional
    public EventFullDto updateByAdmin(Long eventId, UpdateEventAdminRequest updateEventAdminRequest) {
        log.info("{}: Starting execution of {} method.", colorizeClass("EventService"), colorizeMethod("updateByAdmin()"));

        log.info("{}.{}: Fetching event with id={}.", colorizeClass("EventService"), colorizeMethod("updateByAdmin()"), eventId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Event with id=%d not found", eventId)));

        log.info("{}.{}: Beginning updating the fields.", colorizeClass("EventService"), colorizeMethod("updateByAdmin()"));
        StringBuilder updatedFieldsLog = new StringBuilder();

        if (updateEventAdminRequest.getAnnotation() != null) {
            event.setAnnotation(updateEventAdminRequest.getAnnotation());
            updatedFieldsLog.append("Annotation|");
        }
        if (updateEventAdminRequest.getCategory() != null) {
            event.setCategory(categoryRepository.findById(updateEventAdminRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException(String.format("Category with id=%d not found", updateEventAdminRequest.getCategory()))));
            updatedFieldsLog.append("Category|");
        }
        if (updateEventAdminRequest.getDescription() != null) {
            event.setDescription(updateEventAdminRequest.getDescription());
            updatedFieldsLog.append("Description|");
        }
        if (updateEventAdminRequest.getLocation() != null) {
            event.setLocation(updateEventAdminRequest.getLocation());
            updatedFieldsLog.append("Location|");
        }
        if (updateEventAdminRequest.getPaid() != null) {
            event.setPaid(updateEventAdminRequest.getPaid());
            updatedFieldsLog.append("Paid|");
        }
        if (updateEventAdminRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateEventAdminRequest.getParticipantLimit());
            updatedFieldsLog.append("ParticipantLimit|");
        }
        if (updateEventAdminRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateEventAdminRequest.getRequestModeration());
            updatedFieldsLog.append("RequestModeration|");
        }
        if (updateEventAdminRequest.getStateAction() != null) {
            if (updateEventAdminRequest.getStateAction() == StateAdmin.PUBLISH_EVENT) {
                if (event.getState() == State.PENDING) {
                    event.setState(State.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                } else {
                    throw new ConstraintUpdatingException("Event can only be published if it is in a pending state.");
                }
            } else if (updateEventAdminRequest.getStateAction() == StateAdmin.REJECT_EVENT) {
                if (event.getState() == State.PENDING) {
                    event.setState(State.CANCELED);
                } else {
                    throw new ConstraintUpdatingException("Event can only be rejected if it has not yet been published.");
                }
            }
            updatedFieldsLog.append("StateAction|");
        }
        if (updateEventAdminRequest.getEventDate() != null) {
            LocalDateTime minDateConstraint = LocalDateTime.now().plusHours(2);
            log.info("{}.{}: Minimum allowed event date={}", colorizeClass("EventService"), colorizeMethod("updateByAdmin()"), minDateConstraint);

            if (event.getPublishedOn() != null && !isStartDateValid(event.getPublishedOn(), updateEventAdminRequest.getEventDate(), 1)) {
                throw new ConstraintUpdatingException("The start date of the event being modified must be no earlier than an hour from the date of publication.");
            } else if (updateEventAdminRequest.getEventDate().isBefore(minDateConstraint)) {
                throw new BadRequestException(String.format("Field: eventDate. " +
                        "Error: Должно содержать дату не ранее %s. " +
                        "Value: %s", minDateConstraint, updateEventAdminRequest.getEventDate()));
            }
            event.setEventDate(updateEventAdminRequest.getEventDate());
            updatedFieldsLog.append("EventDate|");
        }
        if (updateEventAdminRequest.getTitle() != null) {
            event.setTitle(updateEventAdminRequest.getTitle());
            updatedFieldsLog.append("Title|");
        }

        String updatedFields = updatedFieldsLog.toString().replaceAll("\\|$", "").replace("|", ", ");

        log.info("{}.{}: Updated fields: {}.", colorizeClass("EventService"), colorizeMethod("updateByAdmin()"), updatedFields);

        log.info("{}.{}: Saving updated event to database", colorizeClass("EventService"), colorizeMethod("updateByAdmin()"));
        event = eventRepository.save(event);

        log.info("{}.{}: Mapping event to EventFullDto", colorizeClass("EventService"), colorizeMethod("updateByAdmin()"));
        EventFullDto fullDto = eventMapper.toEventFullDtoFromEvent(event);

        log.info("{}.{}: Successfully updated event with id={}", colorizeClass("EventService"), colorizeMethod("updateByAdmin()"), event.getId());
        return fullDto;
    }


    @Override
    public List<EventShortDto> getEventsByCurrentUser(Long userId, int from, int size) {
        log.info("{}: Starting execution of {} method.", colorizeClass("EventService"), colorizeMethod("getEventsByCurrentUser()"));

        log.info("{}.{}: Fetching user with id: {}", colorizeClass("EventService"), colorizeMethod("getEventsByCurrentUser()"), userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("User with id=%d not found", userId)));

        log.info("{}.{}: Creating Pageable with page number {} and size {}", colorizeClass("EventService"), colorizeMethod("getEventsByCurrentUser()"), from / size, size);
        Pageable pageable = PageRequest.of(from / size, size);

        log.info("{}.{}: Fetching events for user with id: {}", colorizeClass("EventService"), colorizeMethod("getEventsByCurrentUser()"), userId);
        Page<Event> page = eventRepository.findAllByInitiator(user, pageable);

        if (!page.hasContent()) {
            log.info("{}.{}: No events found for user with id: {}", colorizeClass("EventService"), colorizeMethod("getEventsByCurrentUser()"), userId);
            return List.of();
        }

        log.info("{}.{}: Mapping events to EventShortDto", colorizeClass("EventService"), colorizeMethod("getEventsByCurrentUser()"));
        List<EventShortDto> events = page.getContent().stream()
                .map(eventMapper::toEventShortDtoFromEvent)
                .toList();

        log.info("{}.{}: Successfully fetched {} events for user with id: {}", colorizeClass("EventService"), colorizeMethod("getEventsByCurrentUser()"), events.size(), userId);
        return events;
    }

    @Override
    @Transactional
    public EventFullDto create(Long userId, NewEventDto newEventDto) {
        log.info("{}: Starting execution of {} method.", colorizeClass("EventService"), colorizeMethod("create()"));

        log.info("{}.{}: Fetching user with id={}", colorizeClass("EventService"), colorizeMethod("create()"), userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("User with id=%d not found", userId)));

        log.info("{}.{}: Fetching category with id={}", colorizeClass("EventService"), colorizeMethod("create()"), newEventDto.getCategory());
        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException(String.format("Category with id=%d  not found", newEventDto.getCategory())));

        LocalDateTime minDateConstraint = LocalDateTime.now().plusHours(2);
        log.info("{}.{}: Minimum allowed event date={}", colorizeClass("EventService"), colorizeMethod("create()"), minDateConstraint);

        if (newEventDto.getEventDate().isBefore(minDateConstraint)) {
            throw new BadRequestException(String.format("Field: eventDate. " +
                    "Error: Должно содержать дату не ранее %s. " +
                    "Value: %s", minDateConstraint, newEventDto.getEventDate()));
        }

        log.info("{}.{}: Mapping from NewEventDto to Event", colorizeClass("EventService"), colorizeMethod("create()"));
        Event event = eventMapper.toEventFromNewEventDto(newEventDto, category, user);

        log.info("{}.{}: Saving event to database", colorizeClass("EventService"), colorizeMethod("create()"));
        event = eventRepository.save(event);

        log.info("{}.{}: Mapping from Event to EventFullDto", colorizeClass("EventService"), colorizeMethod("create()"));
        EventFullDto fullDto = eventMapper.toEventFullDtoFromEvent(event);

        log.info("{}.{}: Successfully created event with id={}", colorizeClass("EventService"), colorizeMethod("create()"), event.getId());
        return fullDto;
    }

    @Override
    public EventFullDto getFullEventByIdForCurrentUser(Long userId, Long eventId) {
        log.info("{}: Starting execution of {} method.", colorizeClass("EventService"), colorizeMethod("getFullEventByIdForCurrentUser()"));

        log.info("{}.{}: Fetching user with id={}", colorizeClass("EventService"), colorizeMethod("getFullEventByIdForCurrentUser()"), userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("User with id=%d not found", userId)));

        log.info("{}.{}: Fetching event with id={} for user with id={}", colorizeClass("EventService"), colorizeMethod("getFullEventByIdForCurrentUser()"), eventId, userId);
        Event event = eventRepository.findEventByIdAndInitiator(eventId, user)
                .orElseThrow(() -> new NotFoundException(String.format("Event with id=%d not found", eventId)));

        log.info("{}.{}: Mapping event to EventFullDto", colorizeClass("EventService"), colorizeMethod("getFullEventByIdForCurrentUser()"));
        EventFullDto fullDto = eventMapper.toEventFullDtoFromEvent(event);

        log.info("{}.{}: Successfully fetched full event details for event with id={}", colorizeClass("EventService"), colorizeMethod("getFullEventByIdForCurrentUser()"), eventId);
        return fullDto;
    }


    @Override
    @Transactional
    public EventFullDto updateByCurrentUser(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest) {
        log.info("{}: Starting execution of {} method.", colorizeClass("EventService"), colorizeMethod("updateByCurrentUser()"));

        log.info("{}.{}: Fetching user with id={}", colorizeClass("EventService"), colorizeMethod("updateByCurrentUser()"), userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("User with id=%d not found", userId)));

        log.info("{}.{}: Fetching event with id={} for user with id={}", colorizeClass("EventService"), colorizeMethod("updateByCurrentUser()"), eventId, userId);
        Event event = eventRepository.findEventByIdAndInitiator(eventId, user)
                .orElseThrow(() -> new NotFoundException(String.format("Event with id=%d not found", eventId)));

        if (event.getState().equals(State.PUBLISHED)) {
            throw new ViolationOfEditingRulesException("Only pending or canceled events can be changed");
        }

        log.info("{}.{}: Beginning updating the fields.", colorizeClass("EventService"), colorizeMethod("updateByCurrentUser()"));
        StringBuilder updatedFieldsLog = new StringBuilder();

        if (updateEventUserRequest.getAnnotation() != null) {
            event.setAnnotation(updateEventUserRequest.getAnnotation());
            updatedFieldsLog.append("Annotation|");
        }
        if (updateEventUserRequest.getCategory() != null) {
            event.setCategory(categoryRepository.findById(updateEventUserRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException(String.format("Category with id=%d not found", updateEventUserRequest.getCategory()))));
            updatedFieldsLog.append("Category|");
        }
        if (updateEventUserRequest.getDescription() != null) {
            event.setDescription(updateEventUserRequest.getDescription());
            updatedFieldsLog.append("Description|");
        }
        if (updateEventUserRequest.getEventDate() != null) {
            if (!isStartDateValid(LocalDateTime.now(), updateEventUserRequest.getEventDate(), 2)) {
                throw new BadRequestException("The date and time for which the event is scheduled cannot be earlier than two hours from the current moment.");
            }

            event.setEventDate(updateEventUserRequest.getEventDate());
            updatedFieldsLog.append("EventDate|");
        }
        if (updateEventUserRequest.getLocation() != null) {
            event.setLocation(updateEventUserRequest.getLocation());
            updatedFieldsLog.append("Location|");
        }
        if (updateEventUserRequest.getPaid() != null) {
            event.setPaid(updateEventUserRequest.getPaid());
            updatedFieldsLog.append("Paid|");
        }
        if (updateEventUserRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateEventUserRequest.getParticipantLimit());
            updatedFieldsLog.append("ParticipantLimit|");
        }
        if (updateEventUserRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateEventUserRequest.getRequestModeration());
            updatedFieldsLog.append("RequestModeration|");
        }
        if (updateEventUserRequest.getStateAction() != null) {
            if (updateEventUserRequest.getStateAction().equals(StateUser.SEND_TO_REVIEW)) {
                event.setState(State.PENDING);
            }
            if (updateEventUserRequest.getStateAction().equals(StateUser.CANCEL_REVIEW) && event.getState().equals(State.PENDING)) {
                event.setState(State.CANCELED);
            }
            updatedFieldsLog.append("StateAction|");
        }
        if (updateEventUserRequest.getTitle() != null) {
            event.setTitle(updateEventUserRequest.getTitle());
            updatedFieldsLog.append("Title|");
        }

        String updatedFields = updatedFieldsLog.toString().replaceAll("\\|$", "").replace("|", ", ");

        log.info("{}.{}: Updated fields: {}.", colorizeClass("EventService"), colorizeMethod("updateByCurrentUser()"), updatedFields);

        log.info("{}.{}: Saving updated event to database", colorizeClass("EventService"), colorizeMethod("updateByCurrentUser()"));
        event = eventRepository.save(event);

        log.info("{}.{}: Mapping event to EventFullDto", colorizeClass("EventService"), colorizeMethod("updateByCurrentUser()"));
        EventFullDto fullDto = eventMapper.toEventFullDtoFromEvent(event);

        log.info("{}.{}: Successfully updated event with id={}", colorizeClass("EventService"), colorizeMethod("updateByCurrentUser()"), event.getId());
        return fullDto;
    }

    @Override
    public List<ParticipationRequestDto> getRequestsByCurrentUser(Long userId, Long eventId) {
        log.info("{}: Starting execution of {} method.", colorizeClass("EventService"), colorizeMethod("getRequestsByCurrentUser()"));

        log.info("{}.{}: Fetching user with id={}", colorizeClass("EventService"), colorizeMethod("getRequestsByCurrentUser()"), userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("User with id=%d not found", userId)));

        log.info("{}.{}: Fetching event with id={} for user with id={}", colorizeClass("EventService"), colorizeMethod("getRequestsByCurrentUser()"), eventId, userId);
        Event event = eventRepository.findEventByIdAndInitiator(eventId, user)
                .orElseThrow(() -> new NotFoundException(String.format("Event with id=%d not found", eventId)));

        log.info("{}.{}: Fetching requests by event with id={} for user with id={}", colorizeClass("EventService"), colorizeMethod("getRequestsByCurrentUser()"), eventId, userId);
        Collection<ParticipationRequest> requests = requestRepository.findAllByEvent(event);
        if (requests.isEmpty()) {
            log.info("{}.{}: No requests found for user with id={}", colorizeClass("EventService"), colorizeMethod("getRequestsByCurrentUser()"), userId);
            return List.of();
        }

        log.info("{}.{}: Mapping requests to ParticipationRequestDto", colorizeClass("EventService"), colorizeMethod("getRequestsByCurrentUser()"));
        List<ParticipationRequestDto> requestDtos = requests.stream()
                .map(requestMapper::toParticipationRequestDto)
                .toList();

        log.info("{}.{}: Successfully fetched {} requests for user with id: {}", colorizeClass("EventService"), colorizeMethod("getEventsByCurrentUser()"), requestDtos.size(), userId);
        return requestDtos;
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateStatus(Long userId,
                                                       Long eventId,
                                                       EventRequestStatusUpdateRequest statusUpdateRequest) {
        log.info("{}: Starting execution of {} method.", colorizeClass("EventService"), colorizeMethod("updateStatus()"));

        log.info("{}.{}: Fetching user with id={}", colorizeClass("EventService"), colorizeMethod("updateStatus()"), userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("User with id=%d not found", userId)));

        log.info("{}.{}: Fetching event with id={} for user with id={}", colorizeClass("EventService"), colorizeMethod("updateStatus()"), eventId, userId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Event with id=%d not found", eventId)));

        log.info("{}.{}: Processing event with id={}, participant limit={}, request moderation={}", colorizeClass("EventService"), colorizeMethod("updateStatus()"), eventId, event.getParticipantLimit(), event.isRequestModeration());
        if (event.getParticipantLimit() == 0 || !event.isRequestModeration()) {
            log.info("{}: Participants limit is 0 or request moderation is disabled.", colorizeClass("EventService"));
            return EventRequestStatusUpdateResult.builder()
                    .confirmedRequests(requestRepository.findAllByEvent(event).stream().map(requestMapper::toParticipationRequestDto).toList())
                    .rejectedRequests(List.of())
                    .build();
        }

        log.info("{}.{}: Fetching PENDING requests for event with id={}", colorizeClass("EventService"), colorizeMethod("updateStatus()"), eventId);
        Collection<ParticipationRequest> pendingRequests = requestRepository.findAllByEventAndStatus(event, RequestStatus.PENDING);

        if (pendingRequests.isEmpty()) {
            throw new ViolationOfEditingRulesException("Request must have status PENDING");
        }

        long confirmedRequestsCount = requestRepository.countByEventAndStatus(event, RequestStatus.CONFIRMED);
        log.info("{}.{}: Current confirmed requests count for event with id={} is {}", colorizeClass("EventService"), colorizeMethod("updateStatus()"), eventId, confirmedRequestsCount);

        if (statusUpdateRequest.getStatus().equals(RequestStatus.CONFIRMED) && confirmedRequestsCount >= event.getParticipantLimit()) {
            throw new ConflictException("The participant limit has been reached");
        }

        for (ParticipationRequest request : pendingRequests) {
            if (confirmedRequestsCount < event.getParticipantLimit()) {
                log.info("{}.{}: Updating request with id={} to status {}", colorizeClass("EventService"), colorizeMethod("updateStatus()"), request.getId(), statusUpdateRequest.getStatus());
                request.setStatus(statusUpdateRequest.getStatus());
                requestRepository.save(request);

                if (statusUpdateRequest.getStatus().equals(RequestStatus.CONFIRMED)) {
                    confirmedRequestsCount++;
                }
            } else {
                log.info("{}.{}: Rejected request with id={} due to participant limit reached", colorizeClass("EventService"), colorizeMethod("updateStatus()"), request.getId());
                request.setStatus(RequestStatus.REJECTED);
                requestRepository.save(request);
            }
        }

        log.info("{}.{}: Saving event with id={} with updated confirmed requests count={}", colorizeClass("EventService"), colorizeMethod("updateStatus()"), eventId, confirmedRequestsCount);
        event.setConfirmedRequests(confirmedRequestsCount);
        eventRepository.save(event);

        EventRequestStatusUpdateResult result = EventRequestStatusUpdateResult.builder()
                .confirmedRequests(requestRepository.findAllByEventAndStatus(event, RequestStatus.CONFIRMED).stream().map(requestMapper::toParticipationRequestDto).toList())
                .rejectedRequests(requestRepository.findAllByEventAndStatus(event, RequestStatus.REJECTED).stream().map(requestMapper::toParticipationRequestDto).toList())
                .build();
        log.info("{}.{}: Successfully updated status for event with id={}", colorizeClass("EventService"), colorizeMethod("updateStatus()"), eventId);

        return result;
    }

    private boolean isStartDateValid(LocalDateTime publicationDate, LocalDateTime startDate, int constraint) {
        long hoursBetween = ChronoUnit.HOURS.between(publicationDate, startDate);
        return hoursBetween >= constraint;
    }

    private void sendStatisticalData(HttpServletRequest request) {
        EndpointHitDto stat = EndpointHitDto.builder()
                .app(serviceId)
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now())
                .build();

        statsClient.create(stat);
    }

    private List<ViewStatsDto> convertResponseToList(ResponseEntity<Object> response) {
        if (response.getBody() == null) {
            return List.of();
        }
        try {
            return objectMapper.convertValue(response.getBody(), new TypeReference<List<ViewStatsDto>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert response to list", e);
        }
    }

    private Long getUniqueViews(Event event, String uri) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String startDate = event.getCreatedOn().format(formatter);
        String endDate = LocalDateTime.now().format(formatter);
        List<String> uris = List.of(uri);

        List<ViewStatsDto> stats = convertResponseToList(statsClient.getStats(startDate, endDate, uris, true));

        return stats.isEmpty()
                ? 0L
                : stats.stream().mapToLong(ViewStatsDto::getHits).sum();
    }
}
