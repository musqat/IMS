-- =============================================================================
-- IMS 데모용 Seed 데이터 v4
-- =============================================================================
-- 회사 구조 (스마트홈 디바이스 공급망):
--
--   이스마트코리아(E) ──→ 아이테크조립(A) ──→ 비전전자(B)
--                         └─ [초대중] ──→ 디로지스(D)    씨메카닉스(C)
--
--   파트너십:
--     E → A : ACCEPTED  (E 브랜드사가 A 조립사를 하청으로)
--     A → B : ACCEPTED  (A가 전자부품 공급사 B를 하청으로)
--     A → C : ACCEPTED  (A가 기계부품 공급사 C를 하청으로)
--     A → D : PENDING   (A가 물류사 D를 초대 중)
--
-- 테스트 계정 (비밀번호 공통: Test1234!):
--   a@ims.dev → 아이테크조립(주)   [메인 데모 계정]
--   b@ims.dev → 비전전자(주)
--   c@ims.dev → 씨메카닉스(주)
--   d@ims.dev → 디로지스(주)       [초대 대기 중]
--   e@ims.dev → 이스마트코리아(주) [A의 본사]
--
-- 데이터 범위: 90일치 (분기 프리셋 차트 완전 표시)
-- 완제품: F001 스마트스피커 · F002 스마트허브 · F003 스마트플러그 · F004 스마트카메라 · F005 스마트도어락
-- 생산기록: F001 25건 · F002 23건 · F003 11건 · F004 9건 · F005 7건 (총 75건)
-- ANOMALY: 20건 — P001(7), P002(5), P005(2), P009(2), P010(2), P004(1), P006(1), P007(1), P008(1)
-- Top5 바 차트: P001(7), P002(5), P005(2), P009(2), P010(2)
--
-- [자동] docker-compose up --build 시 DataInitializer가 자동 실행
-- [수동] docker exec -i ims-postgres psql -U ims -d ims -f - < backend/src/main/resources/seed.sql
-- =============================================================================

-- DB 선택은 접속 시점에 결정된다 (JDBC URL 또는 psql -d 인자).
-- 여기서 USE로 고정하지 않아야 DB명이 다른 호스트에서도 그대로 돈다.

-- PostgreSQL에는 세션 변수(@x)가 없다.
-- 아래 임시 테이블이 그 역할을 대신한다. 커넥션 종료 시 자동 소멸한다.
CREATE TEMP TABLE seed_ref (k text PRIMARY KEY, id bigint);

INSERT INTO seed_ref (k, id) VALUES ('A', (SELECT id FROM users WHERE email = 'a@ims.dev'));
INSERT INTO seed_ref (k, id) VALUES ('B', (SELECT id FROM users WHERE email = 'b@ims.dev'));
INSERT INTO seed_ref (k, id) VALUES ('C', (SELECT id FROM users WHERE email = 'c@ims.dev'));
INSERT INTO seed_ref (k, id) VALUES ('D', (SELECT id FROM users WHERE email = 'd@ims.dev'));
INSERT INTO seed_ref (k, id) VALUES ('E', (SELECT id FROM users WHERE email = 'e@ims.dev'));
-- -----------------------------------------------------------------------------
-- Partnership
-- -----------------------------------------------------------------------------
INSERT INTO partnerships (main_id, sub_id, status, invite_token, alias, created_at, accepted_at) VALUES
((SELECT id FROM seed_ref WHERE k='E'), (SELECT id FROM seed_ref WHERE k='A'), 'ACCEPTED', gen_random_uuid()::text, 'A(조립파트너)',  NOW() - INTERVAL '95 day', NOW() - INTERVAL '94 day'),
((SELECT id FROM seed_ref WHERE k='A'), (SELECT id FROM seed_ref WHERE k='B'), 'ACCEPTED', gen_random_uuid()::text, '비전전자',       NOW() - INTERVAL '92 day', NOW() - INTERVAL '91 day'),
((SELECT id FROM seed_ref WHERE k='A'), (SELECT id FROM seed_ref WHERE k='C'), 'ACCEPTED', gen_random_uuid()::text, '씨메카닉스',     NOW() - INTERVAL '92 day', NOW() - INTERVAL '91 day'),
((SELECT id FROM seed_ref WHERE k='A'), (SELECT id FROM seed_ref WHERE k='D'), 'PENDING',  gen_random_uuid()::text, NULL,             NOW() - INTERVAL '5 day',  NULL);

-- -----------------------------------------------------------------------------
-- Warehouse
-- -----------------------------------------------------------------------------
INSERT INTO warehouses (owner_id, name, location, created_at, updated_at) VALUES
((SELECT id FROM seed_ref WHERE k='A'), '서울 조립창고',     '서울특별시 금천구 디지털로',  NOW() - INTERVAL '93 day', NOW()),
((SELECT id FROM seed_ref WHERE k='A'), '경기 부품창고',     '경기도 화성시 동탄산업로',    NOW() - INTERVAL '93 day', NOW()),
((SELECT id FROM seed_ref WHERE k='B'), '인천 전자부품창고', '인천광역시 남동구 논현로',    NOW() - INTERVAL '91 day', NOW()),
((SELECT id FROM seed_ref WHERE k='C'), '대구 기계부품창고', '대구광역시 달성군 논공읍',    NOW() - INTERVAL '91 day', NOW()),
((SELECT id FROM seed_ref WHERE k='E'), '이스마트 서울창고', '서울특별시 마포구 월드컵로',  NOW() - INTERVAL '94 day', NOW());

INSERT INTO seed_ref (k, id) VALUES ('wA1', (SELECT id FROM warehouses WHERE name = '서울 조립창고'     AND owner_id = (SELECT id FROM seed_ref WHERE k='A')));
INSERT INTO seed_ref (k, id) VALUES ('wA2', (SELECT id FROM warehouses WHERE name = '경기 부품창고'     AND owner_id = (SELECT id FROM seed_ref WHERE k='A')));
INSERT INTO seed_ref (k, id) VALUES ('wB', (SELECT id FROM warehouses WHERE name = '인천 전자부품창고' AND owner_id = (SELECT id FROM seed_ref WHERE k='B')));
INSERT INTO seed_ref (k, id) VALUES ('wC', (SELECT id FROM warehouses WHERE name = '대구 기계부품창고' AND owner_id = (SELECT id FROM seed_ref WHERE k='C')));
INSERT INTO seed_ref (k, id) VALUES ('wE', (SELECT id FROM warehouses WHERE name = '이스마트 서울창고' AND owner_id = (SELECT id FROM seed_ref WHERE k='E')));
-- -----------------------------------------------------------------------------
-- WarehouseShare
-- -----------------------------------------------------------------------------
INSERT INTO warehouse_shares (warehouse_id, shared_with_id, permission, created_at) VALUES
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='E'), 'VIEW', NOW() - INTERVAL '94 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='B'), 'VIEW', NOW() - INTERVAL '91 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='C'), 'VIEW', NOW() - INTERVAL '91 day'),
((SELECT id FROM seed_ref WHERE k='wB'),  (SELECT id FROM seed_ref WHERE k='A'), 'FULL', NOW() - INTERVAL '91 day'),
((SELECT id FROM seed_ref WHERE k='wC'),  (SELECT id FROM seed_ref WHERE k='A'), 'FULL', NOW() - INTERVAL '91 day'),
((SELECT id FROM seed_ref WHERE k='wE'),  (SELECT id FROM seed_ref WHERE k='A'), 'VIEW', NOW() - INTERVAL '94 day');

-- -----------------------------------------------------------------------------
-- Item (A 소유 — 스마트홈 디바이스 공급망)
--
-- BOM:
--   스마트스피커(F001) = S001×1 + P003×1 + P004×2 + P006×1 + P008×1
--   메인보드(S001)     = P001×1 + P002×1 + P005×4
--   스마트허브(F002)   = S001×1 + P007×1 + P008×1
-- -----------------------------------------------------------------------------
INSERT INTO items (owner_id, item_code, name, type, description, created_at, updated_at) VALUES
((SELECT id FROM seed_ref WHERE k='A'), 'F001', '스마트스피커',    'PRODUCT', 'AI 음성인식 스마트스피커',         NOW() - INTERVAL '93 day', NOW()),
((SELECT id FROM seed_ref WHERE k='A'), 'F002', '스마트허브',      'PRODUCT', 'IoT 기기 통합 제어 허브',          NOW() - INTERVAL '93 day', NOW()),
((SELECT id FROM seed_ref WHERE k='A'), 'F003', '스마트플러그',    'PRODUCT', 'AI 제어 스마트 전원 플러그',       NOW() - INTERVAL '85 day', NOW()),
((SELECT id FROM seed_ref WHERE k='A'), 'F004', '스마트카메라',    'PRODUCT', 'Full HD 홈 보안 카메라',           NOW() - INTERVAL '80 day', NOW()),
((SELECT id FROM seed_ref WHERE k='A'), 'F005', '스마트도어락',    'PRODUCT', '지문/앱 제어 스마트 도어락',       NOW() - INTERVAL '75 day', NOW()),
((SELECT id FROM seed_ref WHERE k='A'), 'S001', '메인보드(반제품)', 'SEMI',   'WiFi/BT 탑재 메인 PCB 조립체',     NOW() - INTERVAL '93 day', NOW()),
((SELECT id FROM seed_ref WHERE k='A'), 'P001', 'PCB기판',         'PART',   '6층 메인보드 기판',                 NOW() - INTERVAL '93 day', NOW()),
((SELECT id FROM seed_ref WHERE k='A'), 'P002', 'WiFi/BT모듈',     'PART',   'Wi-Fi 6 + BT 5.0 콤보모듈',        NOW() - INTERVAL '93 day', NOW()),
((SELECT id FROM seed_ref WHERE k='A'), 'P003', '스피커유닛',      'PART',   '40mm 풀레인지 드라이버',            NOW() - INTERVAL '93 day', NOW()),
((SELECT id FROM seed_ref WHERE k='A'), 'P004', '마이크모듈',      'PART',   '360° 빔포밍 마이크 어레이',         NOW() - INTERVAL '93 day', NOW()),
((SELECT id FROM seed_ref WHERE k='A'), 'P005', '전원IC',          'PART',   'PMIC 전원관리 IC',                  NOW() - INTERVAL '93 day', NOW()),
((SELECT id FROM seed_ref WHERE k='A'), 'P006', '케이스(소)',       'PART',   '스피커/플러그/카메라용 ABS 하우징', NOW() - INTERVAL '93 day', NOW()),
((SELECT id FROM seed_ref WHERE k='A'), 'P007', '케이스(대)',       'PART',   '허브/도어락용 ABS 하우징',          NOW() - INTERVAL '93 day', NOW()),
((SELECT id FROM seed_ref WHERE k='A'), 'P008', 'DC어댑터',        'PART',   '12V 2A DC 전원 어댑터',             NOW() - INTERVAL '93 day', NOW()),
((SELECT id FROM seed_ref WHERE k='A'), 'P009', '카메라센서모듈',  'PART',   '1/2.9" CMOS 이미지센서 모듈',       NOW() - INTERVAL '80 day', NOW()),
((SELECT id FROM seed_ref WHERE k='A'), 'P010', '배터리팩',        'PART',   '3.7V 3000mAh 리튬 배터리',         NOW() - INTERVAL '75 day', NOW());

