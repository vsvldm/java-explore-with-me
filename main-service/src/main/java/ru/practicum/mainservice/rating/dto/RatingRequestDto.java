package ru.practicum.mainservice.rating.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RatingRequestDto {
    @NotNull
    @DecimalMin("1.0")
    @DecimalMax("5.0")
    private double rating;
    @Size(min = 20, max = 5000)
    private String comment;
}
