package com.url_shortener.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.validator.constraints.URL;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortenRequest {

    @NotBlank(message = "url must not be blank")
    @URL(message = "url must be a valid URL")
    @Size(max = 2048, message = "url must be at most 2048 characters")
    private String url;

    @Pattern(
            regexp = "^[a-zA-Z0-9_-]{3,16}$",
            message = "customAlias must be 3-16 chars of letters, digits, underscore or hyphen"
    )
    private String customAlias;

    @Future(message = "expiresAt must be in the future")
    private OffsetDateTime expiresAt;
}
