package com.example.CodeDuel.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class JudgeResponse {

    private String stdout;
    private String stderr;
    @JsonProperty("compile_output")
    private String compileOutput;
    private Float time;
    private Integer memory;
    private Status status;

    @Data
    public static class Status {
        private Integer id;
        private String description;
    }

    public boolean isAccepted() {
        return status != null && status.getId() == 3;
    }

    public boolean isCompileError() {
        return status != null && status.getId() == 6;
    }
}