package me.sonam.authzmanager.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @param number  page number
 * @param size  page size
 * @param totalElements total elements of all
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CustomRestPage<T>(List<T> content, int number, int size, long totalElements, int totalPages) {
    private static final Logger LOG = LoggerFactory.getLogger(CustomRestPage.class);

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public CustomRestPage(@JsonProperty("content") List<T> content, int number, int size, long totalElements, int totalPages) {
        this.number = number; //page number
        this.size = size; //page size
        this.content = content;

        this.totalElements = totalElements; //total amount of elements
        this.totalPages = totalPages;
    }

    public boolean isEmpty() {
        return content.isEmpty();
    }
}