INSERT INTO seed_ref (k, id) VALUES ('f001', (SELECT id FROM items WHERE item_code = 'F001' AND owner_id = (SELECT id FROM seed_ref WHERE k='A')));
INSERT INTO seed_ref (k, id) VALUES ('f002', (SELECT id FROM items WHERE item_code = 'F002' AND owner_id = (SELECT id FROM seed_ref WHERE k='A')));
INSERT INTO seed_ref (k, id) VALUES ('f003', (SELECT id FROM items WHERE item_code = 'F003' AND owner_id = (SELECT id FROM seed_ref WHERE k='A')));
INSERT INTO seed_ref (k, id) VALUES ('f004', (SELECT id FROM items WHERE item_code = 'F004' AND owner_id = (SELECT id FROM seed_ref WHERE k='A')));
INSERT INTO seed_ref (k, id) VALUES ('f005', (SELECT id FROM items WHERE item_code = 'F005' AND owner_id = (SELECT id FROM seed_ref WHERE k='A')));
INSERT INTO seed_ref (k, id) VALUES ('s001', (SELECT id FROM items WHERE item_code = 'S001' AND owner_id = (SELECT id FROM seed_ref WHERE k='A')));
INSERT INTO seed_ref (k, id) VALUES ('p001', (SELECT id FROM items WHERE item_code = 'P001' AND owner_id = (SELECT id FROM seed_ref WHERE k='A')));
INSERT INTO seed_ref (k, id) VALUES ('p002', (SELECT id FROM items WHERE item_code = 'P002' AND owner_id = (SELECT id FROM seed_ref WHERE k='A')));
INSERT INTO seed_ref (k, id) VALUES ('p003', (SELECT id FROM items WHERE item_code = 'P003' AND owner_id = (SELECT id FROM seed_ref WHERE k='A')));
INSERT INTO seed_ref (k, id) VALUES ('p004', (SELECT id FROM items WHERE item_code = 'P004' AND owner_id = (SELECT id FROM seed_ref WHERE k='A')));
INSERT INTO seed_ref (k, id) VALUES ('p005', (SELECT id FROM items WHERE item_code = 'P005' AND owner_id = (SELECT id FROM seed_ref WHERE k='A')));
INSERT INTO seed_ref (k, id) VALUES ('p006', (SELECT id FROM items WHERE item_code = 'P006' AND owner_id = (SELECT id FROM seed_ref WHERE k='A')));
INSERT INTO seed_ref (k, id) VALUES ('p007', (SELECT id FROM items WHERE item_code = 'P007' AND owner_id = (SELECT id FROM seed_ref WHERE k='A')));
INSERT INTO seed_ref (k, id) VALUES ('p008', (SELECT id FROM items WHERE item_code = 'P008' AND owner_id = (SELECT id FROM seed_ref WHERE k='A')));
INSERT INTO seed_ref (k, id) VALUES ('p009', (SELECT id FROM items WHERE item_code = 'P009' AND owner_id = (SELECT id FROM seed_ref WHERE k='A')));
INSERT INTO seed_ref (k, id) VALUES ('p010', (SELECT id FROM items WHERE item_code = 'P010' AND owner_id = (SELECT id FROM seed_ref WHERE k='A')));
-- -----------------------------------------------------------------------------
-- BOM
-- -----------------------------------------------------------------------------
INSERT INTO boms (parent_item_id, child_item_id, quantity) VALUES
-- 스마트스피커: 메인보드+스피커+마이크×2+케이스소+어댑터
((SELECT id FROM seed_ref WHERE k='f001'), (SELECT id FROM seed_ref WHERE k='s001'), 1), ((SELECT id FROM seed_ref WHERE k='f001'), (SELECT id FROM seed_ref WHERE k='p003'), 1), ((SELECT id FROM seed_ref WHERE k='f001'), (SELECT id FROM seed_ref WHERE k='p004'), 2), ((SELECT id FROM seed_ref WHERE k='f001'), (SELECT id FROM seed_ref WHERE k='p006'), 1), ((SELECT id FROM seed_ref WHERE k='f001'), (SELECT id FROM seed_ref WHERE k='p008'), 1),
-- 메인보드(반제품): PCB+WiFi모듈+전원IC×4
((SELECT id FROM seed_ref WHERE k='s001'), (SELECT id FROM seed_ref WHERE k='p001'), 1), ((SELECT id FROM seed_ref WHERE k='s001'), (SELECT id FROM seed_ref WHERE k='p002'), 1), ((SELECT id FROM seed_ref WHERE k='s001'), (SELECT id FROM seed_ref WHERE k='p005'), 4),
-- 스마트허브: 메인보드+케이스대+어댑터
((SELECT id FROM seed_ref WHERE k='f002'), (SELECT id FROM seed_ref WHERE k='s001'), 1), ((SELECT id FROM seed_ref WHERE k='f002'), (SELECT id FROM seed_ref WHERE k='p007'), 1), ((SELECT id FROM seed_ref WHERE k='f002'), (SELECT id FROM seed_ref WHERE k='p008'), 1),
-- 스마트플러그: WiFi모듈+전원IC×2+케이스소+어댑터 (메인보드 없음 — 경량 컨트롤러)
((SELECT id FROM seed_ref WHERE k='f003'), (SELECT id FROM seed_ref WHERE k='p002'), 1), ((SELECT id FROM seed_ref WHERE k='f003'), (SELECT id FROM seed_ref WHERE k='p005'), 2), ((SELECT id FROM seed_ref WHERE k='f003'), (SELECT id FROM seed_ref WHERE k='p006'), 1), ((SELECT id FROM seed_ref WHERE k='f003'), (SELECT id FROM seed_ref WHERE k='p008'), 1),
-- 스마트카메라: 메인보드+카메라센서+마이크+케이스소+어댑터
((SELECT id FROM seed_ref WHERE k='f004'), (SELECT id FROM seed_ref WHERE k='s001'), 1), ((SELECT id FROM seed_ref WHERE k='f004'), (SELECT id FROM seed_ref WHERE k='p009'), 1), ((SELECT id FROM seed_ref WHERE k='f004'), (SELECT id FROM seed_ref WHERE k='p004'), 1), ((SELECT id FROM seed_ref WHERE k='f004'), (SELECT id FROM seed_ref WHERE k='p006'), 1), ((SELECT id FROM seed_ref WHERE k='f004'), (SELECT id FROM seed_ref WHERE k='p008'), 1),
-- 스마트도어락: 메인보드+배터리팩+케이스대+어댑터
((SELECT id FROM seed_ref WHERE k='f005'), (SELECT id FROM seed_ref WHERE k='s001'), 1), ((SELECT id FROM seed_ref WHERE k='f005'), (SELECT id FROM seed_ref WHERE k='p010'), 1), ((SELECT id FROM seed_ref WHERE k='f005'), (SELECT id FROM seed_ref WHERE k='p007'), 1), ((SELECT id FROM seed_ref WHERE k='f005'), (SELECT id FROM seed_ref WHERE k='p008'), 1);

