package me.sonam.authzmanager.controller.admin.oauth2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Set;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
abstract class HashSetMixin {

    @JsonCreator
    HashSetMixin(Set<?> set) {
    }

}
