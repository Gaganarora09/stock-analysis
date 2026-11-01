-- Sync PostgreSQL auto-increment sequences with existing max IDs after data seeding
-- Ensures subsequent JPA inserts do not collide with explicitly seeded IDs
SELECT CASE 
    WHEN pg_get_serial_sequence('stock', 'id') IS NOT NULL 
    THEN setval(pg_get_serial_sequence('stock', 'id'), COALESCE(MAX(id), 1)) 
END FROM stock;

SELECT CASE 
    WHEN pg_get_serial_sequence('stock_price', 'id') IS NOT NULL 
    THEN setval(pg_get_serial_sequence('stock_price', 'id'), COALESCE(MAX(id), 1)) 
END FROM stock_price;

SELECT CASE 
    WHEN pg_get_serial_sequence('app_user', 'id') IS NOT NULL 
    THEN setval(pg_get_serial_sequence('app_user', 'id'), COALESCE(MAX(id), 1)) 
END FROM app_user;

SELECT CASE 
    WHEN pg_get_serial_sequence('watchlist', 'id') IS NOT NULL 
    THEN setval(pg_get_serial_sequence('watchlist', 'id'), COALESCE(MAX(id), 1)) 
END FROM watchlist;
