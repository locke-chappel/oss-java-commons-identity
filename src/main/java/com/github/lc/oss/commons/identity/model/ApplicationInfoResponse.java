package com.github.lc.oss.commons.identity.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public class ApplicationInfoResponse extends ApiResponse<ApplicationInfo> {
}
