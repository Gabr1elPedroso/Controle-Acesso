@echo off
REM Ajuste o caminho abaixo para a pasta onde fica o projeto/jar instalado no cliente.
cd /d "C:\Users\HP\Downloads\Controle Acesso\controle-acesso"

java -cp "controle-acesso-1.0-SNAPSHOT.jar" com.gap.BackupJob >> backup-log.txt 2>&1