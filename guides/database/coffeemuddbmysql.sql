# Connection: SQL
# Host: localhost
# Saved: 2003-04-01 00:29:14
# 
# Connection: SQL
# Host: localhost
# Saved: 2003-04-01 00:27:39
# 
CREATE TABLE CMVFS (
	CMFNAM varchar (255),
	CMDTYP int ,
	CMMODD bigint,
	CMWHOM varchar (50) NULL,
	CMDATA longtext NULL
);

ALTER TABLE CMVFS
	ADD 
	( 
		UNIQUE KEY (CMFNAM)
	);

CREATE TABLE CMCHAB (
	CMUSERID varchar (50) NULL ,
	CMABID varchar (50) NULL ,
	CMABPF int NULL ,
	CMABTX longtext NULL
);

ALTER TABLE CMCHAB
	ADD 
	( 
		UNIQUE KEY (CMUSERID,CMABID)
	);

CREATE TABLE CMSTAT (
	CMSTRT bigint,
	CMENDT bigint,
	CMDATA longtext NULL
);

ALTER TABLE CMSTAT
	ADD 
	( 
		UNIQUE KEY (CMSTRT)
	);
	
CREATE TABLE CMPOLL (
	CMNAME varchar (100) ,
	CMBYNM varchar (100) NULL ,
	CMSUBJ varchar (255) NULL ,
	CMDESC longtext NULL ,
	CMOPTN longtext NULL ,
	CMFLAG int NULL ,
	CMQUAL varchar (255) NULL ,
	CMRESL longtext NULL,
	CMEXPI bigint NULL
);

ALTER TABLE CMPOLL
	ADD 
	( 
		UNIQUE KEY (CMNAME)
	);
	
CREATE TABLE CMCHAR (
	CMUSERID varchar (50) NULL ,
	CMPASS varchar (50) NULL ,
	CMCLAS varchar (200) NULL ,
	CMSTRE int NULL ,
	CMRACE varchar (50) NULL ,
	CMDEXT int NULL ,
	CMCONS int NULL ,
	CMGEND varchar (50) NULL ,
	CMWISD int NULL ,
	CMINTE int NULL ,
	CMCHAR int NULL ,
	CMHITP int NULL ,
	CMLEVL varchar (50) NULL ,
	CMMANA int NULL ,
	CMMOVE int NULL ,
	CMDESC varchar (255) NULL ,
	CMALIG int NULL ,
	CMEXPE int NULL ,
	CMEXLV int NULL ,
	CMWORS varchar (50) NULL ,
	CMPRAC int NULL ,
	CMTRAI int NULL ,
	CMAGEH int NULL ,
	CMGOLD int NULL ,
	CMWIMP int NULL ,
	CMQUES int NULL ,
	CMROID varchar (100) NULL ,
	CMDATE varchar (50) NULL ,
	CMCHAN int NULL ,
	CMATTA int NULL ,
	CMAMOR int NULL ,
	CMDAMG int NULL ,
	CMBTMP int NULL ,
	CMLEIG varchar (50) NULL ,
	CMHEIT int NULL ,
	CMWEIT int NULL ,
	CMPRPT varchar (250) NULL,
	CMCOLR varchar (100) NULL,
	CMLSIP varchar (100) NULL,
	CMCLAN varchar (100) NULL,
	CMCLRO integer NULL,
	CMEMAL varchar (255) NULL,
	CMPFIL longtext NULL,
	CMSAVE varchar (150) NULL,
	CMMXML longtext NULL
);

ALTER TABLE CMCHAR
	ADD 
	( 
		UNIQUE KEY (CMUSERID)
	);

CREATE TABLE CMCHFO (
	CMUSERID varchar (50) NULL ,
	CMFONM int NULL ,
	CMFOID varchar (50) NULL ,
	CMFOTX longtext NULL ,
	CMFOLV int NULL ,
	CMFOAB int NULL 
);

ALTER TABLE CMCHFO
	ADD 
	( 
		UNIQUE KEY (CMUSERID,CMFONM)
	);

CREATE TABLE CMCHIT (
	CMUSERID varchar (50) NULL ,
	CMITNM varchar (100) NULL ,
	CMITID varchar (50) NULL ,
	CMITTX longtext NULL ,
	CMITLO varchar (100) NULL ,
	CMITWO int NULL ,
	CMITUR int NULL ,
	CMITLV int NULL ,
	CMITAB int NULL ,
	CMHEIT int NULL
);

ALTER TABLE CMCHIT
	ADD 
	( 
		UNIQUE KEY (CMUSERID,CMITNM)
	);

CREATE TABLE CMROCH (
	CMROID varchar (50) NULL ,
	CMCHNM varchar (100) NULL ,
	CMCHID varchar (50) NULL ,
	CMCHTX longtext NULL ,
	CMCHLV int NULL ,
	CMCHAB int NULL ,
	CMCHRE int NULL ,
	CMCHRI varchar (100) NULL
);

