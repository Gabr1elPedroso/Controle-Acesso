@echo off
setlocal EnableDelayedExpansion
chcp 65001 > nul

echo ============================================================
echo   GAP - Gerador de Instalador
echo ============================================================
echo.

REM ------------------------------------------------------------
REM 1. Garante que estamos na pasta correta (onde fica o pom.xml)
REM ------------------------------------------------------------
cd /d "%~dp0"

REM ------------------------------------------------------------
REM 2. Compila e empacota com Maven
REM    Gera: target\controle-acesso.jar
REM          target\libs\*.jar   (dependencias)
REM          target\backup-job.jar (fat jar do agendador)
REM ------------------------------------------------------------
echo [1/3] Compilando o projeto com Maven...
call mvn clean package -q
if %ERRORLEVEL% neq 0 (
    echo.
    echo ERRO: Falha na compilação. Verifique o terminal acima.
    pause
    exit /b 1
)
echo       OK.
echo.

REM ------------------------------------------------------------
REM 3. Verifica se o jpackage esta disponivel
REM    (faz parte do JDK 21, mas vale confirmar)
REM ------------------------------------------------------------
where jpackage > nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ERRO: jpackage nao encontrado.
    echo Certifique-se de que o JDK 21 esta instalado e no PATH.
    pause
    exit /b 1
)

REM ------------------------------------------------------------
REM 4. Gera o instalador .exe com jpackage
REM    O instalador fica em: instalador\GAP Controle de Acesso-1.0.exe
REM ------------------------------------------------------------
echo [2/3] Gerando o instalador (.exe) com jpackage...

if exist instalador rmdir /s /q instalador

jpackage ^
  --type exe ^
  --name "GAP Controle de Acesso" ^
  --app-version 1.0 ^
  --input target ^
  --main-jar controle-acesso.jar ^
  --main-class com.gap.App ^
  --win-shortcut ^
  --win-menu ^
  --win-dir-chooser ^
  --dest instalador ^
  --java-options "--add-opens javafx.graphics/com.sun.javafx.application=ALL-UNNAMED" ^
  --java-options "-Dfile.encoding=UTF-8"

if %ERRORLEVEL% neq 0 (
    echo.
    echo ERRO: Falha no jpackage.
    echo.
    echo Causas comuns:
    echo   - WiX Toolset nao instalado ^(baixe em wixtoolset.org^)
    echo   - Se nao quiser instalar o WiX agora, troque --type exe
    echo     por --type app-image neste script e rode novamente.
    echo     O app-image gera uma pasta com o .exe direto, sem instalador.
    pause
    exit /b 1
)

echo       OK.
echo.

REM ------------------------------------------------------------
REM 5. Copia o backup-job.jar para dentro do instalador,
REM    junto com o run-backup.bat, para o cliente poder
REM    configurar o agendamento depois.
REM ------------------------------------------------------------
echo [3/3] Copiando arquivos do agendador de backup...
if exist instalador (
    copy /y target\backup-job.jar instalador\backup-job.jar > nul
    copy /y run-backup.bat instalador\run-backup.bat > nul
)
echo       OK.
echo.

echo ============================================================
echo   Concluido!
echo.
echo   Instalador gerado em:
echo     %~dp0instalador\
echo.
echo   Para instalar o agendador de backup no cliente:
echo     1. Copie backup-job.jar e run-backup.bat para a pasta
echo        onde o sistema foi instalado (ex: C:\GAP\controle-acesso)
echo     2. Edite o caminho dentro do run-backup.bat
echo     3. Rode no terminal (como Administrador):
echo        schtasks /create /tn "GAP - Backup Diario" /tr "C:\GAP\run-backup.bat" /sc daily /st 18:00 /f
echo ============================================================
echo.
pause