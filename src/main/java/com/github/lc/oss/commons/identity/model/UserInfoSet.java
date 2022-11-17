package com.github.lc.oss.commons.identity.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.github.lc.oss.commons.api.identity.ApiObjectCollection;
import com.github.lc.oss.commons.serialization.JsonableHashSet;

@JsonInclude(Include.NON_EMPTY)
public class UserInfoSet extends JsonableHashSet<UserInfo> implements ApiObjectCollection<UserInfo> {
    private static final long serialVersionUID = -1845009342982092876L;
}
