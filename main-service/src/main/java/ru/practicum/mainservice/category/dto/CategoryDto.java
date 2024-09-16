package ru.practicum.mainservice.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryDto {
    private long id;
    @NotBlank
    @Size(min = 1, max = 50)
    private String name;
}
