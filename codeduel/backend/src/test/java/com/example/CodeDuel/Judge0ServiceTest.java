package com.example.CodeDuel;

import com.example.CodeDuel.service.Judge0Service;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Judge0ServiceTest {

    private final Judge0Service judge0Service = new Judge0Service();

    @Test
    void mapsSupportedLanguageNamesAndIds() {
        assertEquals(71, judge0Service.getLanguageId("python"));
        assertEquals(71, judge0Service.getLanguageId("Python 3"));
        assertEquals(71, judge0Service.getLanguageId("71"));
        assertEquals(62, judge0Service.getLanguageId("java"));
        assertEquals(62, judge0Service.getLanguageId("Java 17"));
        assertEquals(62, judge0Service.getLanguageId("62"));
        assertEquals(54, judge0Service.getLanguageId("cpp"));
        assertEquals(54, judge0Service.getLanguageId("C++ 17"));
        assertEquals(54, judge0Service.getLanguageId("54"));
    }
}
