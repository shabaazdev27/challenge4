package com.stadiummate.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrowdSimulateRequest {

    @NotBlank(message = "zoneId must not be blank")
    @JsonProperty("zoneId")
    private String zoneId;

    @NotNull(message = "level must not be null")
    @Min(value = 0, message = "level must be at least 0.0")
    @Max(value = 1, message = "level must be at most 1.0")
    @JsonProperty("level")
    private Double level;
}
