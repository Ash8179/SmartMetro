create table if not exists line_crowding
(
	id int auto_increment
		primary key,
	line_id int not null comment '几号线',
	line_number int not null comment '列车车次',
	line_carriage int not null comment '车厢',
	person_num int not null,
	timestamp datetime not null,
	crowd_level int null
)
comment '车厢拥挤度';