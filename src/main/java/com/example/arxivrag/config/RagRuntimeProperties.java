package com.example.arxivrag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag")
public class RagRuntimeProperties {

    private Runtime runtime = new Runtime();

    private Features features = new Features();

    public Runtime getRuntime() {
        return runtime;
    }

    public void setRuntime(Runtime runtime) {
        this.runtime = runtime;
    }

    public Features getFeatures() {
        return features;
    }

    public void setFeatures(Features features) {
        this.features = features;
    }

    public static class Runtime {

        private String profile = "local";

        public String getProfile() {
            return profile;
        }

        public void setProfile(String profile) {
            this.profile = profile;
        }
    }

    public static class Features {

        private boolean importEnabled;

        private boolean papersEnabled;

        private boolean searchEnabled;

        private boolean chatEnabled;

        private boolean vectorStoresEnabled;

        private boolean benchmarksEnabled;

        public boolean isImportEnabled() {
            return importEnabled;
        }

        public void setImportEnabled(boolean importEnabled) {
            this.importEnabled = importEnabled;
        }

        public boolean isPapersEnabled() {
            return papersEnabled;
        }

        public void setPapersEnabled(boolean papersEnabled) {
            this.papersEnabled = papersEnabled;
        }

        public boolean isSearchEnabled() {
            return searchEnabled;
        }

        public void setSearchEnabled(boolean searchEnabled) {
            this.searchEnabled = searchEnabled;
        }

        public boolean isChatEnabled() {
            return chatEnabled;
        }

        public void setChatEnabled(boolean chatEnabled) {
            this.chatEnabled = chatEnabled;
        }

        public boolean isVectorStoresEnabled() {
            return vectorStoresEnabled;
        }

        public void setVectorStoresEnabled(boolean vectorStoresEnabled) {
            this.vectorStoresEnabled = vectorStoresEnabled;
        }

        public boolean isBenchmarksEnabled() {
            return benchmarksEnabled;
        }

        public void setBenchmarksEnabled(boolean benchmarksEnabled) {
            this.benchmarksEnabled = benchmarksEnabled;
        }
    }
}
