package com.yan.freshfood.common.response;

import com.yan.freshfood.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RTest {

    @Test
    void ok_withData_returnsSuccessResponse() {
        R<String> r = R.ok("hello");
        assertEquals(0, r.getCode());
        assertEquals("ok", r.getMessage());
        assertEquals("hello", r.getData());
    }

    @Test
    void fail_withErrorCode_returnsFailureResponse() {
        R<Void> r = R.fail(ErrorCode.USER_NOT_FOUND);
        assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), r.getCode());
        assertEquals(ErrorCode.USER_NOT_FOUND.getMessage(), r.getMessage());
        assertNull(r.getData());
    }

    @Test
    void ok_noData_returnsSuccessWithoutData() {
        R<Void> r = R.ok();
        assertEquals(0, r.getCode());
        assertNull(r.getData());
    }
}