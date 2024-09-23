package ru.practicum.mainservice.user.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.mainservice.user.dto.UserDto;
import ru.practicum.mainservice.user.dto.UserShortDto;
import ru.practicum.mainservice.user.model.User;

@Component
public class UserMapper {
    public User toUser(UserDto userDto) {
        return User.builder()
                .name(userDto.getName())
                .email(userDto.getEmail())
                .rating(userDto.getRating())
                .build();
    }

    public UserDto toUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .rating(user.getRating())
                .build();
    }

    public UserShortDto toUserShortDto(User user) {
        return UserShortDto.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }
}
