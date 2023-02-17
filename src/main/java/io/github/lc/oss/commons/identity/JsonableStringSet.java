package io.github.lc.oss.commons.identity;

import java.util.Collection;
import java.util.HashSet;

import io.github.lc.oss.commons.serialization.Jsonable;

class JsonableStringSet extends HashSet<String> implements Jsonable {
    private static final long serialVersionUID = -4019153782493444041L;

    public JsonableStringSet(Collection<String> strings) {
        super(strings);
    }
}
