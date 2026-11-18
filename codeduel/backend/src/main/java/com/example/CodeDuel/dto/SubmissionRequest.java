package com.example.CodeDuel.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionRequest {

    @JsonProperty("battleId")
    @JsonAlias({"battle_id", "battleID"})
    private String battleId;

    @JsonProperty("userId")
    @JsonAlias({"user_id", "userID"})
    private String userId;

    @JsonProperty("language")
    @JsonAlias({"lang"})
    private String language; // "python", "java", "cpp"

    @JsonProperty("languageId")
    @JsonAlias({"language_id", "languageID"})
    private Integer languageId;

    @JsonProperty("sourceCode")
    @JsonAlias({"source_code", "code", "source"})
    private String sourceCode;

    @JsonProperty("sampleRun")
    private boolean sampleRun;
}
