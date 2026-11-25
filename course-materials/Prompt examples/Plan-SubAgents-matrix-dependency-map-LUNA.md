# Implementation plan for the whole application from the PRD and ADRs (Haiku + OpenCode with Luna 6)

Create a reviewable implementation plan to implement the whole application (PoC or MVP) defined by the approved PRD and ADRs. One plan contains every phase and detail needed to deliver it fully, so a capable agent can implement it autonomously with good specs and answers to its questions; I may still run it phase by phase with my review between phases. Plan only selected phases when I explicitly ask for a part. Do not implement it or launch agents yet. Follow the applicable AGENTS.md and use the approved PRD, ADRs and design guidance in this application's repository. Ask for their paths only when they are missing or ambiguous; teaching examples in course-materials are not project requirements.

Inspect the current code, working-tree status, entry points and tests. Report what actually exists, the commands you verified and any baseline failure. Use the existing application scaffold or approved legacy checkout; do not replace it with a fresh application. Resolve consequential gaps with targeted questions, without a minimum question count. Keep unrelated work intact.

Save the draft in docs/plans/<plan-name>-implementation-plan.md, unless I named another destination. In a read-only Plan mode, return the complete draft and destination first, then save it when writing is permitted. Mark it as draft awaiting my review. Link exact PRD criteria, ADR sections and design guidance instead of copying their contents. Missing architecture belongs in a proposed ADR, not an invented plan assumption. Before saving, check the plan, PRD, ADRs and AGENTS.md against each other and list every contradiction or ambiguity that could stop an agent or leave it unsure what to do.

Use small verified phases and one authoritative dependency matrix:

| Task | Observable outcome | PRD/ADR links | Prerequisites | Owner/model | Editable files | Verification and handoff | Status |
|---|---|---|---|---|---|---|---|

Give each row a bounded deliverable, acceptance evidence and commit boundary, and define a Definition of Done for the whole plan. Plan a failing behavioral test before implementation, integration checks after dependent components, manual runtime verification, an independent review and a pull request for each finished phase or for the whole application, as the repository's policy prefers. End-to-end and manual tests use the real application, its endpoints and any external, LLM or decision-model calls, never mocks. When unit or integration tests mock external endpoints or APIs, base those mocks on real recorded calls, not on documentation. Use the application's actual commands. A green build alone does not prove startup or the user flow. State the critical path, shared-file hotspots, failure/rollback handling and a smaller fallback if time, tools or model access run out. Track execution status in the plan's Status column. When the repository uses GitHub Issues, check your access during planning; after I approve the plan, create one issue per task and use the same issue ID in the plan and in your task list.

The strong orchestrator makes architectural decisions, resolves contradictions, prepares briefs and accepts results. Prefer cheap small workers for bounded implementation and test-writing: Claude Code subagents on Haiku. Delegate to external models within OpenCode using the headless OpenCode CLI mode. Use the Luna 6 model with OpenRouter (`openrouter/openai/gpt-6-luna`). Assign each row explicitly to a Haiku subagent or to OpenCode with Luna 6 and say why. Also add a brief instruction to the plan on how to use OpenCode in headless mode: the command, the model flag, how the brief is passed, how permissions are granted and where the output is logged. Record the chosen model, provider and budget constraint; escalate when evidence shows the task exceeds the worker's ability. Review and final acceptance stay with a capable model and the human.

Recommend ad hoc subagents by default for a one-off task: an exact brief matters more than a new role file. Use short custom definitions only for a recurring role with reusable tool restrictions or expertise. Existing named roles may be reused after inspecting them, without assuming their model or permissions suit this task. Verify the active harness supports the requested agent mechanism; otherwise execute the same matrix sequentially with one agent. Do not manufacture custom roles merely to match a tutorial.

Parallelize only tasks with independent inputs and disjoint editable files. Tell every worker which other work is concurrent. Serialize shared contracts, lockfiles, migrations and integration. Use one checkout by default, with disjoint files/folders and a dependency plan. Request a worktree only when agents must edit the same files in parallel; follow the repository approval policy and remove it afterwards. Worktrees do not eliminate same-file merge conflicts. Only the orchestrator switches branches, stages changes, writes the Git index or creates commits in the shared checkout; workers edit only their assigned files and return a diff.

After I authorize execution, compile each task's self-contained brief just before dispatch and save it as docs/.agent-briefs/<plan-name>-<brief-name>.md (a brief without a plan is docs/.agent-briefs/<task>.md). Include the relevant source excerpts, current contracts, predecessor outputs, exact editable files, ordered steps, test inputs and expected results. Link the authoritative documents for traceability, but do not make a small worker rediscover the whole architecture. The worker reports contradictions and observed results; it cannot change the agreed scope or accept its own work. The orchestrator reads the diff and reproduces the relevant checks before integrating and committing under the repository's policy. Commit the plan and briefs while the work runs and keep worker logs out of Git. Delete the plan and briefs once the plan's Definition of Done is met (for example merge or production deploy), because leftover plans distract agents; Git history keeps them.

Finish with the saved plan link, unresolved decisions, the first bounded task and the review gate. Planning does not authorize execution, new agents, publication or merge.

---

Copy the prompt above. Use the sections below once the draft plan is ready.

## Exercise

Follow the plan corrections, the planning-skill prompt and the implementation step in [Plan-SubAgents-matrix-dependency-map.md](Plan-SubAgents-matrix-dependency-map.md).

## OpenCode in headless mode

```bash
opencode run -m openrouter/openai/gpt-6-luna -f docs/.agent-briefs/<plan-name>-<brief-name>.md "Do the task described in the attached brief." </dev/null 2>&1 | tee .agent-logs/<plan-name>-<brief-name>.log
```

Add the OpenRouter key with `/connect` first; `opencode models` lists the model IDs. Add `.agent-logs/` to `.gitignore`. A headless run cannot answer permission prompts, so allow the needed tools in the OpenCode configuration; `--auto` approves every permission that is not explicitly denied, so use it only in a sandbox you trust. `--format json` gives machine-readable output, and `-c` or `-s <id>` continues a session.
