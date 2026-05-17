param(
    [string]$EnvFile = ".\data\runtime-init\db-sync.env",
    [ValidateSet("review", "chatbot")]
    [string[]]$Targets = @("review", "chatbot"),
    [switch]$WhatIf
)

$ErrorActionPreference = "Stop"

function Load-EnvFile {
    param([string]$Path)

    if (-not (Test-Path $Path)) {
        throw "Khong tim thay env file: $Path"
    }

    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#")) {
            return
        }

        $parts = $line -split "=", 2
        if ($parts.Count -ne 2) {
            return
        }

        $key = $parts[0].Trim()
        $value = $parts[1].Trim()
        Set-Item -Path "Env:$key" -Value $value
    }
}

function Require-Command {
    param([string]$Name)

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Thieu command can thiet: $Name"
    }
}

function Require-Env {
    param([string]$Name)

    $value = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($value)) {
        throw "Thieu bien moi truong: $Name"
    }
    return $value
}

function Sync-MongoTarget {
    param(
        [string]$Name,
        [string]$SourceUri,
        [string]$TargetUri,
        [string]$DumpRoot,
        [switch]$DryRun
    )

    $targetDir = Join-Path $DumpRoot $Name
    Write-Host "=== Mongo sync: $Name ==="

    if ($DryRun) {
        Write-Host "[WhatIf] Dump tu '$SourceUri' -> $targetDir"
        Write-Host "[WhatIf] Restore len '$TargetUri' voi --drop"
        return
    }

    & mongodump "--uri=$SourceUri" "--out=$targetDir"
    if (-not (Test-Path $targetDir)) {
        throw "Dump that bai cho target: $Name"
    }

    & mongorestore "--uri=$TargetUri" "--drop" $targetDir
    Write-Host "Done: $Name"
}

Load-EnvFile -Path $EnvFile

Require-Command -Name "mongodump"
Require-Command -Name "mongorestore"

$workDir = Join-Path $env:TEMP ("db-sync-mongo-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $workDir | Out-Null

try {
    foreach ($target in $Targets) {
        switch ($target) {
            "review" {
                $sourceUri = Require-Env -Name "LOCAL_REVIEW_MONGO_URI"
                $targetUri = Require-Env -Name "SERVER_REVIEW_MONGO_URI"
                Sync-MongoTarget -Name "review" -SourceUri $sourceUri -TargetUri $targetUri -DumpRoot $workDir -DryRun:$WhatIf
            }
            "chatbot" {
                $sourceUri = Require-Env -Name "LOCAL_CHATBOT_MONGO_URI"
                $targetUri = Require-Env -Name "SERVER_CHATBOT_MONGO_URI"
                Sync-MongoTarget -Name "chatbot" -SourceUri $sourceUri -TargetUri $targetUri -DumpRoot $workDir -DryRun:$WhatIf
            }
        }
    }
}
finally {
    if (Test-Path $workDir) {
        Remove-Item -LiteralPath $workDir -Recurse -Force
    }
}