ALTER TABLE CMROCH 
	ADD 
	( 
		UNIQUE KEY (CMROID,CMCHNM)
	);

CREATE TABLE CMROEX (
	CMROID varchar (50) NULL ,
	CMDIRE int NULL ,
	CMEXID varchar (50) NULL ,
	CMEXTX longtext NULL ,
	CMNRID varchar (50) NULL 
);

ALTER TABLE CMROEX 
	ADD 
	( 
		UNIQUE KEY (CMROID,CMDIRE)
	);

CREATE TABLE CMROIT (
	CMROID varchar (50) NULL ,
	CMITNM varchar (100) NULL ,
	CMITID varchar (50) NULL ,
	CMITLO varchar (100) NULL ,
	CMITTX longtext NULL ,
	CMITRE int NULL ,
	CMITUR int NULL ,
	CMITLV int NULL ,
	CMITAB int NULL ,
	CMHEIT int NULL
);

ALTER TABLE CMROIT 
	ADD 
	( 
		UNIQUE KEY (CMROID,CMITNM)
	);

CREATE TABLE CMROOM (
	CMROID varchar (50) NULL ,
	CMLOID varchar (50) NULL ,
	CMAREA varchar (50) NULL ,
	CMDESC1 varchar (255) NULL ,
	CMDESC2 longtext NULL ,
	CMROTX longtext NULL 
);

ALTER TABLE CMROOM 
	ADD 
	( 
		UNIQUE KEY (CMROID)
	);


CREATE TABLE CMQUESTS (
	CMQUESID varchar (50) NULL ,
	CMQUTYPE varchar (50) NULL ,
	CMQSCRPT longtext NULL ,
	CMQWINNS longtext NULL
);

ALTER TABLE CMQUESTS 
	ADD 
	( 
		UNIQUE KEY (CMQUESID)
	);


CREATE TABLE CMAREA (
	CMAREA varchar (50) ,
	CMTYPE varchar (50) ,
	CMCLIM int NULL ,
	CMSUBS varchar (100) NULL ,
	CMDESC longtext NULL ,
	CMROTX longtext NULL ,
	CMTECH int NULL
);

ALTER TABLE CMAREA 
	ADD 
	( 
		UNIQUE KEY (CMAREA)
	);

CREATE TABLE CMJRNL (
	CMJKEY varchar (75) ,
	CMJRNL varchar (50) NULL ,
	CMFROM varchar (50) NULL ,
	CMDATE varchar (50) NULL ,
	CMTONM varchar (50) NULL ,
	CMSUBJ varchar (255) NULL ,
	CMPART varchar (75) NULL ,
	CMATTR int NULL,
	CMDATA varchar (255) NULL ,
	CMUPTM bigint NULL,
	CMMSGT longtext NULL 
);

ALTER TABLE CMJRNL 
	ADD 
	( 
		UNIQUE KEY (CMJKEY)
	);

CREATE INDEX CMJRNLNAME on CMJRNL (CMJRNL);
CREATE INDEX CMJRNLCMPART on CMJRNL (CMPART);
CREATE INDEX CMJRNLCMTONM on CMJRNL (CMTONM);

CREATE TABLE CMCLAN (
	CMCLID varchar (100) ,
	CMTYPE int ,
	CMDESC longtext NULL ,
	CMACPT varchar (255) NULL ,
	CMPOLI longtext NULL ,
	CMRCLL varchar (50) NULL ,
	CMDNAT varchar (50) NULL ,
	CMSTAT int NULL ,
	CMMORG varchar (50) NULL ,
	CMTROP int NULL
);

ALTER TABLE CMCLAN 
	ADD 
	( 
		UNIQUE KEY (CMCLID)
	);

CREATE TABLE CMPDAT (
	CMPLID varchar (100) ,
	CMSECT varchar (100) ,
	CMPKEY varchar (100) ,
	CMPDAT longtext NULL 
);

ALTER TABLE CMPDAT 
	ADD 
	( 
		UNIQUE KEY (CMPLID,CMSECT,CMPKEY)
	);

CREATE TABLE CMGRAC (
	CMRCID varchar (50) ,
	CMRDAT longtext NULL 
);

ALTER TABLE CMGRAC 
	ADD 
	( 
		UNIQUE KEY (CMRCID)
	);
	
CREATE TABLE CMCCAC (
	CMCCID varchar (50) ,
	CMCDAT longtext NULL 
);

ALTER TABLE CMCCAC 
	ADD 
	( 
		UNIQUE KEY (CMCCID)
	);


CREATE TABLE CMGAAC (
	CMGAID varchar (50) ,
	CMGAAT longtext NULL 
);

ALTER TABLE CMGAAC 
	ADD 
	( 
		UNIQUE KEY (CMGAID)
	);
CREATE TABLE CMACCT (
	CMANAM varchar (50) ,
	CMPASS varchar (50) ,
	CMCHRS longtext NULL ,
	CMAXML longtext NULL 
);

ALTER TABLE CMACCT  ADD UNIQUE KEY (CMANAM);
