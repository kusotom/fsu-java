-- BE-AUTH-P0-001: 角色与权限种子数据 (最小闭环)
-- 本脚本在 dev 环境初始化默认角色和权限，生产环境需由管理员通过 UI 配置.

-- 清理已有角色 (幂等)
DELETE FROM user_role WHERE role_id IN (SELECT id FROM role WHERE role_code IN ('super_admin','platform_admin','protocol_debugger','admin','operator','viewer'));
DELETE FROM role WHERE role_code IN ('super_admin','platform_admin','protocol_debugger','admin','operator','viewer');

-- === 角色 ===
-- super_admin: 全部权限 (不含高风险SET赋权，但可通过isAdminLike跳过权限检查)
INSERT INTO role (role_code, role_name, description, permissions, created_at, updated_at)
VALUES ('super_admin', '超级管理员',
        '全部权限，可查看raw XML、执行run-once、管理系统',
        'dashboard:view,site:view,fsu:view,realtime:view,alarm:view,protocol:raw:view,protocol:raw:download,protocol:runonce:readonly,user:view,role:view,permission:view,tenant:view,audit:view',
        NOW(), NOW());

-- platform_admin: 平台管理员 (除SET外全部)
INSERT INTO role (role_code, role_name, description, permissions, created_at, updated_at)
VALUES ('platform_admin', '平台管理员',
        '平台管理权限，可查看raw XML和run-once，不可执行SET',
        'dashboard:view,site:view,fsu:view,realtime:view,alarm:view,protocol:raw:view,protocol:raw:download,protocol:runonce:readonly,user:view,role:view,permission:view,tenant:view,audit:view',
        NOW(), NOW());

-- protocol_debugger: 协议调试员 (raw XML + run-once 只读)
INSERT INTO role (role_code, role_name, description, permissions, created_at, updated_at)
VALUES ('protocol_debugger', '协议调试员',
        '协议调试权限，可查看raw XML和只读run-once',
        'dashboard:view,site:view,fsu:view,realtime:view,alarm:view,protocol:raw:view,protocol:raw:download,protocol:runonce:readonly',
        NOW(), NOW());

-- admin: 管理员 (基础查看 + 系统管理)
INSERT INTO role (role_code, role_name, description, permissions, created_at, updated_at)
VALUES ('admin', '管理员',
        '基础查看+系统管理权限，不含raw XML和run-once',
        'dashboard:view,site:view,fsu:view,realtime:view,alarm:view,user:view,role:view,permission:view',
        NOW(), NOW());

-- operator: 操作员 (基础查看)
INSERT INTO role (role_code, role_name, description, permissions, created_at, updated_at)
VALUES ('operator', '操作员',
        '基础查看权限：总览、站点、FSU、实时数据、告警',
        'dashboard:view,site:view,fsu:view,realtime:view,alarm:view',
        NOW(), NOW());

-- viewer: 查看者 (只读，有限查看)
INSERT INTO role (role_code, role_name, description, permissions, created_at, updated_at)
VALUES ('viewer', '查看者',
        '有限查看权限：仅总览和站点',
        'dashboard:view,site:view,fsu:view',
        NOW(), NOW());

-- === 为已有用户分配角色 (dev 环境) ===
-- admin 用户 (phone=13800000000, id=1) → super_admin 角色
INSERT INTO user_role (user_id, role_id, created_at)
SELECT 1, id, NOW() FROM role WHERE role_code = 'super_admin'
ON CONFLICT DO NOTHING;

-- operator 用户 (phone=13800000001, id=2) → operator 角色
INSERT INTO user_role (user_id, role_id, created_at)
SELECT 2, id, NOW() FROM role WHERE role_code = 'operator'
ON CONFLICT DO NOTHING;

-- viewer 用户 (phone=13800000002, id=3) → viewer 角色
INSERT INTO user_role (user_id, role_id, created_at)
SELECT 3, id, NOW() FROM role WHERE role_code = 'viewer'
ON CONFLICT DO NOTHING;
