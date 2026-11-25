# Implementation plan for the whole application from the PRD and ADRs

Create a reviewable implementation plan to implement the whole application (PoC or MVP) defined by the approved PRD and ADRs. One plan contains every phase and detail needed to deliver it fully; plan only selected phases when I explicitly ask for a part. Do not implement it or launch agents yet. Follow the applicable AGENTS.md and use the approved PRD, ADRs and design guidance in this application's repository. Ask for their paths only when they are missing or ambiguous; teaching examples in course-materials are not project requirements.

Inspect the current code, working-tree status, entry points and tests. Report what actually exists, the commands you verified and any baseline failure. Use the existing application scaffold or approved legacy checkout; do not replace it with a fresh application. Resolve consequential gaps with targeted questions, without a minimum question count. Keep unrelated work intact.

Save the draft in docs/plans/<feature>-implementation-plan.md, unless I named another destination. In a read-only Plan mode, return the complete draft and destination first, then save it when writing is permitted. Mark it as draft awaiting my review. Link exact PRD criteria, ADR sections and design guidance instead of copying their contents. Missing architecture belongs in a proposed ADR, not an invented plan assumption. Before saving, check the plan, PRD, ADRs and AGENTS.md against each other and list every contradiction or ambiguity that could stop an agent or leave it unsure what to do.

Use small verified phases and one authoritative dependency matrix:

| Task | Observable outcome | PRD/ADR links | Prerequisites | Owner/model | Editable files | Verification and handoff | Status |
|---|---|---|---|---|---|---|---|

Give each row a bounded deliverable, acceptance evidence and commit boundary. Plan a failing behavioral test before implementation, integration checks after dependent components, manual runtime verification, an independent review and a pull request for each finished phase or for the whole application, as the repository's policy prefers. End-to-end and manual tests use the real application, endpoints, LLM and decision-model calls, never mocks. Unit and integration tests may use mocks only when they are recorded from real endpoint calls, not written from documentation. Use the application's actual commands. A green build alone does not prove startup or the user flow. State the critical path, shared-file hotspots, failure/rollback handling and a smaller fallback if time, tools or model access run out. Track execution status in the plan's Status column; when the repository uses GitHub Issues and you have access, also create one issue per task and use the same issue ID in the plan and in your task list.

The strong orchestrator makes architectural decisions, resolves contradictions, prepares briefs and accepts results. Prefer cheap small workers for bounded implementation and test-writing, chosen from what is actually available here, for example:
- Claude Code subagents on Haiku;
- OpenCode in headless mode (`opencode run`), with one of its free built-in models or a provider added with /connect, such as OpenRouter;
- Codex with a small model or GitHub Copilot with the cheapest suitable model, only where the organization provides them; check the model picker and current pricing instead of assuming an old list.
Record the chosen model, provider and budget constraint; escalate when evidence shows the task exceeds the worker's ability. When you delegate to a CLI agent, add a short note to the plan on how to run it headless: the command, the model flag, how the brief is passed and where the output is logged. Review and final acceptance stay with a capable model and the human.

Recommend ad hoc subagents by default for a one-off task: an exact brief matters more than a new role file. Use short custom definitions only for a recurring role with reusable tool restrictions or expertise. Existing named roles may be reused after inspecting them, without assuming their model or permissions suit this task. Verify the active harness supports the requested agent mechanism; otherwise execute the same matrix sequentially with one agent. Do not manufacture custom roles merely to match a tutorial.

Parallelize only tasks with independent inputs and disjoint editable files. Tell every worker which other work is concurrent. Serialize shared contracts, lockfiles, migrations and integration. Use one checkout by default, with disjoint files/folders and a dependency plan. Request a worktree only when agents must edit the same files in parallel; follow the repository approval policy and remove it afterwards.

After I authorize execution, compile each task's self-contained brief just before dispatch and save it as docs/plans/.briefs/<plan-name>-<brief-name>.md. Include the relevant source excerpts, current contracts, predecessor outputs, exact editable files, ordered steps, test inputs and expected results. Link the authoritative documents for traceability, but do not make a small worker rediscover the whole architecture. The worker reports contradictions and observed results; it cannot change the agreed scope or accept its own work. The orchestrator reads the diff and reproduces the relevant checks before integrating and committing under the repository's policy. Commit the plan and briefs while the work runs; delete them once the plan's Definition of Done is met (for example merge or production deploy), because leftover plans distract agents. Git history keeps them.

