module io.github.lc.oss.commons.identity {
    requires transitive io.github.lc.oss.commons.api.identity;
    requires transitive io.github.lc.oss.commons.api.services;
    requires io.github.lc.oss.commons.util;
    requires io.github.lc.oss.commons.encoding;
    requires io.github.lc.oss.commons.signing;

    requires transitive org.apache.httpcomponents.client5.httpclient5;
    requires transitive org.apache.httpcomponents.core5.httpcore5;

    exports io.github.lc.oss.commons.identity;
    exports io.github.lc.oss.commons.identity.model;
}
