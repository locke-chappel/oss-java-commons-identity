module com.github.lc.oss.commons.identity {
    requires transitive com.github.lc.oss.commons.api.identity;
    requires transitive com.github.lc.oss.commons.api.services;
    requires com.github.lc.oss.commons.util;
    requires com.github.lc.oss.commons.encoding;
    requires com.github.lc.oss.commons.signing;

    requires org.apache.httpcomponents.httpclient;
    requires org.apache.httpcomponents.httpcore;

    exports com.github.lc.oss.commons.identity;
    exports com.github.lc.oss.commons.identity.model;
}
