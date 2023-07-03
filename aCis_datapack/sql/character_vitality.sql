--
-- Table structure for table `character_vitality`
--

DROP TABLE IF EXISTS `character_pccafe_points`;
CREATE TABLE `character_vitality` (
  `objectId` int(11) NOT NULL,
  `points` int(11) default '0',
  PRIMARY KEY  (`objectId`)
) ENGINE=MyISAM;
