IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = N'TournamentDB')
BEGIN
    CREATE DATABASE [TournamentDB];
END;
GO