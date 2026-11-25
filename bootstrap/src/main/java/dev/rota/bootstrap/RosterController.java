package dev.rota.bootstrap;

import dev.rota.application.GetRoster;
import dev.rota.application.RepairSickCall;
import dev.rota.application.Roster;
import dev.rota.application.SolveRoster;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/** Roster endpoints (docs/contracts/openapi/rota.yaml). Thin: parameters in, use case, DTO out. */
@RestController
@RequestMapping("/api/rosters")
public class RosterController {

    private final SolveRoster solveRoster;
    private final RepairSickCall repairSickCall;
    private final GetRoster getRoster;

    public RosterController(SolveRoster solveRoster, RepairSickCall repairSickCall, GetRoster getRoster) {
        this.solveRoster = solveRoster;
        this.repairSickCall = repairSickCall;
        this.getRoster = getRoster;
    }

    @PostMapping("/solve")
    public ResponseEntity<RosterResponse> solve(@RequestParam("fixture") String fixture) {
        return created(solveRoster.solve(fixture));
    }

    @PostMapping("/repair")
    public ResponseEntity<RosterResponse> repair(@RequestParam("fixture") String fixture) {
        return created(repairSickCall.repair(fixture));
    }

    @GetMapping("/{id}")
    public RosterResponse get(@PathVariable("id") String id) {
        return RosterResponse.from(getRoster.get(id));
    }

    private static ResponseEntity<RosterResponse> created(Roster roster) {
        return ResponseEntity.created(URI.create("/api/rosters/" + roster.id())).body(RosterResponse.from(roster));
    }
}
