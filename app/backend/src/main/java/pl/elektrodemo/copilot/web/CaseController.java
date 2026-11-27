package pl.elektrodemo.copilot.web;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pl.elektrodemo.copilot.cases.AssessmentService;
import pl.elektrodemo.copilot.cases.DecisionService;
import pl.elektrodemo.copilot.web.dto.AssessmentResponse;
import pl.elektrodemo.copilot.web.dto.CaseFormDto;
import pl.elektrodemo.copilot.web.dto.DecisionRequest;
import pl.elektrodemo.copilot.web.dto.DecisionResponse;
import pl.elektrodemo.copilot.web.dto.FieldErrorDto;

/** ADR-001 §5.1-5.2: thin HTTP adapter over the case use cases. */
@RestController
@RequestMapping("/api/cases")
public class CaseController {

    private final AssessmentService assessmentService;
    private final DecisionService decisionService;

    public CaseController(AssessmentService assessmentService, DecisionService decisionService) {
        this.assessmentService = assessmentService;
        this.decisionService = decisionService;
    }

    @PostMapping(path = "/assessments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AssessmentResponse assess(@RequestPart("form") @Valid CaseFormDto form,
            @RequestPart("photo") MultipartFile photo) throws IOException {
        return assessmentService.assess(form, photo.getBytes());
    }

    @PostMapping(path = "/decisions", consumes = MediaType.APPLICATION_JSON_VALUE)
    public DecisionResponse decide(@RequestBody DecisionRequest request) {
        List<FieldErrorDto> errors = new ArrayList<>();
        if (request.caseContext() == null) {
            errors.add(new FieldErrorDto("caseContext", "Brak danych zgłoszenia."));
        }
        if (request.signature() == null || request.signature().isBlank()) {
            errors.add(new FieldErrorDto("signature", "Brak podpisu zgłoszenia."));
        }
        if (!errors.isEmpty()) {
            throw new RequestValidationException(errors);
        }
        return decisionService.decide(request);
    }

    /** VALIDATION_FAILED raised by the controller itself (DecisionRequest carries no bean-validation constraints). */
    static final class RequestValidationException extends ApiException {

        private final List<FieldErrorDto> fieldErrors;

        RequestValidationException(List<FieldErrorDto> fieldErrors) {
            super(ErrorCode.VALIDATION_FAILED);
            this.fieldErrors = List.copyOf(fieldErrors);
        }

        List<FieldErrorDto> fieldErrors() {
            return fieldErrors;
        }
    }
}
