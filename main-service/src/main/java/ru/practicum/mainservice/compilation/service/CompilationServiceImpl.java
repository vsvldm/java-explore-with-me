package ru.practicum.mainservice.compilation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.mainservice.compilation.dto.CompilationDto;
import ru.practicum.mainservice.compilation.dto.NewCompilationDto;
import ru.practicum.mainservice.compilation.dto.UpdateCompilationRequest;
import ru.practicum.mainservice.compilation.mapper.CompilationMapper;
import ru.practicum.mainservice.compilation.model.Compilation;
import ru.practicum.mainservice.compilation.repository.CompilationRepository;
import ru.practicum.mainservice.event.dto.EventShortDto;
import ru.practicum.mainservice.event.mapper.EventMapper;
import ru.practicum.mainservice.event.model.Event;
import ru.practicum.mainservice.event.repository.EventRepository;
import ru.practicum.mainservice.exception.exception.NotFoundException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static ru.practicum.mainservice.util.LogColorizeUtil.colorizeClass;
import static ru.practicum.mainservice.util.LogColorizeUtil.colorizeMethod;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;
    private final EventMapper eventMapper;

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        log.info("{}: Starting execution of {} method.", colorizeClass("CompilationService"), colorizeMethod("getCompilations()"));

        Pageable pageable = PageRequest.of(from / size, size);
        List<Compilation> compilations;

        if (pinned != null) {
            log.info("{}.{}: Fetching compilations with pinned status: pinned={}", colorizeClass("CompilationService"), colorizeMethod("getCompilations()"), pinned);
            compilations = compilationRepository.findAllByPinned(pinned, pageable).getContent();
        } else {
            log.info("{}.{}: Fetching all compilations.", colorizeClass("CompilationService"), colorizeMethod("getCompilations()"));
            compilations = compilationRepository.findAll(pageable).getContent();
        }

        if (compilations.isEmpty()) {
            log.info("{}.{}: No compilations found. Returning an empty list.", colorizeClass("CompilationService"), colorizeMethod("getCompilations()"));
            return List.of();
        }

        log.info("{}.{}: Mapping compilation to CompilationDto", colorizeClass("CompilationService"), colorizeMethod("getCompilations()"));
        List<CompilationDto> compilationDtos = compilations.stream()
                .map(compilation -> {
                    log.info("{}.{}: Mapping event to EventShortDto", colorizeClass("CompilationService"), colorizeMethod("getCompilations()"));
                    List<EventShortDto> eventShortDtos = compilation.getEvents().stream().map(eventMapper::toEventShortDtoFromEvent).toList();
                    return compilationMapper.toCompilationDtoFromCompilation(compilation, eventShortDtos);
                })
                .toList();

        log.info("{}.{}: Successfully fetched {} compilation.", colorizeClass("CompilationService"), colorizeMethod("getCompilations()"), compilationDtos.size());
        return compilationDtos;
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        log.info("{}: Starting execution of {} method.", colorizeClass("CompilationService"), colorizeMethod("getCompilationById()"));
        log.info("{}.{}: Fetching compilation with id={}.", colorizeClass("CompilationService"), colorizeMethod("getCompilationById()"), compId);
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException(String.format("Compilation with id=%d not found", compId)));

        log.info("{}.{}: Mapping event to EventShortDto", colorizeClass("CompilationService"), colorizeMethod("getCompilationById()"));
        List<EventShortDto> eventShortDtos = compilation.getEvents().stream().map(eventMapper::toEventShortDtoFromEvent).toList();

        log.info("{}.{}: Mapping compilations to CompilationDto", colorizeClass("CompilationService"), colorizeMethod("getCompilationById()"));
        CompilationDto compilationDto = compilationMapper.toCompilationDtoFromCompilation(compilation, eventShortDtos);

        log.info("{}.{}: Successfully fetched compilation with id={}.", colorizeClass("CompilationService"), colorizeMethod("getCompilationById()"), compId);
        return compilationDto;
    }


    @Override
    @Transactional
    public CompilationDto create(NewCompilationDto newCompilationDto) {
        log.info("{}: Starting execution of {} method.", colorizeClass("CompilationService"), colorizeMethod("create()"));
        Set<Event> events = new HashSet<>();

        if (newCompilationDto.getEvents() != null) {
            log.info("{}.{}: Fetching events by ids={}.", colorizeClass("CompilationService"), colorizeMethod("create()"), newCompilationDto.getEvents());
            events.addAll(eventRepository.findAllById(newCompilationDto.getEvents()));
        }

        log.info("{}.{}: Mapping newCompilationDto to Compilation.", colorizeClass("CompilationService"), colorizeMethod("create()"));
        Compilation compilation = compilationMapper.toCompilationFromNewCompilationDto(newCompilationDto, events);

        log.info("{}.{}: Saving compilation to database.", colorizeClass("CompilationService"), colorizeMethod("create()"));
        compilation = compilationRepository.save(compilation);

        log.info("{}.{}: Mapping event to EventShortDto", colorizeClass("CompilationService"), colorizeMethod("create()"));
        List<EventShortDto> eventShortDtos = compilation.getEvents().stream().map(eventMapper::toEventShortDtoFromEvent).toList();

        log.info("{}.{}: Mapping compilations to CompilationDto", colorizeClass("CompilationService"), colorizeMethod("create()"));
        CompilationDto compilationDto = compilationMapper.toCompilationDtoFromCompilation(compilation, eventShortDtos);

        log.info("{}.{}: Successfully created compilation with id={}", colorizeClass("CompilationService"), colorizeMethod("create()"), compilation.getId());
        return compilationDto;
    }

    @Override
    @Transactional
    public void deleteById(Long compId) {
        log.info("{}: Starting execution of {} method.", colorizeClass("CompilationService"), colorizeMethod("deleteById()"));

        log.info("{}.{}: Checking if compilation exists with id={}.", colorizeClass("CompilationService"), colorizeMethod("deleteById()"), compId);
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException(
                    String.format("Compilation with id=%d not found", compId));
        }
        log.info("{}.{}: Deleting compilation with id={}.", colorizeClass("CompilationService"), colorizeMethod("deleteById()"), compId);
        compilationRepository.deleteById(compId);

        log.info("{}.{}: Compilation with id={} deleted successfully.", colorizeClass("UserService"), colorizeMethod("deleteById()"), compId);
    }

    @Override
    @Transactional
    public CompilationDto update(Long compId, UpdateCompilationRequest updateCompilationRequest) {
        log.info("{}: Starting execution of {} method.", colorizeClass("CompilationService"), colorizeMethod("update()"));

        log.info("{}.{}: Fetching compilation with id={}.", colorizeClass("CompilationService"), colorizeMethod("update()"), compId);
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException(String.format("Compilation with id=%d not found", compId)));

        log.info("{}.{}: Beginning updating the fields.", colorizeClass("CompilationService"), colorizeMethod("update()"));
        StringBuilder updatedFieldsLog = new StringBuilder();

        if (updateCompilationRequest.getTitle() != null) {
            compilation.setTitle(updateCompilationRequest.getTitle());
            updatedFieldsLog.append("Title|");
        }
        if (updateCompilationRequest.getEvents() != null) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(updateCompilationRequest.getEvents()));
            compilation.setEvents(events);
            updatedFieldsLog.append("Events|");
        }
        if (updateCompilationRequest.getPinned() != null) {
            compilation.setPinned(updateCompilationRequest.getPinned());
            updatedFieldsLog.append("Pinned|");
        }

        String updatedFields = updatedFieldsLog.toString().replaceAll("\\|$", "").replace("|", ", ");

        log.info("{}.{}: Updated fields: {}.", colorizeClass("CompilationService"), colorizeMethod("update()"), updatedFields);

        log.info("{}.{}: Saving updated compilation to database", colorizeClass("CompilationService"), colorizeMethod("update()"));
        compilation = compilationRepository.save(compilation);

        log.info("{}.{}: Mapping event to EventShortDto", colorizeClass("CompilationService"), colorizeMethod("update()"));
        log.info("{}.{}: Mapping compilations to CompilationDto", colorizeClass("CompilationService"), colorizeMethod("update()"));
        CompilationDto compilationDto = compilationMapper.toCompilationDtoFromCompilation(compilation, compilation.getEvents().stream()
                .map(eventMapper::toEventShortDtoFromEvent)
                .toList());

        log.info("{}.{}: Successfully updated compilation with id={}", colorizeClass("CompilationService"), colorizeMethod("update()"), compId);
        return compilationDto;
    }
}
