ALTER TABLE tournament
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN mode VARCHAR(10) NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN player_x_token VARCHAR(64),
    ADD COLUMN player_o_token VARCHAR(64),
    ADD COLUMN invite_token VARCHAR(64),
    ADD COLUMN invite_seat VARCHAR(1);