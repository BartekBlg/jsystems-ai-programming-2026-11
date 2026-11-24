# Agent automation with Azure DevOps

Checked 2026-11-24 against official documentation. [Research and source details](../../Research/azure-devops-agent-automation-2026-11-24.md).

Yes, Claude Code and Codex can read Azure Boards work items, push Git branches and open Azure Repos pull requests. Use `git` for local changes and pushes, `az boards work-item` for tickets and `az repos pr` for PRs. `az repos pr` supports `create`, `list`, `show`, `update`, `checkout`, `reviewer add/list/remove` and `set-vote`. PR threads use REST. [Azure Repos CLI](https://learn.microsoft.com/en-us/cli/azure/repos/pr?view=azure-cli-latest) · [Azure Boards CLI](https://learn.microsoft.com/en-us/cli/azure/boards/work-item?view=azure-cli-latest) · [Thread API](https://learn.microsoft.com/en-us/rest/api/azure/devops/git/pull-request-threads/create?view=azure-devops-rest-7.1)

## Sign in

Install Azure CLI first. For the exercise use an Azure DevOps Services organization, a sandbox repository and a synthetic work item. Entra access tokens are short-lived and work with organizational identity controls. Configure Git Credential Manager (GCM) with OAuth for `git fetch` and `git push` over HTTPS. Git uses its own credential flow. [CLI sign-in](https://learn.microsoft.com/en-us/cli/azure/authenticate-azure-cli-interactively) · [Git authentication](https://learn.microsoft.com/en-us/azure/devops/repos/git/auth-overview)

```bash
az extension add --name azure-devops --version 1.0.11
az login --tenant YOUR_TENANT_ID --allow-no-subscriptions
```

**PAT fallback.** Create a short-lived PAT for the training organization. `az devops login --organization https://dev.azure.com/YOUR_ORG` prompts for it. For a temporary CLI session, inject `AZURE_DEVOPS_EXT_PAT` from your secret store. This variable is for the extension. For Git over HTTPS, use GCM and authenticate separately. Keep secrets out of prompts, command arguments, repository files and shell history. [CLI PAT sign-in](https://learn.microsoft.com/en-us/azure/devops/cli/log-in-via-pat) · [PAT policy](https://learn.microsoft.com/en-us/azure/devops/organizations/accounts/manage-pats-with-policies-for-administrators)

| PAT scope | Use |
|---|---|
| Code (Read), `vso.code` | Read repos, diffs and PRs. |
| Code (Read & write), `vso.code_write` | Push a branch, create or update PRs. Also accepted by the thread endpoint. |
| Code (Pull request threads), `vso.threads_full` | Read and write discussion threads. Use this for a comment-only writer, with Code (Read) if it must also read diffs. |
| Work Items (Read), `vso.work` | Read a work item. |
| Work Items (Read & write), `vso.work_write` | Create and edit work items. |

A PAT selects an organization and capability scopes. For one-project or one-repository isolation, restrict the owning identity's Azure DevOps permissions. In Azure Pipelines, use the short-lived `System.AccessToken` with project-scoped job authorization. For automation outside Pipelines, use an Entra service principal or managed identity added to Azure DevOps, with project and repo permissions assigned there. [Scope reference](https://learn.microsoft.com/en-us/azure/devops/integrate/get-started/authentication/oauth#scopes) · [Job tokens](https://learn.microsoft.com/en-us/azure/devops/pipelines/process/access-tokens) · [Workload identities](https://learn.microsoft.com/en-us/azure/devops/integrate/get-started/authentication/service-principal-managed-identity)

## Agent workflow: work item to PR comment

Run these blocks in order in Bash from a clean local clone with `origin` pointing to the selected Azure repo. Fill in your training organization, project ID (GUID or URL-safe name), repository name and synthetic work-item ID. Use the Entra sign-in above. Choose an item whose example branch and file are new. In an actual task, have the agent implement the work item and run the project's checks before staging its changes.

```bash
ORG='https://dev.azure.com/YOUR_ORG'
PROJECT='YOUR_PROJECT_ID'
REPO='YOUR_REPO'
ITEM=123
BASE=main
BRANCH="agent/wi-$ITEM"

az boards work-item show --id "$ITEM" --organization "$ORG"
REPO_ID=$(az repos show --repository "$REPO" --project "$PROJECT" \
  --organization "$ORG" --query id --output tsv)
```

```bash
git fetch origin && git switch -c "$BRANCH" "origin/$BASE"
```

```bash
FILE="agent-demo-$ITEM.md"
printf '# Agent exercise\n\nAzure Boards work item %s.\n' "$ITEM" > "$FILE"
git add -- "$FILE" && git commit -m "Add exercise for work item $ITEM" && git push -u origin "$BRANCH"
```

```bash
PR_ID=$(az repos pr create --organization "$ORG" --project "$PROJECT" \
  --repository "$REPO_ID" --source-branch "$BRANCH" --target-branch "$BASE" \
  --title "Exercise for work item $ITEM" --description 'Synthetic course change.' \
  --work-items "$ITEM" --draft true --query pullRequestId --output tsv)

THREAD_URL="$ORG/$PROJECT/_apis/git/repositories/$REPO_ID/pullRequests/$PR_ID/threads?api-version=7.1"
THREAD_JSON='{"comments":[{"parentCommentId":0,"content":"Agent exercise: synthetic change linked to the work item.","commentType":"text"}],"status":"active"}'

az rest --method post --url "$THREAD_URL" --resource 499b84ac-1321-427f-aa17-267ca6975798 \
  --body "$THREAD_JSON" --query id --output tsv
```

`az rest` gets a Microsoft Entra bearer for Azure DevOps. For the PAT-only path, replace the `az rest` call with this equivalent REST request after securely injecting `AZURE_DEVOPS_EXT_PAT`. The pipe keeps the header out of curl's arguments. [Azure CLI REST](https://learn.microsoft.com/en-us/cli/azure/reference-index?view=azure-cli-latest#az-rest) · [Azure DevOps Entra resource](https://learn.microsoft.com/en-us/azure/devops/integrate/get-started/authentication/entra) · [PAT HTTP authentication](https://learn.microsoft.com/en-us/azure/devops/organizations/accounts/use-personal-access-tokens-to-authenticate#use-a-pat)

```bash
if [[ -n "${AZURE_DEVOPS_EXT_PAT:-}" ]]; then
  jq -nr '"Authorization: Basic " + ((":" + env.AZURE_DEVOPS_EXT_PAT) | @base64)' \
    | curl --fail --silent --show-error --request POST --header @- \
        --header 'Content-Type: application/json' --data-binary "$THREAD_JSON" "$THREAD_URL"
fi
```

## MCP in Claude Code and Codex

Microsoft's [local Azure DevOps MCP server](https://github.com/microsoft/azure-devops-mcp) exposes Azure Repos, PRs and Azure Boards tools over stdio. After `az login`, use Node.js 20+ and run the command for your client. `YOUR_ORG` is the organization name. Start by asking the agent to read one synthetic work item and confirm the organization and project it used. [Claude Code MCP](https://code.claude.com/docs/en/mcp) · [Codex MCP](https://developers.openai.com/codex/mcp)

```bash
claude mcp add --transport stdio ado -- npx --yes @azure-devops/mcp@2.11.0 YOUR_ORG --authentication azcli
```

```bash
codex mcp add ado -- npx --yes @azure-devops/mcp@2.11.0 YOUR_ORG --authentication azcli
```

The hosted HTTP server uses `https://mcp.dev.azure.com/YOUR_ORG` and Entra OAuth. For Claude Code, register an Entra public client with redirect `http://localhost:3118/callback`, enable public client flows and grant the required delegated Azure DevOps permissions. Register the server under a different name from the local one and sign in using `/mcp`. Microsoft's setup guide directs Codex users to the local server. [Remote setup and Claude registration](https://learn.microsoft.com/en-us/azure/devops/mcp-server/remote-mcp-server#claude-code)

```bash
claude mcp add --transport http ado-remote https://mcp.dev.azure.com/YOUR_ORG \
  --client-id YOUR_ENTRA_APP_CLIENT_ID --callback-port 3118
```

## Automated PR review with Azure Pipelines

1. Create a pipeline that points to [azure-pipelines-agent-review.yml](azure-pipelines-agent-review.yml). On the target branch, add an automatic **Build validation** policy for that pipeline. Open a non-draft PR to trigger it.
2. Create the `azure-devops-agent-review` variable group, add secret `ANTHROPIC_API_KEY`, and authorize this pipeline only. Use synthetic changes and an approved Anthropic API account for the diff.
3. Enable **Limit job authorization scope to current project for non-release pipelines**. In the target repo's security settings, grant `<Project> Build Service (<Organization>)` **Read** and **Contribute to pull requests**. Check its inherited grants. The job takes its own `System.AccessToken` and uses it only in the publisher step.
4. Open a disposable PR and check the job and the thread. The comment names the reviewed source SHA. Each successful rerun creates a new thread. Keep the review advisory. [Azure Repos PR validation](https://learn.microsoft.com/en-us/azure/devops/pipelines/repos/azure-repos-git#pr-triggers) · [Variable groups](https://learn.microsoft.com/en-us/azure/devops/pipelines/library/variable-groups) · [Repo permissions](https://learn.microsoft.com/en-us/azure/devops/organizations/security/permissions?view=azure-devops) · [Claude Code headless](https://code.claude.com/docs/en/headless)

The sample assumes trusted training contributors because Azure Repos evaluates the PR merge revision for build validation. Anyone able to change the secret-bearing pipeline can use that access. For untrusted PRs, use an independently controlled pipeline or service and treat the PR diff as data. Use explicit review before an agent pushes or posts. GitHub Enterprise Cloud with data residency uses GitHub's `*.ghe.com` service and GitHub tooling, while Azure Repos uses Azure DevOps and Azure DevOps tooling. [GitHub data residency](https://docs.github.com/en/enterprise-cloud@latest/admin/data-residency/about-storage-of-your-data-with-data-residency)
