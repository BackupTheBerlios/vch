create table CHANNEL (
    TITLE VARCHAR(255) NULL,
    LINK VARCHAR(255) NOT NULL,
    DESCRIPTION LONGVARCHAR NULL,
    THUMBNAIL VARCHAR(255) NULL,
    COPYRIGHT VARCHAR(255) NULL,
    PUBDATE TIMESTAMP NULL,
    LANGUAGE VARCHAR(5) NULL,

    primary key (link)
);

create table ENCLOSURE (
    LINK VARCHAR(255) NOT NULL,
    TYPE VARCHAR(255) NULL,
    LENGTH BIGINT NULL,
    DURATION BIGINT NULL,

    primary key (link)
);

create table ITEM (
    TITLE VARCHAR(255) NULL,
    LINK VARCHAR(255) NULL,
    DESCRIPTION LONGVARCHAR NULL,
    THUMBNAIL VARCHAR(255) NULL,
    PUBDATE TIMESTAMP NULL,
    GUID VARCHAR(255) NOT NULL,
    CHANNELKEY VARCHAR(255) NULL,
    ENCLOSUREKEY VARCHAR(255) NULL,

    primary key (guid)
);

create table CONFIG (
    PARAMETERKEY VARCHAR(255) NOT NULL,
    PARAMETERVALUE VARCHAR(255) NULL,

    primary key (PARAMETERKEY)
);

create table GROUPS (
    NAME VARCHAR(255) NULL,
    DESCRIPTION LONGVARCHAR NULL,

    primary key (NAME)
);

create table GROUPS_INTER_CHANNEL (

	ROW_ID INTEGER GENERATED BY DEFAULT AS IDENTITY,
	GROUP_NAME VARCHAR(255) NULL,
	CHANNEL_LINK VARCHAR(255) NOT NULL,
    
    primary key (ROW_ID)
);