package ru.practicum.mainservice.user.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private Long id;
    @NotNull
    @NotEmpty
    @NotBlank
    @Size(min = 2, max = 250)
    private String name;
    @NotNull
    @NotEmpty
    @NotBlank
    @Email
    @Size(min = 6, max = 254)
    private String email;
}
