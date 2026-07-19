package com.stadiummate.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameCollectRequest {

    @JsonProperty("userId")
    private String userId;

    @NotBlank(message = "nodeId must not be blank")
    @JsonProperty("nodeId")
    private String nodeId;

    @NotBlank(message = "itemType must not be blank")
    @JsonProperty("itemType")
    private String itemType;
}
