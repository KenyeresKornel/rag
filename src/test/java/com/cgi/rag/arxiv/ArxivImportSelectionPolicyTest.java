package com.cgi.rag.arxiv;

import com.cgi.rag.config.ArxivImportProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArxivImportSelectionPolicyTest {

    @Test
    void allowsRecordsUntilConfiguredMatchingRecordLimitIsReached() {
        var policy = new ArxivImportSelectionPolicy(
                new ArxivImportProperties(List.of("cs.CL"), 3)
        );

        assertThat(policy.canAcceptAnother(0)).isTrue();
        assertThat(policy.canAcceptAnother(1)).isTrue();
        assertThat(policy.canAcceptAnother(2)).isTrue();
        assertThat(policy.canAcceptAnother(3)).isFalse();
    }

    @Test
    void nullMaxRecordsMeansUnlimited() {
        var policy = new ArxivImportSelectionPolicy(
                new ArxivImportProperties(List.of("cs.CL"), null)
        );

        assertThat(policy.isUnlimited()).isTrue();
        assertThat(policy.canAcceptAnother(Integer.MAX_VALUE)).isTrue();
    }

    @Test
    void rejectsNegativeAcceptedRecordCount() {
        var policy = new ArxivImportSelectionPolicy(
                new ArxivImportProperties(List.of("cs.CL"), 3)
        );

        assertThatThrownBy(() -> policy.canAcceptAnother(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