-- -----------------------------------------------------------------------------
-- Inventory (현재 재고량)
-- ★ = 안전재고 미달 상태
-- -----------------------------------------------------------------------------
INSERT INTO inventories (warehouse_id, item_id, quantity, safety_stock, created_at, updated_at) VALUES
-- wA1 서울 조립창고
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f001'),  68,  20, NOW() - INTERVAL '93 day', NOW()),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f002'),  34,  10, NOW() - INTERVAL '93 day', NOW()),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='s001'), 112,  30, NOW() - INTERVAL '93 day', NOW()),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='p001'),   7,  20, NOW() - INTERVAL '93 day', NOW()),   -- ★ 안전재고 미달
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='p002'), 175,  50, NOW() - INTERVAL '93 day', NOW()),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='p003'), 205,  40, NOW() - INTERVAL '93 day', NOW()),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='p004'), 145,  40, NOW() - INTERVAL '93 day', NOW()),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='p005'), 310,  80, NOW() - INTERVAL '93 day', NOW()),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='p006'), 240,  60, NOW() - INTERVAL '93 day', NOW()),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='p007'), 125,  40, NOW() - INTERVAL '93 day', NOW()),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='p008'),  42,  15, NOW() - INTERVAL '93 day', NOW()),
-- wA2 경기 부품창고
((SELECT id FROM seed_ref WHERE k='wA2'), (SELECT id FROM seed_ref WHERE k='p001'),  11,  20, NOW() - INTERVAL '91 day', NOW()),   -- ★ 안전재고 미달
((SELECT id FROM seed_ref WHERE k='wA2'), (SELECT id FROM seed_ref WHERE k='p005'), 380, 100, NOW() - INTERVAL '91 day', NOW()),
((SELECT id FROM seed_ref WHERE k='wA2'), (SELECT id FROM seed_ref WHERE k='p008'), 190,  50, NOW() - INTERVAL '91 day', NOW()),
-- wB 인천 전자부품창고
((SELECT id FROM seed_ref WHERE k='wB'),  (SELECT id FROM seed_ref WHERE k='p001'), 480, 100, NOW() - INTERVAL '91 day', NOW()),
((SELECT id FROM seed_ref WHERE k='wB'),  (SELECT id FROM seed_ref WHERE k='p002'), 290,  80, NOW() - INTERVAL '91 day', NOW()),
((SELECT id FROM seed_ref WHERE k='wB'),  (SELECT id FROM seed_ref WHERE k='p005'), 760, 200, NOW() - INTERVAL '91 day', NOW()),
-- wC 대구 기계부품창고
((SELECT id FROM seed_ref WHERE k='wC'),  (SELECT id FROM seed_ref WHERE k='p003'), 580, 100, NOW() - INTERVAL '91 day', NOW()),
((SELECT id FROM seed_ref WHERE k='wC'),  (SELECT id FROM seed_ref WHERE k='p004'), 390,  80, NOW() - INTERVAL '91 day', NOW()),
((SELECT id FROM seed_ref WHERE k='wC'),  (SELECT id FROM seed_ref WHERE k='p006'), 680, 150, NOW() - INTERVAL '91 day', NOW()),
((SELECT id FROM seed_ref WHERE k='wC'),  (SELECT id FROM seed_ref WHERE k='p007'), 340,  80, NOW() - INTERVAL '91 day', NOW()),
((SELECT id FROM seed_ref WHERE k='wC'),  (SELECT id FROM seed_ref WHERE k='p008'), 490, 100, NOW() - INTERVAL '91 day', NOW()),
-- wE 이스마트 서울창고
((SELECT id FROM seed_ref WHERE k='wE'),  (SELECT id FROM seed_ref WHERE k='f001'), 168,  30, NOW() - INTERVAL '94 day', NOW()),
((SELECT id FROM seed_ref WHERE k='wE'),  (SELECT id FROM seed_ref WHERE k='f002'),  84,  20, NOW() - INTERVAL '94 day', NOW()),
((SELECT id FROM seed_ref WHERE k='wE'),  (SELECT id FROM seed_ref WHERE k='f003'), 215,  50, NOW() - INTERVAL '84 day', NOW()),
((SELECT id FROM seed_ref WHERE k='wE'),  (SELECT id FROM seed_ref WHERE k='f004'),  52,  15, NOW() - INTERVAL '79 day', NOW()),
((SELECT id FROM seed_ref WHERE k='wE'),  (SELECT id FROM seed_ref WHERE k='f005'),  19,  10, NOW() - INTERVAL '74 day', NOW()),
-- wA1 신규 완제품 + 신규 부품
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f003'),  45,  20, NOW() - INTERVAL '85 day', NOW()),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f004'),  18,  10, NOW() - INTERVAL '80 day', NOW()),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f005'),   8,  10, NOW() - INTERVAL '75 day', NOW()),   -- ★ 안전재고 미달
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='p009'),  65,  20, NOW() - INTERVAL '80 day', NOW()),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='p010'),  32,  15, NOW() - INTERVAL '75 day', NOW());   -- ★ 안전재고 미달

-- inventory ID 변수
INSERT INTO seed_ref (k, id) VALUES ('inv_wA1_f001', (SELECT id FROM inventories WHERE warehouse_id = (SELECT id FROM seed_ref WHERE k='wA1') AND item_id = (SELECT id FROM seed_ref WHERE k='f001')));
INSERT INTO seed_ref (k, id) VALUES ('inv_wA1_f002', (SELECT id FROM inventories WHERE warehouse_id = (SELECT id FROM seed_ref WHERE k='wA1') AND item_id = (SELECT id FROM seed_ref WHERE k='f002')));
INSERT INTO seed_ref (k, id) VALUES ('inv_wA1_p001', (SELECT id FROM inventories WHERE warehouse_id = (SELECT id FROM seed_ref WHERE k='wA1') AND item_id = (SELECT id FROM seed_ref WHERE k='p001')));
INSERT INTO seed_ref (k, id) VALUES ('inv_wA1_p002', (SELECT id FROM inventories WHERE warehouse_id = (SELECT id FROM seed_ref WHERE k='wA1') AND item_id = (SELECT id FROM seed_ref WHERE k='p002')));
INSERT INTO seed_ref (k, id) VALUES ('inv_wA1_p003', (SELECT id FROM inventories WHERE warehouse_id = (SELECT id FROM seed_ref WHERE k='wA1') AND item_id = (SELECT id FROM seed_ref WHERE k='p003')));
INSERT INTO seed_ref (k, id) VALUES ('inv_wA1_p004', (SELECT id FROM inventories WHERE warehouse_id = (SELECT id FROM seed_ref WHERE k='wA1') AND item_id = (SELECT id FROM seed_ref WHERE k='p004')));
INSERT INTO seed_ref (k, id) VALUES ('inv_wA1_p005', (SELECT id FROM inventories WHERE warehouse_id = (SELECT id FROM seed_ref WHERE k='wA1') AND item_id = (SELECT id FROM seed_ref WHERE k='p005')));
INSERT INTO seed_ref (k, id) VALUES ('inv_wA1_p006', (SELECT id FROM inventories WHERE warehouse_id = (SELECT id FROM seed_ref WHERE k='wA1') AND item_id = (SELECT id FROM seed_ref WHERE k='p006')));
INSERT INTO seed_ref (k, id) VALUES ('inv_wA1_p007', (SELECT id FROM inventories WHERE warehouse_id = (SELECT id FROM seed_ref WHERE k='wA1') AND item_id = (SELECT id FROM seed_ref WHERE k='p007')));
INSERT INTO seed_ref (k, id) VALUES ('inv_wA1_p008', (SELECT id FROM inventories WHERE warehouse_id = (SELECT id FROM seed_ref WHERE k='wA1') AND item_id = (SELECT id FROM seed_ref WHERE k='p008')));
INSERT INTO seed_ref (k, id) VALUES ('inv_wA1_p009', (SELECT id FROM inventories WHERE warehouse_id = (SELECT id FROM seed_ref WHERE k='wA1') AND item_id = (SELECT id FROM seed_ref WHERE k='p009')));
INSERT INTO seed_ref (k, id) VALUES ('inv_wA1_p010', (SELECT id FROM inventories WHERE warehouse_id = (SELECT id FROM seed_ref WHERE k='wA1') AND item_id = (SELECT id FROM seed_ref WHERE k='p010')));
INSERT INTO seed_ref (k, id) VALUES ('inv_wA1_f003', (SELECT id FROM inventories WHERE warehouse_id = (SELECT id FROM seed_ref WHERE k='wA1') AND item_id = (SELECT id FROM seed_ref WHERE k='f003')));
INSERT INTO seed_ref (k, id) VALUES ('inv_wA1_f004', (SELECT id FROM inventories WHERE warehouse_id = (SELECT id FROM seed_ref WHERE k='wA1') AND item_id = (SELECT id FROM seed_ref WHERE k='f004')));
INSERT INTO seed_ref (k, id) VALUES ('inv_wA1_f005', (SELECT id FROM inventories WHERE warehouse_id = (SELECT id FROM seed_ref WHERE k='wA1') AND item_id = (SELECT id FROM seed_ref WHERE k='f005')));
INSERT INTO seed_ref (k, id) VALUES ('inv_wE_f001', (SELECT id FROM inventories WHERE warehouse_id = (SELECT id FROM seed_ref WHERE k='wE')  AND item_id = (SELECT id FROM seed_ref WHERE k='f001')));
INSERT INTO seed_ref (k, id) VALUES ('inv_wE_f002', (SELECT id FROM inventories WHERE warehouse_id = (SELECT id FROM seed_ref WHERE k='wE')  AND item_id = (SELECT id FROM seed_ref WHERE k='f002')));
INSERT INTO seed_ref (k, id) VALUES ('inv_wE_f003', (SELECT id FROM inventories WHERE warehouse_id = (SELECT id FROM seed_ref WHERE k='wE')  AND item_id = (SELECT id FROM seed_ref WHERE k='f003')));
INSERT INTO seed_ref (k, id) VALUES ('inv_wE_f004', (SELECT id FROM inventories WHERE warehouse_id = (SELECT id FROM seed_ref WHERE k='wE')  AND item_id = (SELECT id FROM seed_ref WHERE k='f004')));
INSERT INTO seed_ref (k, id) VALUES ('inv_wE_f005', (SELECT id FROM inventories WHERE warehouse_id = (SELECT id FROM seed_ref WHERE k='wE')  AND item_id = (SELECT id FROM seed_ref WHERE k='f005')));
-- -----------------------------------------------------------------------------
-- InventoryHistory (90일치 — 분기 프리셋 분석 차트 완전 표시)
-- F001 OUT 이력: 30회 이상 → 안전재고 추천 활성화 (MIN_DAYS=7 충족)
-- -----------------------------------------------------------------------------
INSERT INTO inventory_histories (inventory_id, type, delta, memo, created_at) VALUES

