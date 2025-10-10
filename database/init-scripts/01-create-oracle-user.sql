DECLARE
    user_exists NUMBER;
BEGIN
SELECT COUNT(*) INTO user_exists FROM all_users WHERE username = 'BOOKSTORE';

IF user_exists = 0 THEN
EXECUTE IMMEDIATE 'CREATE USER bookstore IDENTIFIED BY bookstore123';
EXECUTE IMMEDIATE 'GRANT ALL PRIVILEGES TO bookstore';
DBMS_OUTPUT.PUT_LINE('User BOOKSTORE created successfully');
ELSE
        DBMS_OUTPUT.PUT_LINE('User BOOKSTORE already exists');
END IF;
END;
/