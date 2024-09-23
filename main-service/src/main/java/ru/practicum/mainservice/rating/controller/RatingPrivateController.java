package ru.practicum.mainservice.rating.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.mainservice.rating.dto.RatingDto;
import ru.practicum.mainservice.rating.dto.RatingRequestDto;
import ru.practicum.mainservice.rating.service.RatingService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}")
@RequiredArgsConstructor
public class RatingPrivateController {
    private final RatingService ratingService;

    @PostMapping("/events/{eventId}/ratings")
    @ResponseStatus(HttpStatus.CREATED)
    public RatingDto create(@PathVariable Long userId,
                            @PathVariable Long eventId,
                            @Valid @RequestBody RatingRequestDto ratingRequestDto) {
        return ratingService.create(userId, eventId, ratingRequestDto);
    }

    @DeleteMapping("/ratings/{ratingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable Long userId,
                           @PathVariable Long ratingId) {
        ratingService.deleteById(userId, ratingId);
    }

    @GetMapping("/ratings")
    public List<RatingDto> getAllRatingsByUser(@PathVariable Long userId,
                                               @RequestParam(defaultValue = "0") int from,
                                               @RequestParam(defaultValue = "10") int size) {
        return ratingService.getAllRatingsByUser(userId, from, size);
    }
}
