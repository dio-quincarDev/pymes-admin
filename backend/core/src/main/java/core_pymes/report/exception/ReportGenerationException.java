package core_pymes.report.exception;

import core_pymes.common.exception.CodigoError;
import core_pymes.common.exception.CoreApiException;

public class ReportGenerationException extends CoreApiException {
    public ReportGenerationException(String message) {
        super(CodigoError.REPORT_GENERATION, message);
    }

    public ReportGenerationException(String message, Throwable cause) {
        super(CodigoError.REPORT_GENERATION, message);
        initCause(cause);
    }
}
