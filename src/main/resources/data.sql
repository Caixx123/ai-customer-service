INSERT INTO orders (order_no, user_id, product_name, amount, status, created_at) VALUES
('O20261023001', 'user-001', '无线降噪耳机', 299.00, 'DELIVERED', '2026-10-18 10:15:00'),
('O20261023002', 'user-001', '不锈钢保温杯', 159.00, 'REFUNDING', '2026-10-20 09:00:00'),
('O20261023003', 'user-001', '机械键盘', 499.00, 'SHIPPED', '2026-10-22 18:40:00'),
('O20261023005', 'user-001', '手机保护壳', 39.00, 'PAID', '2026-10-23 09:30:00'),
('O20261023004', 'user-002', '27寸显示器', 1299.00, 'PAID', '2026-10-19 14:20:00');

INSERT INTO logistics (order_no, carrier, tracking_no, status, detail, updated_at) VALUES
('O20261023001', '顺丰速运', 'SF1234567890', '已签收',
 '2026-10-20 14:32 快件已由本人签收，签收人：张先生', '2026-10-20 14:32:00'),
('O20261023003', '中通快递', 'ZT987654321', '运输中',
 '2026-10-23 08:10 快件已到达【上海转运中心】，预计 10-24 送达', '2026-10-23 08:10:00');

INSERT INTO refunds (order_no, user_id, amount, status, reason, applied_at, updated_at) VALUES
('O20261023002', 'user-001', 159.00, 'MERCHANT_REVIEW', '商品表面有划痕', '2026-10-21 11:20:00', '2026-10-22 09:05:00');
