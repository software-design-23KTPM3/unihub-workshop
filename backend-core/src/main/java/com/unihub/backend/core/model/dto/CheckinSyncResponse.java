package com.unihub.backend.core.model.dto;

import java.util.ArrayList;
import java.util.List;

public class CheckinSyncResponse {
    private int total;
    private int success;
    private int failed;
    private List<CheckinResult> results = new ArrayList<>();

    public CheckinSyncResponse() {
    }

    public CheckinSyncResponse(int total, int success, int failed, List<CheckinResult> results) {
        this.total = total;
        this.success = success;
        this.failed = failed;
        this.results = results;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getSuccess() {
        return success;
    }

    public void setSuccess(int success) {
        this.success = success;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public List<CheckinResult> getResults() {
        return results;
    }

    public void setResults(List<CheckinResult> results) {
        this.results = results;
    }

    public static CheckinSyncResponseBuilder builder() {
        return new CheckinSyncResponseBuilder();
    }

    public static class CheckinSyncResponseBuilder {
        private int total;
        private int success;
        private int failed;
        private List<CheckinResult> results = new ArrayList<>();

        public CheckinSyncResponseBuilder total(int total) {
            this.total = total;
            return this;
        }

        public CheckinSyncResponseBuilder success(int success) {
            this.success = success;
            return this;
        }

        public CheckinSyncResponseBuilder failed(int failed) {
            this.failed = failed;
            return this;
        }

        public CheckinSyncResponseBuilder results(List<CheckinResult> results) {
            this.results = results;
            return this;
        }

        public CheckinSyncResponse build() {
            return new CheckinSyncResponse(total, success, failed, results);
        }
    }
}
