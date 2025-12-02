# If you see an error like:
# cannot be loaded because running scripts is disabled on this system
# Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
# powershell -ExecutionPolicy Bypass -File .\BackupFileCleanup.ps1
# powershell.exe -ExecutionPolicy Bypass -File "C:\Users\Mohamed\Desktop\accounts-backup\BackupFileCleanup.ps1"

# Delete files older than 30 days from backup folder
$backupPath = "C:\Users\Mohamed\Desktop\accounts-backup"
$daysToKeep = 30
$filePrefix = "main-account"
$cutoffDate = (Get-Date).AddDays(-$daysToKeep)

Write-Host "Deleting files starting with '$filePrefix' older than $cutoffDate" -ForegroundColor Yellow
Write-Host "Path: $backupPath" -ForegroundColor Cyan
Write-Host ""

if (Test-Path $backupPath) {
    $filesToDelete = Get-ChildItem -Path $backupPath -Recurse -File -Filter "$filePrefix*" |
                     Where-Object { $_.LastWriteTime -lt $cutoffDate }

    $count = 0
    foreach ($file in $filesToDelete) {
        try {
            Remove-Item -Path $file.FullName -Force
            Write-Host "Deleted: $($file.FullName)" -ForegroundColor Green
            $count++
        } catch {
            Write-Host "Failed to delete: $($file.FullName) - $($_.Exception.Message)" -ForegroundColor Red
        }
    }

    Write-Host ""
    Write-Host "Cleanup completed! Deleted $count file(s)." -ForegroundColor Green
} else {
    Write-Host "Backup path does not exist: $backupPath" -ForegroundColor Red
}

Read-Host "Press Enter to exit"