package com.enda.wallet.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResult {
    private int totalLines;
    private List<ErrorLine> errors;
    private boolean hasErrors;
    private int errorCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorLine {
        private int line;
        private List<String> errors;
        private Map<String, Object> data;
    }
}