package ru.practicum.mainservice.rating.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.mainservice.event.model.Event;
import ru.practicum.mainservice.event.model.State;
import ru.practicum.mainservice.event.repository.EventRepository;
import ru.practicum.mainservice.exception.exception.BadRequestException;
import ru.practicum.mainservice.exception.exception.NotFoundException;
import ru.practicum.mainservice.exception.exception.StateConflictException;
import ru.practicum.mainservice.rating.dto.RatingDto;
import ru.practicum.mainservice.rating.dto.RatingRequestDto;
import ru.practicum.mainservice.rating.mapper.RatingMapper;
import ru.practicum.mainservice.rating.model.Rating;
import ru.practicum.mainservice.rating.model.RatingSortType;
import ru.practicum.mainservice.rating.repository.RatingRepository;
import ru.practicum.mainservice.user.model.User;
import ru.practicum.mainservice.user.repository.UserRepository;

import java.util.Comparator;
import java.util.List;

import static ru.practicum.mainservice.util.LogColorizeUtil.colorizeClass;
import static ru.practicum.mainservice.util.LogColorizeUtil.colorizeMethod;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RatingServiceImpl implements RatingService {
    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RatingMapper ratingMapper;

    @Override
    @Transactional
    public RatingDto create(Long userId, Long eventId, RatingRequestDto ratingRequestDto) {
        log.info("{}: Starting execution of {} method.", colorizeClass("RatingService"), colorizeMethod("create()"));
        log.info("{}.{}: Fetching user with id={}", colorizeClass("RatingService"), colorizeMethod("create()"), userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("User with id=%d not found.", userId)));
        log.info("{}.{}: Fetching event with id={}", colorizeClass("RatingService"), colorizeMethod("create()"), eventId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Event with id=%d not found.", eventId)));

        if (event.getInitiator().equals(user)) {
            throw new BadRequestException("The user cannot rate the event he organized");
        }
        if (!event.getState().equals(State.COMPLETED)) {
            throw new StateConflictException("An unfinished event cannot be rated.");
        }

        log.info("{}.{}: Mapping from RatingRequestDto to Rating", colorizeClass("RatingService"), colorizeMethod("create()"));
        Rating rating = ratingMapper.toRatingFromRatingRequestDto(user, event, ratingRequestDto);

        log.info("{}.{}: Saving rating to database", colorizeClass("RatingService"), colorizeMethod("create()"));
        rating = ratingRepository.save(rating);

        log.info("{}.{}: Calculating event rating", colorizeClass("RatingService"), colorizeMethod("create()"));
        double eventRating = Math.round(ratingRepository.avgRatingByEvent(event) + 100.0) / 100.0;
        event.setRating(eventRating);
        log.info("{}.{}: Saving event with updated rating", colorizeClass("RatingService"), colorizeMethod("create()"));
        eventRepository.save(event);

        log.info("{}.{}: Calculating user rating", colorizeClass("RatingService"), colorizeMethod("create()"));
        double initiatorRating = Math.round(ratingRepository.avgRatingByUser(user) * 100.0) / 100.0;

        User initiator = event.getInitiator();
        initiator.setRating(initiatorRating);
        log.info("{}.{}: Saving user with updated rating", colorizeClass("RatingService"), colorizeMethod("create()"));
        userRepository.save(initiator);

        log.info("{}.{}: Mapping from Rating to RatingDto", colorizeClass("RatingService"), colorizeMethod("create()"));
        RatingDto ratingDto = ratingMapper.toRatingDto(rating);

        log.info("{}.{}: Successfully created rating with id={}", colorizeClass("RatingService"), colorizeMethod("create()"), rating.getId());
        return ratingDto;
    }

    @Override
    @Transactional
    public void deleteById(Long userId, Long ratingId) {
        log.info("{}: Starting execution of {} method.", colorizeClass("RatingService"), colorizeMethod("deleteById()"));
        log.info("{}.{}: Fetching user with id={}", colorizeClass("RatingService"), colorizeMethod("deleteById()"), userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("User with id=%d not found.", userId)));
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new NotFoundException(String.format("Rating with id=%d not found.", ratingId)));

        if (!rating.getUser().equals(user)) {
            throw new NotFoundException(String.format("Rating with id=%d not found.", ratingId));
        }
        log.info("{}.{}: Deleting rating with id={}", colorizeClass("RatingService"), colorizeMethod("deleteById()"), ratingId);
        ratingRepository.deleteById(ratingId);

        log.info("{}.{}: Rating with id={} deleted successfully.", colorizeClass("RatingService"), colorizeMethod("deleteById()"), ratingId);
    }

    @Override
    public RatingDto getRatingById(Long ratingId) {
        log.info("{}: Starting execution of {} method.", colorizeClass("RatingService"), colorizeMethod("getRatingById()"));
        log.info("{}.{}: Fetching rating with id={}", colorizeClass("RatingService"), colorizeMethod("getRatingById()"), ratingId);
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new NotFoundException(String.format("Rating with id=%d not found.", ratingId)));

        log.info("{}.{}: Mapping from Rating to RatingDto", colorizeClass("RatingService"), colorizeMethod("getRatingById()"));
        RatingDto ratingDto = ratingMapper.toRatingDto(rating);

        log.info("{}.{}: Successfully fetched rating with id={}", colorizeClass("RatingService"), colorizeMethod("getRatingById()"), rating.getId());
        return ratingDto;
    }

    @Override
    public List<RatingDto> getAllRatingsByUser(Long userId, int from, int size) {
        log.info("{}: Starting execution of {} method.", colorizeClass("RatingService"), colorizeMethod("getAllRatingsByUser()"));
        log.info("{}.{}: Fetching user with id={}", colorizeClass("RatingService"), colorizeMethod("getAllRatingsByUser()"), userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("User with id=%d not found.", userId)));

        log.info("{}.{}: Creating pageable object", colorizeClass("RatingService"), colorizeMethod("getAllRatingsByUser()"));
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("timestamp").descending());

        log.info("{}.{}: Fetching ratings for user with id={}", colorizeClass("RatingService"), colorizeMethod("getAllRatingsByUser()"), userId);
        Page<Rating> ratingsPage = ratingRepository.findAllByUser(user, pageable);

        log.info("{}.{}: Mapping from Rating to RatingDto", colorizeClass("RatingService"), colorizeMethod("getAllRatingsByUser()"));
        List<RatingDto> ratingDtos = ratingsPage.getContent().stream()
                .map(ratingMapper::toRatingDto)
                .toList();

        log.info("{}.{}: Successfully fetched {} ratings for user with id={}", colorizeClass("RatingService"), colorizeMethod("getAllRatingsByUser()"), ratingDtos.size(), userId);
        return ratingDtos;

    }

    @Override
    public List<RatingDto> getAllRatingsByEvent(Long eventId, RatingSortType sortType, int from, int size) {
        log.info("{}: Starting execution of {} method.", colorizeClass("RatingService"), colorizeMethod("getAllRatingsByEvent()"));
        log.info("{}.{}: Fetching event with id={}", colorizeClass("RatingService"), colorizeMethod("getAllRatingsByEvent()"), eventId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Event with id=%d not found.", eventId)));

        log.info("{}.{}: Creating comparator.", colorizeClass("RatingService"), colorizeMethod("getAllRatingsByEvent()"));

        Comparator<Rating> ratingComparator = Comparator.comparing((Rating r) -> r.getComment() != null, Comparator.reverseOrder())
                .thenComparing(Rating::getTimestamp, Comparator.reverseOrder())
                .thenComparing(Rating::getComment, Comparator.nullsLast(Comparator.reverseOrder()));

        if (sortType.equals(RatingSortType.HIGH_RATING)) {
            ratingComparator = Comparator.comparing((Rating r) -> r.getComment() != null, Comparator.reverseOrder())
                    .thenComparing(Rating::getRating, Comparator.reverseOrder());
        }
        if (sortType.equals(RatingSortType.LOW_RATING)) {
            ratingComparator = Comparator.comparing((Rating r) -> r.getComment() != null, Comparator.reverseOrder())
                    .thenComparing(Rating::getRating, Comparator.naturalOrder());
        }

        log.info("{}.{}: Creating pageable object", colorizeClass("RatingService"), colorizeMethod("getAllRatingsByEvent()"));
        Pageable pageable = PageRequest.of(from / size, size);

        log.info("{}.{}: Fetching ratings for event with id={}", colorizeClass("RatingService"), colorizeMethod("getAllRatingsByEvent()"), eventId);
        Page<Rating> ratingsPage = ratingRepository.findAllByEvent(event, pageable);

        log.info("{}.{}: Mapping from Rating to RatingDto", colorizeClass("RatingService"), colorizeMethod("getAllRatingsByEvent()"));
        List<RatingDto> ratingDtos = ratingsPage.getContent().stream()
                .sorted(ratingComparator)
                .map(ratingMapper::toRatingDto)
                .toList();

        log.info("{}.{}: Successfully fetched {} ratings for event with id={}", colorizeClass("RatingService"), colorizeMethod("getAllRatingsByEvent()"), ratingDtos.size(), eventId);
        return ratingDtos;
    }
}
