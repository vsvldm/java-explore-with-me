package ru.practicum.mainservice.event.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.mainservice.event.dto.*;
import ru.practicum.mainservice.event.model.State;
import ru.practicum.mainservice.event.model.EventSortType;
import ru.practicum.mainservice.request.dto.ParticipationRequestDto;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {
    List<EventShortDto> getEvents(String text,
                                  List<Long> categories,
                                  Boolean paid,
                                  LocalDateTime rangeStart,
                                  LocalDateTime rangeEnd,
                                  Boolean onlyAvailable,
                                  EventSortType sort,
                                  Integer from,
                                  Integer size, HttpServletRequest request);

    EventFullDto getEventById(Long eventId, HttpServletRequest request);

    List<EventFullDto> getFullEvents(List<Long> users,
                                     List<State> states,
                                     List<Long> categories,
                                     LocalDateTime rangeStart,
                                     LocalDateTime rangeEnd,
                                     Integer from,
                                     Integer size);

    EventFullDto updateByAdmin(Long eventId, UpdateEventAdminRequest updateEventAdminRequest);

    List<EventShortDto> getEventsByCurrentUser(Long userId, int from, int size);


    EventFullDto create(Long userId, NewEventDto newEventDto);

    EventFullDto getFullEventByIdForCurrentUser(Long userId, Long eventId);

    EventFullDto updateByCurrentUser(Long userId,
                                     Long eventId,
                                     UpdateEventUserRequest updateEventUserRequest);

    List<ParticipationRequestDto> getRequestsByCurrentUser(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateStatus(Long userId,
                                                Long eventId,
                                                EventRequestStatusUpdateRequest statusUpdateRequest);
}
