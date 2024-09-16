package ru.practicum.mainservice.compilation.dto;

import lombok.Builder;
import lombok.Data;
import ru.practicum.mainservice.event.dto.EventShortDto;

import java.util.List;

@Data
@Builder
public class CompilationDto {
    private Long id;
    private String title;
    private List<EventShortDto> events;
    private boolean pinned;
}