-- ── 스마트스피커 F001 (wA1) ─────────────────────────────────────────────────
-- 초기 대량 입고
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'IN',    800, '초기 입고',               NOW() - INTERVAL '90 day'),
-- 90~61일: 격일~3일 간격 출고
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -25, '이스마트 납품 (1차)',      NOW() - INTERVAL '88 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -18, '소매 채널 출고',           NOW() - INTERVAL '86 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -22, '이스마트 납품 (2차)',      NOW() - INTERVAL '84 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -15, '긴급 출고',                NOW() - INTERVAL '82 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'IN',    200, '생산 완료 입고',           NOW() - INTERVAL '80 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -20, '이스마트 납품 (3차)',      NOW() - INTERVAL '79 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -17, '소매 채널 출고',           NOW() - INTERVAL '77 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -28, '이스마트 납품 (4차)',      NOW() - INTERVAL '75 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -12, '긴급 출고',                NOW() - INTERVAL '73 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'PRODUCTION_DEDUCTION', -45, '생산 차감', NOW() - INTERVAL '71 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'IN',    190, '생산 완료 입고',           NOW() - INTERVAL '70 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -23, '이스마트 납품 (5차)',      NOW() - INTERVAL '68 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -19, '소매 채널 출고',           NOW() - INTERVAL '66 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -30, '이스마트 납품 (6차)',      NOW() - INTERVAL '64 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -14, '긴급 출고',                NOW() - INTERVAL '62 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'PRODUCTION_DEDUCTION', -48, '생산 차감', NOW() - INTERVAL '60 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'IN',    210, '생산 완료 입고',           NOW() - INTERVAL '58 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -26, '이스마트 납품 (7차)',      NOW() - INTERVAL '57 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -21, '소매 채널 출고',           NOW() - INTERVAL '55 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -32, '이스마트 납품 (8차)',      NOW() - INTERVAL '53 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -16, '긴급 출고',                NOW() - INTERVAL '51 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'PRODUCTION_DEDUCTION', -52, '생산 차감', NOW() - INTERVAL '50 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'IN',    220, '생산 완료 입고',           NOW() - INTERVAL '48 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -29, '이스마트 납품 (9차)',      NOW() - INTERVAL '47 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -24, '소매 채널 출고',           NOW() - INTERVAL '45 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -35, '이스마트 납품 (10차)',     NOW() - INTERVAL '43 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -11, '긴급 출고',                NOW() - INTERVAL '41 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'PRODUCTION_DEDUCTION', -41, '생산 차감', NOW() - INTERVAL '40 day'),
-- 40~11일: 기존 이력 유지 + 추가
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'IN',    130, '생산 완료 입고',           NOW() - INTERVAL '38 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -18, '이스마트 납품 (11차)',     NOW() - INTERVAL '37 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -12, '소매 채널 출고',           NOW() - INTERVAL '35 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -20, '이스마트 납품 (12차)',     NOW() - INTERVAL '33 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'PRODUCTION_DEDUCTION', -30, '생산 차감', NOW() - INTERVAL '31 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'IN',    130, '생산 완료 입고',           NOW() - INTERVAL '29 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -15, '이스마트 납품 (13차)',     NOW() - INTERVAL '28 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -11, '소매 채널 출고',           NOW() - INTERVAL '26 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -22, '이스마트 납품 (14차)',     NOW() - INTERVAL '24 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'PRODUCTION_DEDUCTION', -28, '생산 차감', NOW() - INTERVAL '23 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'IN',    140, '생산 완료 입고',           NOW() - INTERVAL '22 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -25, '이스마트 납품 (15차)',     NOW() - INTERVAL '21 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -14, '소매 채널 출고',           NOW() - INTERVAL '19 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -17, '이스마트 납품 (16차)',     NOW() - INTERVAL '17 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'PRODUCTION_DEDUCTION', -35, '생산 차감', NOW() - INTERVAL '16 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'IN',    140, '생산 완료 입고',           NOW() - INTERVAL '15 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -19, '이스마트 납품 (17차)',     NOW() - INTERVAL '14 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -10, '소매 채널 출고',           NOW() - INTERVAL '12 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -21, '이스마트 납품 (18차)',     NOW() - INTERVAL '10 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',    -8, '긴급 출고',                NOW() - INTERVAL '8 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'PRODUCTION_DEDUCTION', -26, '생산 차감', NOW() - INTERVAL '7 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'IN',    110, '생산 완료 입고',           NOW() - INTERVAL '6 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -16, '이스마트 납품 (19차)',     NOW() - INTERVAL '5 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -13, '소매 채널 출고',           NOW() - INTERVAL '3 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f001'), 'OUT',   -20, '이스마트 납품 (20차)',     NOW() - INTERVAL '1 day'),

-- ── 스마트허브 F002 (wA1) ──────────────────────────────────────────────────
((SELECT id FROM seed_ref WHERE k='inv_wA1_f002'), 'IN',    400, '초기 입고',               NOW() - INTERVAL '90 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f002'), 'OUT',   -15, '이스마트 납품',           NOW() - INTERVAL '87 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f002'), 'OUT',   -10, '소매 채널 출고',          NOW() - INTERVAL '83 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f002'), 'OUT',   -13, '이스마트 납품',           NOW() - INTERVAL '79 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f002'), 'PRODUCTION_DEDUCTION', -29, '생산 차감', NOW() - INTERVAL '77 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f002'), 'IN',    120, '생산 완료 입고',          NOW() - INTERVAL '75 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f002'), 'OUT',   -12, '이스마트 납품',           NOW() - INTERVAL '72 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f002'), 'OUT',    -9, '소매 채널 출고',          NOW() - INTERVAL '68 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f002'), 'OUT',   -11, '이스마트 납품',           NOW() - INTERVAL '64 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f002'), 'PRODUCTION_DEDUCTION', -27, '생산 차감', NOW() - INTERVAL '62 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f002'), 'IN',    110, '생산 완료 입고',          NOW() - INTERVAL '60 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f002'), 'OUT',   -14, '이스마트 납품',           NOW() - INTERVAL '57 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f002'), 'OUT',   -10, '소매 채널 출고',          NOW() - INTERVAL '53 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f002'), 'OUT',   -12, '이스마트 납품',           NOW() - INTERVAL '49 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f002'), 'PRODUCTION_DEDUCTION', -32, '생산 차감', NOW() - INTERVAL '47 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f002'), 'IN',    130, '생산 완료 입고',          NOW() - INTERVAL '45 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f002'), 'OUT',   -14, '이스마트 납품',           NOW() - INTERVAL '41 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f002'), 'OUT',   -10, '소매 채널 출고',          NOW() - INTERVAL '37 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f002'), 'OUT',   -11, '이스마트 납품',           NOW() - INTERVAL '33 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f002'), 'PRODUCTION_DEDUCTION', -20, '생산 차감', NOW() - INTERVAL '31 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f002'), 'IN',     80, '생산 완료 입고',          NOW() - INTERVAL '29 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f002'), 'OUT',   -14, '이스마트 납품',           NOW() - INTERVAL '25 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f002'), 'OUT',   -10, '소매 채널 출고',          NOW() - INTERVAL '21 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f002'), 'OUT',   -11, '이스마트 납품',           NOW() - INTERVAL '17 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f002'), 'PRODUCTION_DEDUCTION', -15, '생산 차감', NOW() - INTERVAL '15 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f002'), 'IN',     60, '생산 완료 입고',          NOW() - INTERVAL '13 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f002'), 'OUT',   -10, '이스마트 납품',           NOW() - INTERVAL '9 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f002'), 'OUT',    -8, '소매 채널 출고',          NOW() - INTERVAL '5 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f002'), 'OUT',   -10, '이스마트 납품',           NOW() - INTERVAL '2 day'),

-- ── 부품 이력 (wA1) ───────────────────────────────────────────────────────
((SELECT id FROM seed_ref WHERE k='inv_wA1_p001'), 'IN',    500, '비전전자 납품 입고',       NOW() - INTERVAL '90 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p001'), 'PRODUCTION_DEDUCTION', -120, '생산 차감', NOW() - INTERVAL '71 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p001'), 'PRODUCTION_DEDUCTION', -110, '생산 차감', NOW() - INTERVAL '50 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p001'), 'IN',    100, '비전전자 긴급 납품',       NOW() - INTERVAL '45 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p001'), 'PRODUCTION_DEDUCTION',  -90, '생산 차감', NOW() - INTERVAL '31 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p001'), 'PRODUCTION_DEDUCTION',  -68, '생산 차감', NOW() - INTERVAL '16 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p001'), 'PRODUCTION_DEDUCTION',  -35, '생산 차감', NOW() - INTERVAL '7 day'),

((SELECT id FROM seed_ref WHERE k='inv_wA1_p002'), 'IN',    600, '비전전자 납품 입고',       NOW() - INTERVAL '90 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p002'), 'IN',    100, '추가 입고',                NOW() - INTERVAL '50 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p002'), 'PRODUCTION_DEDUCTION', -220, '생산 차감', NOW() - INTERVAL '71 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p002'), 'PRODUCTION_DEDUCTION', -160, '생산 차감', NOW() - INTERVAL '50 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p002'), 'PRODUCTION_DEDUCTION', -145, '생산 차감', NOW() - INTERVAL '16 day'),

((SELECT id FROM seed_ref WHERE k='inv_wA1_p003'), 'IN',    700, '씨메카닉스 납품 입고',     NOW() - INTERVAL '90 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p003'), 'PRODUCTION_DEDUCTION', -120, '생산 차감', NOW() - INTERVAL '71 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p003'), 'IN',    150, '씨메카닉스 추가 납품',     NOW() - INTERVAL '45 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p003'), 'PRODUCTION_DEDUCTION', -130, '생산 차감', NOW() - INTERVAL '31 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p003'), 'PRODUCTION_DEDUCTION', -105, '생산 차감', NOW() - INTERVAL '7 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p003'), 'ADJUSTMENT',  -90, '실사 재고 조정',     NOW() - INTERVAL '3 day'),

