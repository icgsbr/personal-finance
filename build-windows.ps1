# =============================================================================
#  build-windows.ps1 — Gera instaladores nativos para Windows (.msi e .exe)
#  Requisitos:
#    - JDK 21+ instalado (com jpackage)
#    - Maven instalado e no PATH
#    - WiX Toolset 3.x instalado (para .msi): https://wixtoolset.org/
#    - Inno Setup (para .exe): https://jrsoftware.org/isinfo.php  [opcional]
# =============================================================================

$ErrorActionPreference = "Stop"

$APP_NAME    = "FinancasPessoais"
$APP_VERSION = "1.0.0"
$MAIN_CLASS  = "com.finance.Launcher"
$MAIN_JAR    = "personal-finance.jar"
$VENDOR      = "Personal Finance"
$DESCRIPTION = "Controle de financas pessoais"

$JVM_OPTS = @(
    "--add-opens=java.base/java.lang=ALL-UNNAMED",
    "--add-opens=java.base/java.util=ALL-UNNAMED",
    "--add-opens=java.base/java.io=ALL-UNNAMED",
    "-Dfile.encoding=UTF-8",
    "-Djava.awt.headless=false"
)

# ---------------------------------------------------------------------------
Write-Host "🧹 Compilando projeto..." -ForegroundColor Cyan
mvn clean package -DskipTests -q
if ($LASTEXITCODE -ne 0) { throw "Maven build falhou." }

New-Item -ItemType Directory -Force -Path "dist" | Out-Null

$commonArgs = @(
    "--input",       "target\",
    "--main-jar",    $MAIN_JAR,
    "--main-class",  $MAIN_CLASS,
    "--app-version", $APP_VERSION,
    "--vendor",      $VENDOR,
    "--description", $DESCRIPTION,
    "--dest",        "dist\"
)
foreach ($opt in $JVM_OPTS) {
    $commonArgs += "--java-options"
    $commonArgs += $opt
}

# ---------------------------------------------------------------------------
#  .msi — Windows Installer (requer WiX Toolset)
# ---------------------------------------------------------------------------
Write-Host "🪟 Gerando instalador .msi..." -ForegroundColor Cyan
& jpackage @commonArgs `
    --name          $APP_NAME `
    --type          msi `
    --win-dir-chooser `
    --win-menu `
    --win-menu-group $VENDOR `
    --win-shortcut `
    --win-shortcut-prompt `
    --win-upgrade-uuid "a1b2c3d4-e5f6-7890-abcd-ef1234567890"

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ .msi gerado em dist\" -ForegroundColor Green
} else {
    Write-Host "⚠️  .msi falhou (WiX Toolset instalado?)" -ForegroundColor Yellow
}

# ---------------------------------------------------------------------------
#  .exe — Instalador executável
# ---------------------------------------------------------------------------
Write-Host "🪟 Gerando instalador .exe..." -ForegroundColor Cyan
& jpackage @commonArgs `
    --name          "${APP_NAME}-Setup" `
    --type          exe `
    --win-dir-chooser `
    --win-menu `
    --win-menu-group $VENDOR `
    --win-shortcut `
    --win-shortcut-prompt `
    --win-upgrade-uuid "a1b2c3d4-e5f6-7890-abcd-ef1234567890"

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ .exe gerado em dist\" -ForegroundColor Green
} else {
    Write-Host "⚠️  .exe falhou." -ForegroundColor Yellow
}

# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "📦 Instaladores em: $((Get-Location).Path)\dist\" -ForegroundColor Cyan
Get-ChildItem dist\ | Format-Table Name, Length, LastWriteTime
