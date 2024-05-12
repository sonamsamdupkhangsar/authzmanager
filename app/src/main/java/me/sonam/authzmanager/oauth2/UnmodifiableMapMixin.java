package me.sonam.authzmanager.oauth2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Map;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonDeserialize(using = UnmodifiableMapDeserializer.class)
abstract class UnmodifiableMapMixin {

    @JsonCreator
    UnmodifiableMapMixin(Map<?, ?> map) {
    }

}