((SELECT id FROM seed_ref WHERE k='inv_wA1_p004'), 'IN',    600, '씨메카닉스 납품 입고',     NOW() - INTERVAL '90 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p004'), 'PRODUCTION_DEDUCTION', -240, '생산 차감', NOW() - INTERVAL '71 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p004'), 'IN',    100, '씨메카닉스 추가 납품',     NOW() - INTERVAL '45 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p004'), 'PRODUCTION_DEDUCTION', -145, '생산 차감', NOW() - INTERVAL '31 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p004'), 'PRODUCTION_DEDUCTION',  -75, '생산 차감', NOW() - INTERVAL '7 day'),

((SELECT id FROM seed_ref WHERE k='inv_wA1_p005'), 'IN',    900, '비전전자 납품 입고',       NOW() - INTERVAL '90 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p005'), 'IN',    200, '추가 입고',                NOW() - INTERVAL '50 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p005'), 'PRODUCTION_DEDUCTION', -480, '생산 차감', NOW() - INTERVAL '71 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p005'), 'PRODUCTION_DEDUCTION', -180, '생산 차감', NOW() - INTERVAL '31 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p005'), 'PRODUCTION_DEDUCTION', -130, '생산 차감', NOW() - INTERVAL '7 day'),

((SELECT id FROM seed_ref WHERE k='inv_wA1_p006'), 'IN',    700, '씨메카닉스 납품 입고',     NOW() - INTERVAL '90 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p006'), 'PRODUCTION_DEDUCTION', -120, '생산 차감', NOW() - INTERVAL '71 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p006'), 'PRODUCTION_DEDUCTION', -130, '생산 차감', NOW() - INTERVAL '31 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p006'), 'IN',    200, '씨메카닉스 추가 납품',     NOW() - INTERVAL '15 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p006'), 'PRODUCTION_DEDUCTION', -105, '생산 차감', NOW() - INTERVAL '7 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p006'), 'ADJUSTMENT',  -65, '실사 재고 조정',     NOW() - INTERVAL '1 day'),

((SELECT id FROM seed_ref WHERE k='inv_wA1_p007'), 'IN',    400, '씨메카닉스 납품 입고',     NOW() - INTERVAL '90 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p007'), 'PRODUCTION_DEDUCTION', -130, '생산 차감', NOW() - INTERVAL '71 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p007'), 'PRODUCTION_DEDUCTION',  -90, '생산 차감', NOW() - INTERVAL '47 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p007'), 'IN',    100, '씨메카닉스 추가 납품',     NOW() - INTERVAL '30 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p007'), 'PRODUCTION_DEDUCTION',  -75, '생산 차감', NOW() - INTERVAL '15 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p007'), 'ADJUSTMENT',  -80, '실사 재고 조정',     NOW() - INTERVAL '2 day'),

((SELECT id FROM seed_ref WHERE k='inv_wA1_p008'), 'IN',    500, '납품 입고',                NOW() - INTERVAL '90 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p008'), 'IN',    100, '추가 입고',                NOW() - INTERVAL '60 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p008'), 'PRODUCTION_DEDUCTION', -200, '생산 차감', NOW() - INTERVAL '71 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p008'), 'PRODUCTION_DEDUCTION', -180, '생산 차감', NOW() - INTERVAL '47 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p008'), 'PRODUCTION_DEDUCTION', -100, '생산 차감', NOW() - INTERVAL '15 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p008'), 'ADJUSTMENT',  -78, '실사 재고 조정',     NOW() - INTERVAL '1 day'),

-- ── 이스마트 창고 (wE) ───────────────────────────────────────────────────
((SELECT id FROM seed_ref WHERE k='inv_wE_f001'), 'IN',     60, 'A사 1차 납품',             NOW() - INTERVAL '87 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f001'), 'OUT',   -35, '온라인몰 판매 출고',        NOW() - INTERVAL '80 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f001'), 'IN',     55, 'A사 2차 납품',             NOW() - INTERVAL '73 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f001'), 'OUT',   -40, '온라인몰 판매 출고',        NOW() - INTERVAL '65 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f001'), 'IN',     70, 'A사 3차 납품',             NOW() - INTERVAL '57 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f001'), 'OUT',   -45, '온라인몰 판매 출고',        NOW() - INTERVAL '50 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f001'), 'IN',     50, 'A사 4차 납품',             NOW() - INTERVAL '43 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f001'), 'OUT',   -30, '온라인몰 판매 출고',        NOW() - INTERVAL '35 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f001'), 'IN',     44, 'A사 5차 납품',             NOW() - INTERVAL '28 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f001'), 'OUT',   -40, '온라인몰 판매 출고',        NOW() - INTERVAL '20 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f001'), 'IN',     75, 'A사 6차 납품',             NOW() - INTERVAL '12 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f001'), 'OUT',   -50, '온라인몰 판매 출고',        NOW() - INTERVAL '6 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f001'), 'IN',     69, 'A사 7차 납품',             NOW() - INTERVAL '2 day'),

((SELECT id FROM seed_ref WHERE k='inv_wE_f002'), 'IN',     40, 'A사 1차 납품',             NOW() - INTERVAL '87 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f002'), 'OUT',   -22, '온라인몰 판매 출고',        NOW() - INTERVAL '75 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f002'), 'IN',     38, 'A사 2차 납품',             NOW() - INTERVAL '60 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f002'), 'OUT',   -25, '온라인몰 판매 출고',        NOW() - INTERVAL '48 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f002'), 'IN',     42, 'A사 3차 납품',             NOW() - INTERVAL '35 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f002'), 'OUT',   -30, '온라인몰 판매 출고',        NOW() - INTERVAL '22 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f002'), 'IN',     36, 'A사 4차 납품',             NOW() - INTERVAL '8 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f002'), 'OUT',   -15, '온라인몰 판매 출고',        NOW() - INTERVAL '3 day'),

-- ── 스마트플러그 F003 (wA1) ──────────────────────────────────────────────
((SELECT id FROM seed_ref WHERE k='inv_wA1_f003'), 'IN',    300, '초기 입고',               NOW() - INTERVAL '85 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f003'), 'OUT',   -30, '이스마트 납품 (1차)',     NOW() - INTERVAL '83 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f003'), 'OUT',   -22, '소매 채널 출고',          NOW() - INTERVAL '79 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f003'), 'OUT',   -28, '이스마트 납품 (2차)',     NOW() - INTERVAL '75 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f003'), 'IN',    200, '생산 완료 입고',          NOW() - INTERVAL '72 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f003'), 'OUT',   -25, '이스마트 납품 (3차)',     NOW() - INTERVAL '70 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f003'), 'PRODUCTION_DEDUCTION', -55, '생산 차감', NOW() - INTERVAL '68 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f003'), 'OUT',   -20, '소매 채널 출고',          NOW() - INTERVAL '65 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f003'), 'OUT',   -32, '이스마트 납품 (4차)',     NOW() - INTERVAL '61 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f003'), 'IN',    210, '생산 완료 입고',          NOW() - INTERVAL '58 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f003'), 'OUT',   -26, '이스마트 납품 (5차)',     NOW() - INTERVAL '55 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f003'), 'PRODUCTION_DEDUCTION', -63, '생산 차감', NOW() - INTERVAL '52 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f003'), 'OUT',   -18, '소매 채널 출고',          NOW() - INTERVAL '49 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f003'), 'OUT',   -35, '이스마트 납품 (6차)',     NOW() - INTERVAL '45 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f003'), 'IN',    220, '생산 완료 입고',          NOW() - INTERVAL '42 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f003'), 'OUT',   -28, '이스마트 납품 (7차)',     NOW() - INTERVAL '38 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f003'), 'PRODUCTION_DEDUCTION', -50, '생산 차감', NOW() - INTERVAL '35 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f003'), 'OUT',   -22, '소매 채널 출고',          NOW() - INTERVAL '31 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f003'), 'OUT',   -30, '이스마트 납품 (8차)',     NOW() - INTERVAL '27 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f003'), 'IN',    180, '생산 완료 입고',          NOW() - INTERVAL '24 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f003'), 'OUT',   -24, '이스마트 납품 (9차)',     NOW() - INTERVAL '20 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f003'), 'OUT',   -18, '소매 채널 출고',          NOW() - INTERVAL '15 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f003'), 'PRODUCTION_DEDUCTION', -47, '생산 차감', NOW() - INTERVAL '12 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f003'), 'OUT',   -20, '이스마트 납품 (10차)',    NOW() - INTERVAL '7 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f003'), 'OUT',   -15, '소매 채널 출고',          NOW() - INTERVAL '3 day'),

-- ── 스마트카메라 F004 (wA1) ──────────────────────────────────────────────
((SELECT id FROM seed_ref WHERE k='inv_wA1_f004'), 'IN',    120, '초기 입고',               NOW() - INTERVAL '80 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f004'), 'OUT',   -12, '이스마트 납품 (1차)',     NOW() - INTERVAL '77 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f004'), 'OUT',    -8, '소매 채널 출고',          NOW() - INTERVAL '72 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f004'), 'OUT',   -10, '이스마트 납품 (2차)',     NOW() - INTERVAL '67 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f004'), 'PRODUCTION_DEDUCTION', -22, '생산 차감', NOW() - INTERVAL '62 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f004'), 'IN',     80, '생산 완료 입고',          NOW() - INTERVAL '60 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f004'), 'OUT',   -11, '이스마트 납품 (3차)',     NOW() - INTERVAL '56 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f004'), 'OUT',    -9, '소매 채널 출고',          NOW() - INTERVAL '51 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f004'), 'PRODUCTION_DEDUCTION', -25, '생산 차감', NOW() - INTERVAL '46 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f004'), 'IN',     70, '생산 완료 입고',          NOW() - INTERVAL '43 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f004'), 'OUT',   -12, '이스마트 납품 (4차)',     NOW() - INTERVAL '38 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f004'), 'OUT',    -8, '소매 채널 출고',          NOW() - INTERVAL '32 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f004'), 'PRODUCTION_DEDUCTION', -21, '생산 차감', NOW() - INTERVAL '28 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f004'), 'IN',     60, '생산 완료 입고',          NOW() - INTERVAL '25 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f004'), 'OUT',   -10, '이스마트 납품 (5차)',     NOW() - INTERVAL '18 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f004'), 'OUT',    -7, '소매 채널 출고',          NOW() - INTERVAL '10 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f004'), 'OUT',    -9, '이스마트 납품 (6차)',     NOW() - INTERVAL '4 day'),

