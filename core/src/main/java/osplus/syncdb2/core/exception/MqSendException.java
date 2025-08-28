package osplus.syncdb2.core.exception;

/**
 * Zweck
 * -----
 * Signalisierung eines Fehlers beim synchronen Aufruf von SEND_TO_MQ.
 * Enthält den gemeldeten Statuscode und ggf. eine Fehlermeldung der SP.
 */
public class MqSendException extends RuntimeException {

    private final int status;

    public MqSendException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