Finish with the saved plan link, unresolved decisions, the first bounded task and the review gate. Planning does not authorize execution, new agents, publication or merge.

## Exercise: correct the plan, then turn the session into your own planning skill

Work through these steps in order, in the same session that wrote the plan.

1. **Whole application.** Check that the plan covers the whole application from the PRD in phases, not one slice. Ask for parts only when you decide to implement step by step.
2. **Consistency review.** Ask the agent to check every document before implementation:

   ```text
   Check the correctness of all documents created for implementing this plan: the plan itself, the PRD, the ADRs and the agent instructions (AGENTS.md). Check that they are correct and consistent, and identify any contradictions or elements that could prevent the plan from being implemented properly or leave the agent unsure what to do.
   ```

3. **Real tests.** Make sure the plan does not mock end-to-end or manual tests:

   ```text
   Check that all end-to-end and manual tests use the real application: every endpoint, LLM call and decision-making method must be exercised for real, without mocks. Mocks in unit and integration tests must be based on real endpoint calls, not on documentation. Correct the plan where it does not.
   ```

4. **What the documents should say.** Review together whether the PRD, ADRs and plan state the problem the application solves, its users, its scale (small company, internal app, PoC, enterprise, global startup), the company's specifics (for example a regulated sector: government, finance, military, medical) and diagrams such as UML in Mermaid.
5. **Briefs and plan lifecycle.** The plan tells the orchestrator to save every brief as `docs/plans/.briefs/<plan-name>-<brief-name>.md`, to commit plans and briefs during the work, and to delete them after the Definition of Done.
6. **Tracking.** Status lives in the plan's Status column. Optionally the same tasks become GitHub Issues, and the plan and the agent's task list use their IDs.
7. **Tools.** Delegation must use agents you actually have. OpenCode is always available: its free models work right after installation, and `/connect` adds a provider such as OpenRouter. If pnpm is missing, npm is fine.
8. **Execution.** Start a new session (`/new`) and run `/goal implement this plan: @<path-to-plan>.md`. `/status` shows when the usage limit resets; a monitor can resume the work after the limit.
9. **Instructions.** Check which `AGENTS.md` levels apply: organization, user in the cloud, local user, project root, modules and folders.

Then create a planning skill from this session, so the next plan starts right:

```text
Create a reusable planning skill from this session. Read the whole session history: the plan prompt, every correction and decision I gave, every mistake you made or had to fix, and the final plan. Turn them into rules so the next plan is right the first time. The skill must: plan the whole application from the PRD and ADRs by default (parts only on request); review the plan, PRD, ADRs and AGENTS.md for contradictions before saving; forbid mocks in end-to-end and manual tests and base unit/integration mocks on real endpoint calls; keep one dependency matrix with a Status column and optional GitHub Issue IDs; choose workers from the agents actually available (OpenCode headless as the always-available option) and document the headless command; save briefs in docs/plans/.briefs/, commit them during the work and delete the plan and briefs after the Definition of Done; stop for my review before execution. Keep it generic: no domain, client names or decisions from this application. Save it as a project skill, show it to me, then test it on a second small feature and report what to improve.
```

Test the skill on a second small feature: does it find the real sources, ask only missing questions, avoid overlapping files and stop before implementation? Refine it from the observed result.

Optional external example for comparison: [write-implementation-plan, public PR #28](https://github.com/EdukeyTeam/agent-toolbox/pull/28). It is a pending proposal, not a required installation or an approved dependency of this exercise. Inspect its current state and compare its plan/brief separation with your own skill.

## OpenCode in headless mode

```bash
opencode run -m openrouter/openai/gpt-6-luna "$(cat docs/plans/.briefs/<plan-name>-<brief-name>.md)" </dev/null 2>&1 | tee docs/plans/.briefs/<plan-name>-<brief-name>.log
```

`opencode models` lists the model IDs, including the free `opencode/...` models. `--format json` gives machine-readable output, `-c` or `-s <id>` continues a session and `-f <file>` attaches a file. `--auto` approves every permission that is not explicitly denied; use it only in a sandbox you trust.

Only the coordinator may switch branches, stage changes, write the Git index or create commits in the shared checkout. Workers edit only their assigned files and return a diff; they do not run Git writes. Worktrees do not eliminate same-file merge conflicts.
