package pl.elektrodemo.copilot.prescreen;

import java.util.List;

public record PrescreenResult(
        PrescreenStatus status,
        List<PrescreenAnswer> photoAnswers,
        List<PrescreenAnswer> consistencySignals,
        GateStatus gate,
        List<String> gateReasons,
        String catalogVersion,
        long latencyMs,
        double cost,
        String model,
        String unavailableReason) {

    public PrescreenResult {
        photoAnswers = photoAnswers == null ? List.of() : List.copyOf(photoAnswers);
        consistencySignals = consistencySignals == null ? List.of() : List.copyOf(consistencySignals);
        gateReasons = gateReasons == null ? List.of() : List.copyOf(gateReasons);
    }

    public static PrescreenResult unavailable(String reason, String catalogVersion, long latencyMs) {
        return new PrescreenResult(PrescreenStatus.UNAVAILABLE, List.of(), List.of(), GateStatus.UNCERTAIN,
                List.of(reason), catalogVersion, latencyMs, 0, null, reason);
    }
}
