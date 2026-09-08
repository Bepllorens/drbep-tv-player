package com.drbep.tvplayer;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

final class BackendTransportFailurePolicy {
    private BackendTransportFailurePolicy() {
    }

    static boolean isTransportFailure(Throwable error) {
        Throwable current = error;
        int depth = 0;
        while (current != null && depth < 12) {
            if (current instanceof ConnectException
                    || current instanceof NoRouteToHostException
                    || current instanceof SocketTimeoutException
                    || current instanceof UnknownHostException
                    || current instanceof SocketException) {
                return true;
            }
            Throwable cause = current.getCause();
            if (cause == current) {
                break;
            }
            current = cause;
            depth++;
        }
        return false;
    }
}