-- ── 스마트도어락 F005 (wA1) ──────────────────────────────────────────────
((SELECT id FROM seed_ref WHERE k='inv_wA1_f005'), 'IN',     80, '초기 입고',               NOW() - INTERVAL '75 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f005'), 'OUT',    -8, '이스마트 납품 (1차)',     NOW() - INTERVAL '71 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f005'), 'OUT',    -6, '소매 채널 출고',          NOW() - INTERVAL '66 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f005'), 'PRODUCTION_DEDUCTION', -15, '생산 차감', NOW() - INTERVAL '62 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f005'), 'IN',     50, '생산 완료 입고',          NOW() - INTERVAL '59 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f005'), 'OUT',    -7, '이스마트 납품 (2차)',     NOW() - INTERVAL '55 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f005'), 'OUT',    -5, '소매 채널 출고',          NOW() - INTERVAL '49 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f005'), 'PRODUCTION_DEDUCTION', -10, '생산 차감', NOW() - INTERVAL '45 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f005'), 'IN',     40, '생산 완료 입고',          NOW() - INTERVAL '42 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f005'), 'OUT',    -8, '이스마트 납품 (3차)',     NOW() - INTERVAL '36 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f005'), 'OUT',    -6, '소매 채널 출고',          NOW() - INTERVAL '29 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f005'), 'PRODUCTION_DEDUCTION', -14, '생산 차감', NOW() - INTERVAL '25 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f005'), 'IN',     30, '생산 완료 입고',          NOW() - INTERVAL '22 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f005'), 'OUT',    -7, '이스마트 납품 (4차)',     NOW() - INTERVAL '15 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f005'), 'OUT',    -5, '소매 채널 출고',          NOW() - INTERVAL '7 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_f005'), 'OUT',    -6, '이스마트 납품 (5차)',     NOW() - INTERVAL '2 day'),

-- ── 신규 부품 이력 (wA1) ─────────────────────────────────────────────────
((SELECT id FROM seed_ref WHERE k='inv_wA1_p009'), 'IN',    200, '초기 입고',               NOW() - INTERVAL '80 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p009'), 'PRODUCTION_DEDUCTION', -22, '생산 차감', NOW() - INTERVAL '62 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p009'), 'PRODUCTION_DEDUCTION', -25, '생산 차감', NOW() - INTERVAL '46 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p009'), 'PRODUCTION_DEDUCTION', -21, '생산 차감', NOW() - INTERVAL '28 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p009'), 'IN',     80, '추가 입고',               NOW() - INTERVAL '20 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p009'), 'PRODUCTION_DEDUCTION', -47, '생산 차감', NOW() - INTERVAL '10 day'),
-- P009 조기 소진 발생 (D71 기준 stock 부족 → ANOMALY 시나리오)

((SELECT id FROM seed_ref WHERE k='inv_wA1_p010'), 'IN',    100, '초기 입고',               NOW() - INTERVAL '75 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p010'), 'PRODUCTION_DEDUCTION', -15, '생산 차감', NOW() - INTERVAL '62 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p010'), 'PRODUCTION_DEDUCTION', -10, '생산 차감', NOW() - INTERVAL '45 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p010'), 'IN',     40, '추가 입고',               NOW() - INTERVAL '30 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p010'), 'PRODUCTION_DEDUCTION', -14, '생산 차감', NOW() - INTERVAL '25 day'),
((SELECT id FROM seed_ref WHERE k='inv_wA1_p010'), 'PRODUCTION_DEDUCTION', -13, '생산 차감', NOW() - INTERVAL '10 day'),

-- ── 이스마트 창고 — 신규 완제품 ─────────────────────────────────────────
((SELECT id FROM seed_ref WHERE k='inv_wE_f003'), 'IN',     80, 'A사 1차 납품',             NOW() - INTERVAL '82 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f003'), 'OUT',   -45, '온라인몰 판매 출고',        NOW() - INTERVAL '70 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f003'), 'IN',     90, 'A사 2차 납품',             NOW() - INTERVAL '57 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f003'), 'OUT',   -55, '온라인몰 판매 출고',        NOW() - INTERVAL '43 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f003'), 'IN',    100, 'A사 3차 납품',             NOW() - INTERVAL '28 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f003'), 'OUT',   -60, '온라인몰 판매 출고',        NOW() - INTERVAL '12 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f003'), 'IN',    105, 'A사 4차 납품',             NOW() - INTERVAL '3 day'),

((SELECT id FROM seed_ref WHERE k='inv_wE_f004'), 'IN',     30, 'A사 1차 납품',             NOW() - INTERVAL '77 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f004'), 'OUT',   -18, '온라인몰 판매 출고',        NOW() - INTERVAL '62 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f004'), 'IN',     32, 'A사 2차 납품',             NOW() - INTERVAL '46 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f004'), 'OUT',   -22, '온라인몰 판매 출고',        NOW() - INTERVAL '28 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f004'), 'IN',     30, 'A사 3차 납품',             NOW() - INTERVAL '10 day'),

((SELECT id FROM seed_ref WHERE k='inv_wE_f005'), 'IN',     18, 'A사 1차 납품',             NOW() - INTERVAL '71 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f005'), 'OUT',   -10, '온라인몰 판매 출고',        NOW() - INTERVAL '55 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f005'), 'IN',     16, 'A사 2차 납품',             NOW() - INTERVAL '36 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f005'), 'OUT',   -12, '온라인몰 판매 출고',        NOW() - INTERVAL '15 day'),
((SELECT id FROM seed_ref WHERE k='inv_wE_f005'), 'IN',     15, 'A사 3차 납품',             NOW() - INTERVAL '2 day');

