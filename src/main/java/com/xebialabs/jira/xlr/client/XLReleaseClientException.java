package com.xebialabs.jira.xlr.client;

public class XLReleaseClientException extends Exception {
    public XLReleaseClientException() {
    }

    public XLReleaseClientException(final String message) {
        super(message);
    }

    public XLReleaseClientException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public XLReleaseClientException(final Throwable cause) {
        super(cause);
    }

    public XLReleaseClientException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
