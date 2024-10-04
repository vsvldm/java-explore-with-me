package ru.practicum.mainservice.event.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.mainservice.event.model.Event;
import ru.practicum.mainservice.event.model.EventSortType;
import ru.practicum.mainservice.event.model.State;

import java.time.LocalDateTime;
import java.util.List;

public class EventSpecification {
    public static Specification<Event> hasUsers(List<Long> users) {
        return (root, query, builder) -> users == null || users.isEmpty()
                ? null
                : root.get("initiator").get("id").in(users);
    }

    public static Specification<Event> hasStates(List<State> states) {
        return (root, query, builder) -> states == null || states.isEmpty()
                ? null
                : root.get("state").in(states);
    }

    public static Specification<Event> hasCategories(List<Long> categories) {
        return (root, query, builder) -> categories == null || categories.isEmpty()
                ? null
                : root.get("category").get("id").in(categories);
    }

    public static Specification<Event> afterRangeStart(LocalDateTime rangeStart) {
        return (root, query, builder) -> rangeStart == null
                ? null
                : builder.greaterThanOrEqualTo(root.get("eventDate"), rangeStart);
    }

    public static Specification<Event> beforeRangeEnd(LocalDateTime rangeEnd) {
        return (root, query, builder) -> rangeEnd == null
                ? null
                : builder.lessThanOrEqualTo(root.get("eventDate"), rangeEnd);
    }

    public static Specification<Event> onlyPublished() {
        return (root, query, builder) -> builder.equal(root.get("state"), State.PUBLISHED);
    }

    public static Specification<Event> onlyCompleted() {
        return (root, query, builder) -> builder.equal(root.get("state"), State.COMPLETED);
    }

    public static Specification<Event> searchText(String text) {
        return (root, query, builder) -> {
            if (text == null || text.isEmpty()) {
                return null;
            }
            String searchPattern = "%" + text.toLowerCase() + "%";
            Predicate annotationPredicate = builder.like(builder.lower(root.get("annotation")), searchPattern);
            Predicate descriptionPredicate = builder.like(builder.lower(root.get("description")), searchPattern);
            return builder.or(annotationPredicate, descriptionPredicate);
        };
    }

    public static Specification<Event> isPaid(Boolean paid) {
        return (root, query, builder) -> paid == null
                ? null
                : builder.equal(root.get("paid"), paid);
    }

    public static Specification<Event> onlyAvailable(Boolean onlyAvailable) {
        return (root, query, builder) -> (onlyAvailable != null && onlyAvailable)
                ? builder.greaterThan(root.get("participantLimit"), root.get("confirmedRequests"))
                : null;
    }

    public static Specification<Event> getAdminFilters(List<Long> users,
                                                       List<State> states,
                                                       List<Long> categories,
                                                       LocalDateTime rangeStart,
                                                       LocalDateTime rangeEnd) {
        return Specification.where(hasUsers(users))
                .and(hasStates(states))
                .and(hasCategories(categories))
                .and(rangeStart != null
                        ? afterRangeStart(rangeStart)
                        : afterRangeStart(LocalDateTime.now()))
                .and(beforeRangeEnd(rangeEnd));
    }

    public static Specification<Event> getPublicFilters(String text,
                                                        List<Long> categories,
                                                        Boolean paid,
                                                        LocalDateTime rangeStart,
                                                        LocalDateTime rangeEnd,
                                                        Boolean onlyAvailable,
                                                        EventSortType sortType) {
        LocalDateTime now = LocalDateTime.now();

        return Specification.where(sortType == null || !sortType.equals(EventSortType.RATING) ? onlyPublished() : onlyCompleted())
                .and(searchText(text))
                .and(hasCategories(categories))
                .and(isPaid(paid))
                .and(rangeStart != null ? afterRangeStart(rangeStart) : afterRangeStart(now))
                .and(beforeRangeEnd(rangeEnd))
                .and(onlyAvailable(onlyAvailable));
    }
}
