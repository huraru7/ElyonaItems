@echo off
setlocal enabledelayedexpansion

echo ===================================================
echo  ElyonaItems - Setup Script
echo ===================================================
echo.

:: ユーザー名の全角文字によるGradleエラーを回避するため
:: GRADLE_USER_HOME を ASCII パスに設定する
set GRADLE_USER_HOME=C:\gradle-home
echo [INFO] GRADLE_USER_HOME を C:\gradle-home に設定しました

if not exist "C:\gradle-home" (
    mkdir "C:\gradle-home"
    echo [INFO] C:\gradle-home を作成しました
)

:: libs フォルダの確認
if not exist "libs" mkdir "libs"
echo [INFO] libs フォルダを確認しました
echo [WARN] libs\ElyonaCore.jar と libs\ItemsAdder.jar を手動で配置してください

:: gradle-wrapper.jar のダウンロード
if not exist "gradle\wrapper\gradle-wrapper.jar" (
    echo [INFO] gradle-wrapper.jar をダウンロードします...
    powershell -Command "& { try { Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.8-wrapper.jar' -OutFile 'gradle\wrapper\gradle-wrapper.jar' -ErrorAction Stop; Write-Host '[INFO] ダウンロード成功' } catch { Write-Host '[WARN] 直接ダウンロード失敗。代替方法を試みます...' } }"

    if not exist "gradle\wrapper\gradle-wrapper.jar" (
        echo [INFO] Gradle ディストリビューションからラッパーを抽出します...
        powershell -Command "& { $url = 'https://services.gradle.org/distributions/gradle-8.8-bin.zip'; $tmp = \"$env:TEMP\gradle-8.8-bin.zip\"; $dir = \"$env:TEMP\gradle-8.8\"; Write-Host '[INFO] Gradle 8.8 をダウンロード中 (しばらくかかります)...'; Invoke-WebRequest -Uri $url -OutFile $tmp; Expand-Archive -Path $tmp -DestinationPath $dir -Force; Copy-Item \"$dir\gradle-8.8\lib\plugins\gradle-wrapper*.jar\" 'gradle\wrapper\gradle-wrapper.jar' -ErrorAction SilentlyContinue; if (-not (Test-Path 'gradle\wrapper\gradle-wrapper.jar')) { Get-ChildItem \"$dir\" -Recurse -Filter '*wrapper*.jar' | Select-Object -First 1 | Copy-Item -Destination 'gradle\wrapper\gradle-wrapper.jar' }; Write-Host '[INFO] 完了' }"
    )
)

if exist "gradle\wrapper\gradle-wrapper.jar" (
    echo [INFO] gradle-wrapper.jar が存在します
    echo.
    echo [INFO] セットアップ完了！ビルドするには build.bat を実行してください
) else (
    echo [ERROR] gradle-wrapper.jar の取得に失敗しました
    echo [INFO] 手動で以下のいずれかを実行してください:
    echo   1. Gradle がインストールされている場合: gradle wrapper --gradle-version 8.8
    echo   2. IntelliJ IDEA でプロジェクトを開く (自動でラッパーが生成されます)
)

endlocal
pause