-- -----------------------------------------------------------------------------
-- ProductionRecord (90일치 — F001 25건 / F002 23건)
-- 수량은 (warehouse_id, item_id) 범위 내 고유 → SET 문으로 ID 조회에 사용
-- -----------------------------------------------------------------------------
INSERT INTO production_records (warehouse_id, item_id, quantity, status, created_at) VALUES
-- ── F001 스마트스피커 (25건) ──────────────────────────────────────────────
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f001'),  45, 'SETTLED', NOW() - INTERVAL '90 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f001'),  42, 'SETTLED', NOW() - INTERVAL '86 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f001'),  48, 'SETTLED', NOW() - INTERVAL '82 day'),  -- ANOMALY: P001
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f001'),  38, 'SETTLED', NOW() - INTERVAL '78 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f001'),  52, 'SETTLED', NOW() - INTERVAL '74 day'),  -- ANOMALY: P002
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f001'),  41, 'SETTLED', NOW() - INTERVAL '70 day'),  -- ANOMALY: P001
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f001'),  44, 'SETTLED', NOW() - INTERVAL '66 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f001'),  49, 'SETTLED', NOW() - INTERVAL '62 day'),  -- ANOMALY: P004
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f001'),  33, 'SETTLED', NOW() - INTERVAL '58 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f001'),  36, 'SETTLED', NOW() - INTERVAL '54 day'),  -- ANOMALY: P005
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f001'),  31, 'SETTLED', NOW() - INTERVAL '50 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f001'),  43, 'SETTLED', NOW() - INTERVAL '47 day'),  -- ANOMALY: P001+P002
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f001'),  37, 'SETTLED', NOW() - INTERVAL '43 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f001'),  29, 'SETTLED', NOW() - INTERVAL '38 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f001'),  46, 'SETTLED', NOW() - INTERVAL '34 day'),  -- ANOMALY: P006
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f001'),  30, 'SETTLED', NOW() - INTERVAL '30 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f001'),  28, 'SETTLED', NOW() - INTERVAL '26 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f001'),  40, 'SETTLED', NOW() - INTERVAL '21 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f001'),  22, 'SETTLED', NOW() - INTERVAL '19 day'),  -- ANOMALY: P001
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f001'),  35, 'SETTLED', NOW() - INTERVAL '15 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f001'),  32, 'SETTLED', NOW() - INTERVAL '12 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f001'),  27, 'SETTLED', NOW() - INTERVAL '10 day'),  -- ANOMALY: P001
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f001'),  25, 'SETTLED', NOW() - INTERVAL '7 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f001'),  20, 'SETTLED', NOW() - INTERVAL '3 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f001'),  23, 'PENDING', NOW() - INTERVAL '1 day'),
-- ── F002 스마트허브 (23건) ────────────────────────────────────────────────
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f002'),  26, 'SETTLED', NOW() - INTERVAL '88 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f002'),  23, 'SETTLED', NOW() - INTERVAL '84 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f002'),  29, 'SETTLED', NOW() - INTERVAL '80 day'),  -- ANOMALY: P002
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f002'),  24, 'SETTLED', NOW() - INTERVAL '76 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f002'),  27, 'SETTLED', NOW() - INTERVAL '72 day'),  -- ANOMALY: P001
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f002'),  22, 'SETTLED', NOW() - INTERVAL '68 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f002'),  25, 'SETTLED', NOW() - INTERVAL '64 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f002'),  19, 'SETTLED', NOW() - INTERVAL '60 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f002'),  28, 'SETTLED', NOW() - INTERVAL '56 day'),  -- ANOMALY: P008
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f002'),  32, 'SETTLED', NOW() - INTERVAL '52 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f002'),  31, 'SETTLED', NOW() - INTERVAL '48 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f002'),  33, 'SETTLED', NOW() - INTERVAL '44 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f002'),  34, 'SETTLED', NOW() - INTERVAL '40 day'),  -- ANOMALY: P007
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f002'),  37, 'SETTLED', NOW() - INTERVAL '36 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f002'),  39, 'SETTLED', NOW() - INTERVAL '32 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f002'),  18, 'SETTLED', NOW() - INTERVAL '28 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f002'),  16, 'SETTLED', NOW() - INTERVAL '24 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f002'),  15, 'SETTLED', NOW() - INTERVAL '17 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f002'),  21, 'SETTLED', NOW() - INTERVAL '14 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f002'),  12, 'SETTLED', NOW() - INTERVAL '8 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f002'),  17, 'SETTLED', NOW() - INTERVAL '5 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f002'),  11, 'SETTLED', NOW() - INTERVAL '2 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f002'),  14, 'PENDING', NOW() - INTERVAL '1 day'),
-- ── F003 스마트플러그 (11건) ──────────────────────────────────────────────
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f003'),  60, 'SETTLED', NOW() - INTERVAL '85 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f003'),  55, 'SETTLED', NOW() - INTERVAL '77 day'),  -- ANOMALY: P002
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f003'),  58, 'SETTLED', NOW() - INTERVAL '69 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f003'),  52, 'SETTLED', NOW() - INTERVAL '62 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f003'),  63, 'SETTLED', NOW() - INTERVAL '55 day'),  -- ANOMALY: P002
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f003'),  50, 'SETTLED', NOW() - INTERVAL '48 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f003'),  64, 'SETTLED', NOW() - INTERVAL '41 day'),  -- ANOMALY: P005
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f003'),  47, 'SETTLED', NOW() - INTERVAL '35 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f003'),  56, 'SETTLED', NOW() - INTERVAL '28 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f003'),  51, 'SETTLED', NOW() - INTERVAL '14 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f003'),  53, 'PENDING', NOW() - INTERVAL '1 day'),
-- ── F004 스마트카메라 (9건) ───────────────────────────────────────────────
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f004'),  24, 'SETTLED', NOW() - INTERVAL '80 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f004'),  20, 'SETTLED', NOW() - INTERVAL '72 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f004'),  22, 'SETTLED', NOW() - INTERVAL '63 day'),  -- ANOMALY: P009
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f004'),  25, 'SETTLED', NOW() - INTERVAL '55 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f004'),  18, 'SETTLED', NOW() - INTERVAL '47 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f004'),  21, 'SETTLED', NOW() - INTERVAL '39 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f004'),  23, 'SETTLED', NOW() - INTERVAL '30 day'),  -- ANOMALY: P009
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f004'),  26, 'SETTLED', NOW() - INTERVAL '20 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f004'),  19, 'PENDING', NOW() - INTERVAL '1 day'),
-- ── F005 스마트도어락 (7건) ───────────────────────────────────────────────
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f005'),  12, 'SETTLED', NOW() - INTERVAL '75 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f005'),  15, 'SETTLED', NOW() - INTERVAL '65 day'),  -- ANOMALY: P010
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f005'),  10, 'SETTLED', NOW() - INTERVAL '55 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f005'),  14, 'SETTLED', NOW() - INTERVAL '45 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f005'),  11, 'SETTLED', NOW() - INTERVAL '35 day'),  -- ANOMALY: P010
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f005'),  13, 'SETTLED', NOW() - INTERVAL '22 day'),
((SELECT id FROM seed_ref WHERE k='wA1'), (SELECT id FROM seed_ref WHERE k='f005'),  16, 'PENDING', NOW() - INTERVAL '1 day');

