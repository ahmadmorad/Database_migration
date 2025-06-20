-- Simuliere lange Ausführungszeit mit einer großen Datenmenge
CREATE TABLE big_data (
                          id SERIAL PRIMARY KEY,
                          payload TEXT
);
INSERT INTO big_data (payload)
SELECT repeat('dummy-data-', 1000)
FROM generate_series(1, 10000);
