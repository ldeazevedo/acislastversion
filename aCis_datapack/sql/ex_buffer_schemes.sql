CREATE TABLE IF NOT EXISTS `ex_buffer_schemes` (
  `PlayerID` int(10) NOT NULL,
  `Description` varchar(50) DEFAULT NULL,
  `IconID` int(10) DEFAULT NULL,
  `Name` varchar(50) DEFAULT NULL,
  `CreatedDate` decimal(20,0) DEFAULT NULL,
  `Skills` varchar(300) DEFAULT NULL,
  `RestoreCP` tinyint(4) NOT NULL DEFAULT '0',
  `RestoreHP` tinyint(4) NOT NULL DEFAULT '0',
  `RestoreMP` tinyint(4) NOT NULL DEFAULT '0',
  `ShareCode` varchar(20) DEFAULT NULL,
  `ShareCodeLocked` tinyint(4) NOT NULL DEFAULT '0'
);