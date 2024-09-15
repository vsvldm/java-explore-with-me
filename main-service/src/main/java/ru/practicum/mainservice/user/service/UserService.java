package ru.practicum.mainservice.user.service;

import ru.practicum.mainservice.user.dto.UserDto;

import java.util.List;

public interface UserService {
    UserDto create(UserDto userDto);

    List<UserDto> findAll(List<Long> ids, Integer from, Integer size);

    void deleteById(Long userId);
}
