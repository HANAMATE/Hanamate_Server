insert into my_wallets values("my", 1, 200000);
insert into my_wallets values("my", 2, 1000);
insert into my_wallets values("my", 3, 2000);
insert into my_wallets values("my", 4, 0);
insert into my_wallets values("my", 5, 10000);

insert into users(create_date, modified_date, login_id, rnn, name, password, phone_number, user_type, account_user_id, my_wallet_id) values('2023-08-14 17:28:25.306829', '2023-08-14 17:28:25.306829', 'test1', '001004-3234567', '최안식', '$2a$10$a6nTLbQFCipdRD3xqGLLn.EA95hTmdrCtU6kZ0oK99Fr3x13eMV0G', '010-0987-6543', 'Parent', NULL, 1);
insert into users(create_date, modified_date, login_id, rnn, name, password, phone_number, user_type, account_user_id, my_wallet_id) values('2023-08-14 17:28:25.306829', '2023-08-14 17:28:25.306829', 'test2', '001004-4234568', '민새미', '$2a$10$a6nTLbQFCipdRD3xqGLLn.EA95hTmdrCtU6kZ0oK99Fr3x13eMV0G', '010-1234-5678', 'Child', NULL, 2);
insert into users(create_date, modified_date, login_id, rnn, name, password, phone_number, user_type, account_user_id, my_wallet_id) values('2023-08-14 17:28:25.306829', '2023-08-14 17:28:25.306829', 'test3', '971004-2234568', '권민선', '$2a$10$a6nTLbQFCipdRD3xqGLLn.EA95hTmdrCtU6kZ0oK99Fr3x13eMV0G', '010-4321-8765', 'Child', NULL, 3);
insert into users(create_date, modified_date, login_id, rnn, name, password, phone_number, user_type, account_user_id, my_wallet_id) values('2023-08-14 17:28:25.306829', '2023-08-14 17:28:25.306829', 'test4', '991004-1234568', '김민재', '$2a$10$a6nTLbQFCipdRD3xqGLLn.EA95hTmdrCtU6kZ0oK99Fr3x13eMV0G', '010-4444-3333', 'Parent', NULL, 4);
insert into users(create_date, modified_date, login_id, rnn, name, password, phone_number, user_type, account_user_id, my_wallet_id) values('2023-08-14 17:28:25.306829', '2023-08-14 17:28:25.306829', 'test5', '981004-2234568', '강민경', '$2a$10$a6nTLbQFCipdRD3xqGLLn.EA95hTmdrCtU6kZ0oK99Fr3x13eMV0G', '010-2222-1111', 'Child', NULL, 5);

insert into user_roles (user_user_id, roles) values (1, 'ROLE_USER');
insert into user_roles (user_user_id, roles) values (2, 'ROLE_USER');
insert into user_roles (user_user_id, roles) values (3, 'ROLE_USER');
insert into user_roles (user_user_id, roles) values (4, 'ROLE_USER');
insert into user_roles (user_user_id, roles) values (5, 'ROLE_USER');

insert into accounts(user_id, account_id, balance, name) values(1, 12345678, 1000000, '하나자유입출금통장');

insert into my_wallets (balance, dtype, wallet_id) values (0, 'moim',6);
insert into my_wallets (balance, dtype, wallet_id) values (0, 'moim',7);
insert into my_wallets (balance, dtype, wallet_id) values (0, 'moim',8);

insert into moim_wallet (target_amount, wallet_id) values (100, 6);
insert into moim_wallet (target_amount, wallet_id) values (10000, 7);
insert into moim_wallet (target_amount, wallet_id) values (1000000, 8);

insert into parent_and_child (id ,child_idx, parent_idx) values (1, 3, 1);
insert into parent_and_child (id ,child_idx, parent_idx) values (2, 5, 4);

-- insert into wallets_and_users (moim_wallet_id, user_id) values (1, 1);

insert into allowances (allowance_id, allowance_amount, children_idx, parent_idx, valid) values (1, 20000, 3, 1, true);

-- insert into loans (loan_id, balance, child_id, completed, duration, end_date, interest_rate, loan_amount, loan_message, loan_name, parent_id, payment_method, sequence, start_date, total_interest_rate, total_repayment_amount, valid, wallet_id) values (1, 10017, 3, true, 91, '2023-11-21T09:00:00.000+0900', 1, 100, '긴히 쓸일이 있어용', 'test1234', 1, '원금균등상환', 3, '2023-08-21T09:00:00.000+0900', 17, 10017, true, 3);

