DO $$
DECLARE
default_password VARCHAR := '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzPKnouShjzPSz1wZGsG0p9F2nuzs2';
BEGIN
FOR i IN 1..500 LOOP
        INSERT INTO users (username, password)
        VALUES ('user_' || i, default_password);
END LOOP;
END $$;