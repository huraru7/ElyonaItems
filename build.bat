@echo off
setlocal

:: ユーザー名の全角文字によるGradleエラーを回避
set GRADLE_USER_HOME=C:\gradle-home

if not exist "C:\gradle-home" (
    mkdir "C:\gradle-home"
)

if not exist "gradle\wrapper\gradle-wrapper.jar" (
    echo [ERROR] gradle-wrapper.jar が見つかりません。先に setup.bat を実行してください。
    pause
    exit /b 1
)

echo [INFO] GRADLE_USER_HOME = %GRADLE_USER_HOME%
echo [INFO] ビルドを開始します...
echo.

call gradlew.bat %* reobfJar

if %ERRORLEVEL% equ 0 (
    echo.
    echo [INFO] ビルド成功！
    echo [INFO] 出力: build\libs\ElyonaItems-1.0.0.jar  (サーバーに入れるファイル)
    echo [INFO]       build\libs\ElyonaItems-1.0.0-dev.jar  (開発用/中間ファイル)
) else (
    echo.
    echo [ERROR] ビルドに失敗しました
)

endlocal
pause
