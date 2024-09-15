package ru.practicum.mainservice.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.mainservice.event.dto.*;
import ru.practicum.mainservice.event.service.EventService;
import ru.practicum.mainservice.request.dto.ParticipationRequestDto;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
public class EventPrivateController {
    private final EventService eventService;

    @GetMapping
    public List<EventShortDto> getEventsByCurrentUser(@PathVariable Long userId,
                                                      @RequestParam(defaultValue = "0") int from,
                                                      @RequestParam(defaultValue = "10") int size) {
        return eventService.getEventsByCurrentUser(userId, from, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto create(@PathVariable Long userId,
                               @Valid @RequestBody NewEventDto newEventDto) {
        return eventService.create(userId, newEventDto);
    }

    @GetMapping("/{eventId}")
    public EventFullDto getFullEventsByCurrentUser(@PathVariable Long userId,
                                                   @PathVariable Long eventId) {
        return eventService.getFullEventByIdForCurrentUser(userId, eventId);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateByCurrentUser(@PathVariable Long userId,
                                            @PathVariable Long eventId,
                                            @Valid @RequestBody UpdateEventUserRequest updateEventUserRequest) {
        return eventService.updateByCurrentUser(userId, eventId, updateEventUserRequest);
    }

    @GetMapping("/{eventId}/requests")
    public List<ParticipationRequestDto> getRequestsByCurrentUser(@PathVariable Long userId,
                                                                  @PathVariable Long eventId) {
        return eventService.getRequestsByCurrentUser(userId, eventId);
    }

    @PatchMapping("/{eventId}/requests")
    public EventRequestStatusUpdateResult updateStatus(@PathVariable Long userId,
                                                       @PathVariable Long eventId,
                                                       @RequestBody EventRequestStatusUpdateRequest statusUpdateRequest) {
        return eventService.updateStatus(userId, eventId, statusUpdateRequest);
    }
}
