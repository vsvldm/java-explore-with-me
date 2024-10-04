package ru.practicum.mainservice.rating.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.mainservice.event.model.Event;
import ru.practicum.mainservice.rating.dto.RatingDto;
import ru.practicum.mainservice.rating.dto.RatingRequestDto;
import ru.practicum.mainservice.rating.model.Rating;
import ru.practicum.mainservice.user.model.User;

import java.time.LocalDateTime;

@Component
public class RatingMapper {
    public Rating toRatingFromRatingRequestDto(User user, Event event, RatingRequestDto ratingRequestDto) {
        return Rating.builder()
                .rating(ratingRequestDto.getRating())
                .comment(ratingRequestDto.getComment())
                .event(event)
                .user(user)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public RatingDto toRatingDto(Rating rating) {
        return RatingDto.builder()
                .id(rating.getId())
                .rating(rating.getRating())
                .comment(rating.getComment())
                .eventId(rating.getEvent().getId())
                .userId(rating.getUser().getId())
                .timestamp(rating.getTimestamp())
                .build();
    }
}
