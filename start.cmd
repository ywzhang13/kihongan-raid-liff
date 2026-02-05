@echo off
set DATABASE_URL=jdbc:postgresql://localhost:5432/postgres
set DATABASE_USERNAME=postgres
set DATABASE_PASSWORD=jaypostgre913
set JWT_SECRET=your-secret-key-must-be-at-least-32-characters-long-please-change-this

echo Starting KiHongan Raid System...
echo Database: %DATABASE_URL%
echo.

mvnw.cmd spring-boot:run -Dmaven.test.skip=true
