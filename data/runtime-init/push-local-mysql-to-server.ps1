param(
    [string]$EnvFile = ".\data\runtime-init\db-sync.env",
    [string[]]$Databases = @("fashion_user_db", "fashion_product_db", "fashion_order_db", "fashion_promotion_db"),
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

function Resolve-DockerMySqlHost {
    param([string]$InputHost)

    # Docker client container khong the quay nguoc ve 127.0.0.1 cua may host.
    # Khi user mo tunnel bang kubectl port-forward tren local, can doi sang
    # host.docker.internal de mysql client container nhin thay cong tunnel do.
    if ($InputHost -eq "127.0.0.1" -or $InputHost -eq "localhost") {
        return "host.docker.internal"
    }

    return $InputHost
}

function Require-Env {
    param([string]$Name)

    $value = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($value)) {
        throw "Thieu bien moi truong: $Name"
    }
    return $value
}

Load-EnvFile -Path $EnvFile

Require-Command -Name "docker"

$localContainer = Require-Env -Name "LOCAL_MYSQL_CONTAINER"
$localPassword = Require-Env -Name "LOCAL_MYSQL_ROOT_PASSWORD"
$serverHost = Require-Env -Name "SERVER_MYSQL_HOST"
$serverPort = Require-Env -Name "SERVER_MYSQL_PORT"
$serverUser = Require-Env -Name "SERVER_MYSQL_USER"
$serverPassword = Require-Env -Name "SERVER_MYSQL_PASSWORD"
$dockerServerHost = Resolve-DockerMySqlHost -InputHost $serverHost

$workDir = Join-Path $env:TEMP ("db-sync-mysql-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $workDir | Out-Null

try {
    foreach ($db in $Databases) {
        $dumpFile = Join-Path $workDir "$db.sql"
        $containerDumpFile = "/tmp/$db.sql"
        Write-Host "=== MySQL sync: $db ==="

        $dumpCommand = @(
            "exec", "-i", $localContainer,
            "mysqldump",
            "-uroot",
            "-p$localPassword",
            "--default-character-set=utf8mb4",
            "--single-transaction",
            "--routines",
            "--triggers",
            "--databases",
            $db
        )

        $importCommand = @(
            "run", "--rm",
            "-v", "${dumpFile}:/sync/$db.sql:ro",
            "mysql:8.0",
            "sh", "-c",
            "mysql --default-character-set=utf8mb4 -h `"$dockerServerHost`" -P `"$serverPort`" -u `"$serverUser`" -p`"$serverPassword`" < /sync/$db.sql"
        )

        $writeDumpInContainerCommand = @(
            "exec", "-i", $localContainer,
            "sh", "-c",
            "mysqldump -uroot -p`"$localPassword`" --default-character-set=utf8mb4 --single-transaction --routines --triggers --databases `"$db`" > `"$containerDumpFile`""
        )

        $copyDumpCommand = @(
            "cp",
            "${localContainer}:$containerDumpFile",
            $dumpFile
        )

        $cleanupDumpCommand = @(
            "exec", "-i", $localContainer,
            "rm", "-f", $containerDumpFile
        )

        $legacyImportArgs = @(
            "-h", $dockerServerHost,
            "-P", $serverPort,
            "-u", $serverUser,
            "-p$serverPassword"
        )

        if ($WhatIf) {
            Write-Host "[WhatIf] Dump tu container '$localContainer' DB '$db' -> $dumpFile (utf8mb4, binary-safe)"
            Write-Host ("[WhatIf] Import len MySQL server '{0}:{1}' (docker host '{2}') DB '{3}'" -f $serverHost, $serverPort, $dockerServerHost, $db)
            continue
        }

        try {
            # Ghi file dump ben trong container roi docker cp ra ngoai de tranh PowerShell
            # dong/chuyen ma hoa stream stdout, vi se lam hong tieng Viet utf8mb4.
            & docker @writeDumpInContainerCommand | Out-Null
            & docker @copyDumpCommand | Out-Null
            if (-not (Test-Path $dumpFile)) {
                throw "Dump that bai cho DB: $db"
            }

            & docker @importCommand | Out-Null
            Write-Host "Done: $db"
        }
        finally {
            & docker @cleanupDumpCommand | Out-Null
        }
    }
}
finally {
    if (Test-Path $workDir) {
        Remove-Item -LiteralPath $workDir -Recurse -Force
    }
}
