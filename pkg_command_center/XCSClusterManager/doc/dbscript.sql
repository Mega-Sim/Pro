drop table ocs_cluster_state purge;
create table ocs_cluster_state (
	process_name				varchar2(50),
	inservice_host				varchar2(100),
	primary_state				varchar2(50),
	secondary_state				varchar2(50),
	failover_acceptance			varchar2(10),
	user_request				varchar2(100),
	primary_updation_time		timestamp default SYSDATE,
	secondary_updation_time		timestamp default SYSDATE
);

-- alter table ocs_cluster_state add user_request varchar2(100) default null;

insert into ocs_cluster_state (process_name, inservice_host, primary_state, secondary_state ,failover_acceptance)
values ('operation', 'Primary', 'OutOfService', 'OutOfService', 'TRUE' );
insert into ocs_cluster_state (process_name, inservice_host, primary_state, secondary_state ,failover_acceptance)
values ('jobassign', 'Primary', 'OutOfService', 'OutOfService', 'TRUE' );
insert into ocs_cluster_state (process_name, inservice_host, primary_state, secondary_state ,failover_acceptance)
values ('ibsem', 'Primary', 'OutOfService', 'OutOfService', 'VIA VIP' );


-- delete ocsinfo where name like 'CLUSTER_%';
drop table ocs_cluster_info purge;
create table ocs_cluster_info (
	host			varchar2(100),
	info_item 		varchar2(64),
	state			varchar2(32) default 'DEAD',
	updation_time	timestamp default SYSDATE
);

insert into ocs_cluster_info (host, info_item) values('Primary','PUBLIC_NET');
insert into ocs_cluster_info (host, info_item) values('Secondary','PUBLIC_NET');
insert into ocs_cluster_info (host, info_item) values('Primary','LOCAL_NET');
insert into ocs_cluster_info (host, info_item) values('Secondary','LOCAL_NET');
insert into ocs_cluster_info (host, info_item) values('Primary','VIP');
insert into ocs_cluster_info (host, info_item) values('Secondary','VIP');
insert into ocs_cluster_info (host, info_item) values('Primary','CM');
insert into ocs_cluster_info (host, info_item) values('Secondary','CM');
insert into ocs_cluster_info (host, info_item) values('Primary','operation');
insert into ocs_cluster_info (host, info_item) values('Secondary','operation');
insert into ocs_cluster_info (host, info_item) values('Primary','jobassign');
insert into ocs_cluster_info (host, info_item) values('Secondary','jobassign');
insert into ocs_cluster_info (host, info_item) values('Primary','ibsem');
insert into ocs_cluster_info (host, info_item) values('Secondary','ibsem');

insert into ocs_cluster_info (host, info_item) values('Primary','stbc');
insert into ocs_cluster_info (host, info_item) values('Secondary','stbc');
insert into ocs_cluster_info (host, info_item) values('Primary','unitdevice');
insert into ocs_cluster_info (host, info_item) values('Secondary','unitdevice');
insert into ocs_cluster_info (host, info_item) values('Primary','ucinterface');
insert into ocs_cluster_info (host, info_item) values('Secondary','ucinterface');
insert into ocs_cluster_info (host, info_item) values('Primary','remoteserver');
insert into ocs_cluster_info (host, info_item) values('Secondary','remoteserver');
insert into ocs_cluster_info (host, info_item) values('Primary','optimizer');
insert into ocs_cluster_info (host, info_item) values('Secondary','optimizer');


/*
insert into ocsinfo (name, value) values('CLUSTER_PRI_PUBLIC','DEAD');
insert into ocsinfo (name, value) values('CLUSTER_PRI_LOCAL','DEAD');
insert into ocsinfo (name, value) values('CLUSTER_PRI_CM','DEAD');
insert into ocsinfo (name, value) values('CLUSTER_PRI_VIP','DEAD');
insert into ocsinfo (name, value) values('CLUSTER_PRI_OPERATION','DEAD');

insert into ocsinfo (name, value) values('CLUSTER_SEC_PUBLIC','DEAD');
insert into ocsinfo (name, value) values('CLUSTER_SEC_LOCAL','DEAD');
insert into ocsinfo (name, value) values('CLUSTER_SEC_CM','DEAD');
insert into ocsinfo (name, value) values('CLUSTER_SEC_VIP','DEAD');
insert into ocsinfo (name, value) values('CLUSTER_SEC_OPERATION','DEAD');
*/


-- TYPE     TAKEOVER, STATECHANGE, LOCALRETRY, STARTUP, REMOTESTATECHANGE, DETECT
-- PROCESS  operation, ibsem, jobassign, clustermanager
-- HOST     Primary, Secondary
-- SETTIME  
-- SHOWMSG
-- ALARMTEXT
-- ALARMCODE

drop table ocs_cluster_history purge;
create table ocs_cluster_history (
	event_type					varchar2(50),
	process_name				varchar2(50),
	host_name					varchar2(50),
	settime						timestamp default SYSDATE,
	alarmcode 					NUMBER,
    alarmtext					VARCHAR2(160),
    showmsg						VARCHAR2(5)
);



