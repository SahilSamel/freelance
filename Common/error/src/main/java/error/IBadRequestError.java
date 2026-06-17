package error;

public interface IBadRequestError extends IError {

    @Override
    default int getStatusCode() {
        return 400;
    }
}