-- production_record ID 변수 (quantity 고유 → 간단 조회)
-- F001
INSERT INTO seed_ref (k, id) VALUES ('pr_f001_q45', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f001') AND quantity=45));
INSERT INTO seed_ref (k, id) VALUES ('pr_f001_q42', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f001') AND quantity=42));
INSERT INTO seed_ref (k, id) VALUES ('pr_f001_q48', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f001') AND quantity=48));
INSERT INTO seed_ref (k, id) VALUES ('pr_f001_q38', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f001') AND quantity=38));
INSERT INTO seed_ref (k, id) VALUES ('pr_f001_q52', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f001') AND quantity=52));
INSERT INTO seed_ref (k, id) VALUES ('pr_f001_q41', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f001') AND quantity=41));
INSERT INTO seed_ref (k, id) VALUES ('pr_f001_q44', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f001') AND quantity=44));
INSERT INTO seed_ref (k, id) VALUES ('pr_f001_q49', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f001') AND quantity=49));
INSERT INTO seed_ref (k, id) VALUES ('pr_f001_q33', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f001') AND quantity=33));
INSERT INTO seed_ref (k, id) VALUES ('pr_f001_q36', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f001') AND quantity=36));
INSERT INTO seed_ref (k, id) VALUES ('pr_f001_q31', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f001') AND quantity=31));
INSERT INTO seed_ref (k, id) VALUES ('pr_f001_q43', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f001') AND quantity=43));
INSERT INTO seed_ref (k, id) VALUES ('pr_f001_q37', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f001') AND quantity=37));
INSERT INTO seed_ref (k, id) VALUES ('pr_f001_q29', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f001') AND quantity=29));
INSERT INTO seed_ref (k, id) VALUES ('pr_f001_q46', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f001') AND quantity=46));
INSERT INTO seed_ref (k, id) VALUES ('pr_f001_q30', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f001') AND quantity=30));
INSERT INTO seed_ref (k, id) VALUES ('pr_f001_q28', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f001') AND quantity=28));
INSERT INTO seed_ref (k, id) VALUES ('pr_f001_q40', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f001') AND quantity=40));
INSERT INTO seed_ref (k, id) VALUES ('pr_f001_q22', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f001') AND quantity=22));
INSERT INTO seed_ref (k, id) VALUES ('pr_f001_q35', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f001') AND quantity=35));
INSERT INTO seed_ref (k, id) VALUES ('pr_f001_q32', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f001') AND quantity=32));
INSERT INTO seed_ref (k, id) VALUES ('pr_f001_q27', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f001') AND quantity=27));
INSERT INTO seed_ref (k, id) VALUES ('pr_f001_q25', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f001') AND quantity=25));
INSERT INTO seed_ref (k, id) VALUES ('pr_f001_q20', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f001') AND quantity=20));
-- F002
INSERT INTO seed_ref (k, id) VALUES ('pr_f002_q26', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f002') AND quantity=26));
INSERT INTO seed_ref (k, id) VALUES ('pr_f002_q23', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f002') AND quantity=23));
INSERT INTO seed_ref (k, id) VALUES ('pr_f002_q29', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f002') AND quantity=29));
INSERT INTO seed_ref (k, id) VALUES ('pr_f002_q24', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f002') AND quantity=24));
INSERT INTO seed_ref (k, id) VALUES ('pr_f002_q27', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f002') AND quantity=27));
INSERT INTO seed_ref (k, id) VALUES ('pr_f002_q22', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f002') AND quantity=22));
INSERT INTO seed_ref (k, id) VALUES ('pr_f002_q25', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f002') AND quantity=25));
INSERT INTO seed_ref (k, id) VALUES ('pr_f002_q19', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f002') AND quantity=19));
INSERT INTO seed_ref (k, id) VALUES ('pr_f002_q28', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f002') AND quantity=28));
INSERT INTO seed_ref (k, id) VALUES ('pr_f002_q32', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f002') AND quantity=32));
INSERT INTO seed_ref (k, id) VALUES ('pr_f002_q31', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f002') AND quantity=31));
INSERT INTO seed_ref (k, id) VALUES ('pr_f002_q33', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f002') AND quantity=33));
INSERT INTO seed_ref (k, id) VALUES ('pr_f002_q34', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f002') AND quantity=34));
INSERT INTO seed_ref (k, id) VALUES ('pr_f002_q37', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f002') AND quantity=37));
INSERT INTO seed_ref (k, id) VALUES ('pr_f002_q39', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f002') AND quantity=39));
INSERT INTO seed_ref (k, id) VALUES ('pr_f002_q18', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f002') AND quantity=18));
INSERT INTO seed_ref (k, id) VALUES ('pr_f002_q16', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f002') AND quantity=16));
INSERT INTO seed_ref (k, id) VALUES ('pr_f002_q15', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f002') AND quantity=15));
INSERT INTO seed_ref (k, id) VALUES ('pr_f002_q21', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f002') AND quantity=21));
INSERT INTO seed_ref (k, id) VALUES ('pr_f002_q12', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f002') AND quantity=12));
INSERT INTO seed_ref (k, id) VALUES ('pr_f002_q17', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f002') AND quantity=17));
INSERT INTO seed_ref (k, id) VALUES ('pr_f002_q11', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f002') AND quantity=11));
-- F003
INSERT INTO seed_ref (k, id) VALUES ('pr_f003_q60', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f003') AND quantity=60));
INSERT INTO seed_ref (k, id) VALUES ('pr_f003_q55', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f003') AND quantity=55));
INSERT INTO seed_ref (k, id) VALUES ('pr_f003_q58', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f003') AND quantity=58));
INSERT INTO seed_ref (k, id) VALUES ('pr_f003_q52', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f003') AND quantity=52));
INSERT INTO seed_ref (k, id) VALUES ('pr_f003_q63', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f003') AND quantity=63));
INSERT INTO seed_ref (k, id) VALUES ('pr_f003_q50', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f003') AND quantity=50));
INSERT INTO seed_ref (k, id) VALUES ('pr_f003_q64', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f003') AND quantity=64));
INSERT INTO seed_ref (k, id) VALUES ('pr_f003_q47', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f003') AND quantity=47));
INSERT INTO seed_ref (k, id) VALUES ('pr_f003_q56', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f003') AND quantity=56));
INSERT INTO seed_ref (k, id) VALUES ('pr_f003_q51', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f003') AND quantity=51));
-- F004
INSERT INTO seed_ref (k, id) VALUES ('pr_f004_q24', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f004') AND quantity=24));
INSERT INTO seed_ref (k, id) VALUES ('pr_f004_q20', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f004') AND quantity=20));
INSERT INTO seed_ref (k, id) VALUES ('pr_f004_q22', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f004') AND quantity=22));
INSERT INTO seed_ref (k, id) VALUES ('pr_f004_q25', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f004') AND quantity=25));
INSERT INTO seed_ref (k, id) VALUES ('pr_f004_q18', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f004') AND quantity=18));
INSERT INTO seed_ref (k, id) VALUES ('pr_f004_q21', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f004') AND quantity=21));
INSERT INTO seed_ref (k, id) VALUES ('pr_f004_q23', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f004') AND quantity=23));
INSERT INTO seed_ref (k, id) VALUES ('pr_f004_q26', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f004') AND quantity=26));
-- F005
INSERT INTO seed_ref (k, id) VALUES ('pr_f005_q12', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f005') AND quantity=12));
INSERT INTO seed_ref (k, id) VALUES ('pr_f005_q15', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f005') AND quantity=15));
INSERT INTO seed_ref (k, id) VALUES ('pr_f005_q10', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f005') AND quantity=10));
INSERT INTO seed_ref (k, id) VALUES ('pr_f005_q14', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f005') AND quantity=14));
INSERT INTO seed_ref (k, id) VALUES ('pr_f005_q11', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f005') AND quantity=11));
INSERT INTO seed_ref (k, id) VALUES ('pr_f005_q13', (SELECT id FROM production_records WHERE warehouse_id=(SELECT id FROM seed_ref WHERE k='wA1') AND item_id=(SELECT id FROM seed_ref WHERE k='f005') AND quantity=13));
-- -----------------------------------------------------------------------------
-- Settlement
-- ANOMALY 13건 — 5가지 부품 → 자주 부족한 부품 Top5 차트 완전 표시
--   P001(PCB기판):     6회 → 1위
--   P002(WiFi/BT모듈): 3회 → 2위
--   P004(마이크모듈):  1회
--   P005(전원IC):      1회
--   P006(케이스소):    1회
--   P007(케이스대):    1회  ← F002 전용
--   P008(DC어댑터):    1회  ← F002 전용
-- -----------------------------------------------------------------------------
INSERT INTO settlements (production_record_id, result, anomaly_detail, memo, settled_at) VALUES

-- F001 90일차 ~ 31일차 (신규 15건)
((SELECT id FROM seed_ref WHERE k='pr_f001_q45'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '89 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f001_q42'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '85 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f001_q48'), 'ANOMALY', '{"P001":{"required":48,"stock":20}}', 'PCB기판 부족',   NOW() - INTERVAL '81 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f001_q38'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '77 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f001_q52'), 'ANOMALY', '{"P002":{"required":52,"stock":30}}', 'WiFi/BT모듈 부족', NOW() - INTERVAL '73 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f001_q41'), 'ANOMALY', '{"P001":{"required":41,"stock":5}}',  'PCB기판 부족',   NOW() - INTERVAL '69 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f001_q44'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '65 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f001_q49'), 'ANOMALY', '{"P004":{"required":98,"stock":60}}', '마이크모듈 부족', NOW() - INTERVAL '61 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f001_q33'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '57 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f001_q36'), 'ANOMALY', '{"P005":{"required":144,"stock":90}}','전원IC 부족',    NOW() - INTERVAL '53 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f001_q31'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '49 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f001_q43'), 'ANOMALY', '{"P001":{"required":43,"stock":0},"P002":{"required":43,"stock":15}}', 'PCB기판·WiFi모듈 동시 부족', NOW() - INTERVAL '46 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f001_q37'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '42 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f001_q29'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '37 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f001_q46'), 'ANOMALY', '{"P006":{"required":46,"stock":25}}', '케이스(소) 부족', NOW() - INTERVAL '33 day' + INTERVAL '5 minute'),

-- F001 30일차 ~ 3일차 (기존 9건)
((SELECT id FROM seed_ref WHERE k='pr_f001_q30'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '29 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f001_q28'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '25 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f001_q40'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '20 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f001_q22'), 'ANOMALY', '{"P001":{"required":22,"stock":8}}',  'PCB기판 재고 부족', NOW() - INTERVAL '18 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f001_q35'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '14 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f001_q32'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '11 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f001_q27'), 'ANOMALY', '{"P001":{"required":27,"stock":0}}',  'PCB기판 완전 소진', NOW() - INTERVAL '9 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f001_q25'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '6 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f001_q20'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '2 day' + INTERVAL '5 minute'),

-- F002 90일차 ~ 32일차 (신규 15건)
((SELECT id FROM seed_ref WHERE k='pr_f002_q26'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '87 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f002_q23'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '83 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f002_q29'), 'ANOMALY', '{"P002":{"required":29,"stock":10}}', 'WiFi/BT모듈 부족', NOW() - INTERVAL '79 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f002_q24'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '75 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f002_q27'), 'ANOMALY', '{"P001":{"required":27,"stock":3}}',  'PCB기판 부족',    NOW() - INTERVAL '71 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f002_q22'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '67 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f002_q25'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '63 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f002_q19'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '59 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f002_q28'), 'ANOMALY', '{"P008":{"required":28,"stock":12}}', 'DC어댑터 부족',   NOW() - INTERVAL '55 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f002_q32'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '51 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f002_q31'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '47 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f002_q33'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '43 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f002_q34'), 'ANOMALY', '{"P007":{"required":34,"stock":18}}', '케이스(대) 부족', NOW() - INTERVAL '39 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f002_q37'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '35 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f002_q39'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '31 day' + INTERVAL '5 minute'),

-- F002 28일차 ~ 2일차 (기존 7건)
((SELECT id FROM seed_ref WHERE k='pr_f002_q18'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '27 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f002_q16'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '23 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f002_q15'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '16 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f002_q21'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '13 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f002_q12'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '7 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f002_q17'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '4 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f002_q11'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '1 day' + INTERVAL '5 minute'),

-- F003 스마트플러그 (10건 결산)
((SELECT id FROM seed_ref WHERE k='pr_f003_q60'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '84 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f003_q55'), 'ANOMALY', '{"P002":{"required":55,"stock":20}}', 'WiFi/BT모듈 부족', NOW() - INTERVAL '76 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f003_q58'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '68 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f003_q52'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '61 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f003_q63'), 'ANOMALY', '{"P002":{"required":63,"stock":30}}', 'WiFi/BT모듈 부족', NOW() - INTERVAL '54 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f003_q50'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '47 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f003_q64'), 'ANOMALY', '{"P005":{"required":128,"stock":80}}','전원IC 부족',     NOW() - INTERVAL '40 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f003_q47'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '34 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f003_q56'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '27 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f003_q51'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '13 day' + INTERVAL '5 minute'),

-- F004 스마트카메라 (8건 결산)
((SELECT id FROM seed_ref WHERE k='pr_f004_q24'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '79 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f004_q20'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '71 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f004_q22'), 'ANOMALY', '{"P009":{"required":22,"stock":10}}', '카메라센서모듈 부족', NOW() - INTERVAL '62 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f004_q25'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '54 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f004_q18'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '46 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f004_q21'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '38 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f004_q23'), 'ANOMALY', '{"P009":{"required":23,"stock":5}}',  '카메라센서모듈 부족', NOW() - INTERVAL '29 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f004_q26'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '19 day' + INTERVAL '5 minute'),

-- F005 스마트도어락 (6건 결산)
((SELECT id FROM seed_ref WHERE k='pr_f005_q12'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '74 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f005_q15'), 'ANOMALY', '{"P010":{"required":15,"stock":8}}',  '배터리팩 부족',   NOW() - INTERVAL '64 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f005_q10'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '54 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f005_q14'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '44 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f005_q11'), 'ANOMALY', '{"P010":{"required":11,"stock":3}}',  '배터리팩 부족',   NOW() - INTERVAL '34 day' + INTERVAL '5 minute'),
((SELECT id FROM seed_ref WHERE k='pr_f005_q13'), 'SUCCESS', NULL, NULL,                                        NOW() - INTERVAL '21 day' + INTERVAL '5 minute');

-- -----------------------------------------------------------------------------
-- 완료 확인
-- -----------------------------------------------------------------------------
SELECT '=== Seed 완료 (v4) ===' AS status;
SELECT '사용자'    AS entity, COUNT(*) AS cnt FROM users
UNION ALL SELECT '파트너십',   COUNT(*) FROM partnerships
UNION ALL SELECT '창고',       COUNT(*) FROM warehouses
UNION ALL SELECT '창고공유',   COUNT(*) FROM warehouse_shares
UNION ALL SELECT '품목',       COUNT(*) FROM items
UNION ALL SELECT 'BOM',        COUNT(*) FROM boms
UNION ALL SELECT '재고',       COUNT(*) FROM inventories
UNION ALL SELECT '입출고이력', COUNT(*) FROM inventory_histories
UNION ALL SELECT '생산기록',   COUNT(*) FROM production_records
UNION ALL SELECT '결산',       COUNT(*) FROM settlements;
