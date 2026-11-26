package pl.elektrodemo.copilot.prescreen;

import java.util.List;

public record GateResult(GateStatus status, List<String> reasons) {
    public GateResult {
        reasons = List.copyOf(reasons);
    }
}
