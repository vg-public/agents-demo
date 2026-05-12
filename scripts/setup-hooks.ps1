# =============================================================================
# setup-hooks.ps1 — Register .githooks as the Git hooks directory (Windows)
#
# Usage (run once per clone):
#   .\scripts\setup-hooks.ps1
#
# Hooks activated:
#   pre-commit  — branch protection, secret detection, System.out check, Spotless format
#   commit-msg  — enforce Conventional Commits or Jira ticket format
#   pre-push    — compile + unit test gate before push
# =============================================================================

$ErrorActionPreference = "Stop"

$hooksDir = ".githooks"
$hooks = @("pre-commit", "commit-msg", "pre-push")

Write-Host "Configuring Git to use '$hooksDir' as the hooks directory..." -ForegroundColor Cyan

# Point Git at our tracked hooks directory
git config core.hooksPath $hooksDir
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to set core.hooksPath. Is this a Git repository?"
    exit 1
}

# On Windows, Git (MINGW) requires hook files to use LF line endings, not CRLF.
# Normalise all hook files to LF.
foreach ($hook in $hooks) {
    $hookPath = Join-Path $hooksDir $hook
    if (Test-Path $hookPath) {
        $content = Get-Content $hookPath -Raw
        $content = $content -replace "`r`n", "`n"
        [System.IO.File]::WriteAllText((Resolve-Path $hookPath), $content)
        Write-Host "Line endings normalised to LF: $hookPath" -ForegroundColor Green
    } else {
        Write-Warning "Expected hook not found: $hookPath"
    }
}

Write-Host ""
Write-Host "Done. Git hooks are now active:" -ForegroundColor Green
Write-Host "  pre-commit  -> $hooksDir/pre-commit  (branch guard | secret scan | System.out check | Spotless)"
Write-Host "  commit-msg  -> $hooksDir/commit-msg  (Conventional Commits / Jira ticket format)"
Write-Host "  pre-push    -> $hooksDir/pre-push    (mvn compile + mvn test)"
Write-Host ""
Write-Host "Tip: to bypass pre-push in an emergency: `$env:SKIP_PRE_PUSH=1; git push" -ForegroundColor Yellow
