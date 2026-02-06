CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS users (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    name varchar(20) NOT NULL UNIQUE,
    email varchar(50) NOT NULL UNIQUE,
    password varchar(100) NOT NULL,
    country varchar(50) NOT NULL,
    experience int2,
    photo varchar(100),
    biography varchar(1000)
);
