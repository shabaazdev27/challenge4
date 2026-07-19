package com.stadiummate.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherSimulateRequest {

    @NotNull(message = "isRaining must not be null")
    @JsonProperty("isRaining")
    private Boolean isRaining;
}
