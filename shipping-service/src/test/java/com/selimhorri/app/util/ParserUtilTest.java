package com.selimhorri.app.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.selimhorri.app.exception.custom.InvalidInputException;

public class ParserUtilTest {

    @Test
    void parseId_valid() {
        Integer v = ParserUtil.parseId("  42 ", "testId");
        assertEquals(42, v);
    }

    @Test
    void parseId_invalid_throws() {
        assertThrows(InvalidInputException.class, () -> ParserUtil.parseId("not-a-number", "testId"));
    }
}
