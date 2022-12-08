module com.github.lc.oss.commons.identity {
    requires transitive com.github.lc.oss.commons.api.identity;
    requires transitive com.github.lc.oss.commons.api.services;
    requires com.github.lc.oss.commons.util;
    requires com.github.lc.oss.commons.encoding;
    requires com.github.lc.oss.commons.signing;

    requires transitive org.apache.httpcomponents.client5.httpclient5;
    requires transitive org.apache.httpcomponents.core5.httpcore5;

    exports com.github.lc.oss.commons.identity;
    exports com.github.lc.oss.commons.identity.model;
}
