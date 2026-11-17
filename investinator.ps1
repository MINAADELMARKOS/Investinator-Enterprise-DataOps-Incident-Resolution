$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
& python (Join-Path $root "scripts/investinator_cli.py") @args
exit $LASTEXITCODE
