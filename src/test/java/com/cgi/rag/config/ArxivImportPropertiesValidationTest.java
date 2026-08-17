package com.cgi.rag.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ArxivImportPropertiesValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsDefaultConfigurationShape() {
        var properties = new ArxivImportProperties(List.of("cs.CL", "cs.AI", "cs.LG"), 50_000);
        assertThat(validator.validate(properties)).isEmpty();
    }

    @Test
    void acceptsNullLimitAsUnlimited() {
        var properties = new ArxivImportProperties(List.of("cs.CL"), null);
        assertThat(validator.validate(properties)).isEmpty();
        assertThat(properties.isUnlimited()).isTrue();
    }

    @Test
    void rejectsEmptyCategoryList() {
        var properties = new ArxivImportProperties(List.of(), 50_000);
        assertThat(validator.validate(properties)).isNotEmpty();
    }

    @Test
    void rejectsZeroLimit() {
        var properties = new ArxivImportProperties(List.of("cs.CL"), 0);
        assertThat(validator.validate(properties)).isNotEmpty();
    }

    @Test
    void rejectsNegativeLimit() {
        var properties = new ArxivImportProperties(List.of("cs.CL"), -1);
        assertThat(validator.validate(properties)).isNotEmpty();
    }
}
