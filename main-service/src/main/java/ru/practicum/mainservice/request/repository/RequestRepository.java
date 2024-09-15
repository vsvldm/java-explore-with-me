package ru.practicum.mainservice.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.mainservice.event.model.Event;
import ru.practicum.mainservice.request.model.ParticipationRequest;
import ru.practicum.mainservice.request.model.RequestStatus;
import ru.practicum.mainservice.user.model.User;

import java.util.Collection;

public interface RequestRepository extends JpaRepository<ParticipationRequest, Long> {
    Collection<ParticipationRequest> findAllByEvent(Event event);

    Collection<ParticipationRequest> findAllByEventAndStatus(Event event, RequestStatus status);

    Long countByEventAndStatus(Event event, RequestStatus status);

    Collection<ParticipationRequest> findAllByRequester(User user);
}
