package com.yan.freshfood.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "物流轨迹")
public class LogisticsVO {

    @Schema(description = "快递公司")
    private String company;

    @Schema(description = "快递单号")
    private String trackingNo;

    @Schema(description = "当前物流状态文字")
    private String statusText;

    @Schema(description = "物流轨迹（时间倒序）")
    private List<Trace> traces;

    @Data
    @Schema(description = "物流轨迹点")
    public static class Trace {

        @Schema(description = "时间")
        private LocalDateTime time;

        @Schema(description = "轨迹描述")
        private String desc;
    }
}