package com.yan.freshfood.user.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class LogisticsVO {
    private String company;
    private String trackingNo;
    private String statusText;
    private List<Trace> traces;

    @Data
    public static class Trace {
        private LocalDateTime time;
        private String desc;
    }
}