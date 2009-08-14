create table USER_FEED (
    LINK VARCHAR(255) NOT NULL,
    CHANNELKEY VARCHAR(255),
    
    primary key (link),
    foreign key (channelkey) references channel(link)
);