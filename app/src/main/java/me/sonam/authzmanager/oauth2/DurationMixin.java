package me.sonam.authzmanager.oauth2;

import com.fasterxml.jackson.annotation.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
abstract class DurationMixin {

    @JsonCreator
    static void ofSeconds(@JsonProperty("seconds") long seconds, @JsonProperty("nano") long nanoAdjustment) {
    }

    @JsonGetter("seconds")
    abstract long getSeconds();

    @JsonGetter("nano")
    abstract int getNano();

}
