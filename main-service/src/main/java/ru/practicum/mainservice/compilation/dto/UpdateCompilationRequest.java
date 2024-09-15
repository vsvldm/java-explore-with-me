package ru.practicum.mainservice.compilation.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.UniqueElements;

import java.util.List;

@Data
public class UpdateCompilationRequest {
    @Size(min = 1, max = 50)
    private String title;
    @UniqueElements
    private List<Long> events;
    private Boolean pinned;
}
