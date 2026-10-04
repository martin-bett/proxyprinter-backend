@echo off
rem Lokale Postgres (Docker) auf den Stand der SQL-Skripte bringen.
rem Starten: Doppelklick, oder in VS Code "Terminal > Run Task > DB lokal aktualisieren".
rem Alle Skripte in src\main\resources\sql duerfen beliebig oft laufen.
rem Anderer Containername: db-update-local.cmd MEIN_CONTAINER
setlocal
set CONTAINER=%1
if "%CONTAINER%"=="" set CONTAINER=proxymaster
cd /d "%~dp0"
echo Spiele SQL-Skripte in Container %CONTAINER% ein ...
type src\main\resources\sql\*.sql 2>nul | docker exec -i %CONTAINER% sh -c "psql -v ON_ERROR_STOP=1 -U \"$POSTGRES_USER\" -d \"${POSTGRES_DB:-$POSTGRES_USER}\""
if errorlevel 1 (
  echo.
  echo FEHLER: siehe Ausgabe oben. Laeuft Docker und heisst der Container wirklich %CONTAINER%? Pruefen mit: docker ps
) else (
  echo.
  echo Fertig, lokale Datenbank ist aktuell.
)
pause
